package com.example.aptitude.service;

import com.example.aptitude.dto.AnswerDto;
import com.example.aptitude.dto.AnsweredQuestionDto;
import com.example.aptitude.dto.AptitudeFindingsDto;
import com.example.aptitude.dto.QuestionDto;
import com.example.aptitude.dto.QuestionOptionDto;
import com.example.aptitude.dto.QuestionOptionType;
import com.example.aptitude.dto.QuestionType;
import com.example.aptitude.dto.SessionResponseDto;
import com.example.aptitude.model.AptitudeSession;
import com.example.aptitude.model.SessionStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AptitudeSessionService {

    private static final TypeReference<List<AnsweredQuestionDto>> ANSWERS_TYPE = new TypeReference<>() {
    };
    private static final List<String> FALLBACK_FOLLOW_UP_QUESTIONS = List.of(
            "What kind of work environment helps you do your best work?",
            "When a project is difficult, what usually keeps you motivated?",
            "Which skills would you be excited to practice every week?",
            "What kinds of responsibilities would you prefer to avoid in a long-term role?",
            "How do you like to balance independent work with collaboration?"
    );

    private final AptitudeSessionStore store;
    private final AptitudeAiClient aiClient;
    private final ObjectMapper objectMapper;
    private final int maxQuestions;

    public AptitudeSessionService(
            AptitudeSessionStore store,
            AptitudeAiClient aiClient,
            ObjectMapper objectMapper,
            @Value("${aptitude.max-questions:15}") int maxQuestions
    ) {
        this.store = store;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.maxQuestions = Math.clamp(maxQuestions, 1, 15);
    }

    @Transactional
    public SessionResponseDto createSession() {
        QuestionDto firstQuestion = sanitizeQuestion(aiClient.firstQuestion());
        AptitudeSession session = new AptitudeSession(UUID.randomUUID(), firstQuestion, writeJson(firstQuestion));
        return toResponse(store.save(session));
    }

    @Transactional(readOnly = true)
    public SessionResponseDto getSession(UUID sessionId) {
        return store.findById(sessionId)
                .map(this::toResponse)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    @Transactional
    public SessionResponseDto answer(UUID sessionId, AnswerDto answer) {
        AptitudeSession session = store.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new SessionAlreadyCompletedException(sessionId);
        }

        List<AnsweredQuestionDto> answers = new ArrayList<>(readAnswers(session));
        QuestionDto currentQuestion = readQuestion(session);
        AnswerSubmission answerSubmission = formatAnswer(currentQuestion, answer);
        answers.add(new AnsweredQuestionDto(
                currentQuestion.prompt(),
                answerSubmission.answerText(),
                currentQuestion,
                answerSubmission.selectedOptionIds()
        ));

        AptitudeAiDecision decision = aiClient.evaluate(answers, maxQuestions);
        boolean forcedCompletion = answers.size() >= maxQuestions;
        boolean completed = forcedCompletion || decision.ready();

        session.setAnswersJson(writeJson(answers));
        if (completed) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setCurrentQuestion(null);
            session.setCurrentQuestionJson(null);
            session.setFindingsJson(writeJson(decision.findings()));
        } else {
            QuestionDto nextQuestion = nextDistinctQuestion(sanitizeQuestion(decision.nextQuestion()), answers);
            session.setQuestionCount(answers.size() + 1);
            session.setCurrentQuestion(nextQuestion.prompt());
            session.setCurrentQuestionJson(writeJson(nextQuestion));
        }

        AptitudeSession savedSession = store.save(session);
        SessionResponseDto response = toResponse(savedSession);

        if (completed) {
            store.delete(savedSession);
        }

        return response;
    }

    private SessionResponseDto toResponse(AptitudeSession session) {
        return new SessionResponseDto(
                session.getId(),
                session.getStatus(),
                session.getQuestionCount(),
                maxQuestions,
                session.getStatus() == SessionStatus.COMPLETED ? null : readQuestion(session),
                session.getCurrentQuestion(),
                readAnswers(session),
                readFindings(session),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private List<AnsweredQuestionDto> readAnswers(AptitudeSession session) {
        try {
            return objectMapper.readValue(session.getAnswersJson(), ANSWERS_TYPE);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private QuestionDto readQuestion(AptitudeSession session) {
        if (session.getCurrentQuestionJson() == null || session.getCurrentQuestionJson().isBlank()) {
            return QuestionDto.freeText(session.getCurrentQuestion());
        }

        try {
            return sanitizeQuestion(objectMapper.readValue(session.getCurrentQuestionJson(), QuestionDto.class));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private AptitudeFindingsDto readFindings(AptitudeSession session) {
        if (session.getFindingsJson() == null || session.getFindingsJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(session.getFindingsJson(), AptitudeFindingsDto.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private QuestionDto sanitizeQuestion(QuestionDto question) {
        if (question == null || question.prompt().isBlank()) {
            return QuestionDto.freeText("What would you like your work to feel like day to day?");
        }

        if (question.type() == QuestionType.FREE_TEXT) {
            return QuestionDto.freeText(question.prompt());
        }

        Map<String, QuestionOptionDto> optionsById = new LinkedHashMap<>();
        question.options().stream()
                .filter(option -> option != null && !option.id().isBlank() && !option.label().isBlank())
                .limit(8)
                .forEach(option -> optionsById.putIfAbsent(option.id(), option));
        List<QuestionOptionDto> options = List.copyOf(optionsById.values());

        if (options.size() < 2) {
            return QuestionDto.freeText(question.prompt());
        }

        return new QuestionDto(question.type(), question.prompt(), options);
    }

    private QuestionDto nextDistinctQuestion(QuestionDto candidate, List<AnsweredQuestionDto> answers) {
        List<String> askedPrompts = answers.stream()
                .map(AnsweredQuestionDto::question)
                .map(this::normalizePrompt)
                .toList();

        if (!askedPrompts.contains(normalizePrompt(candidate.prompt()))) {
            return candidate;
        }

        return FALLBACK_FOLLOW_UP_QUESTIONS.stream()
                .filter(prompt -> !askedPrompts.contains(normalizePrompt(prompt)))
                .findFirst()
                .map(QuestionDto::freeText)
                .orElseGet(() -> QuestionDto.freeText("What else should I know about what makes work feel meaningful to you?"));
    }

    private String normalizePrompt(String prompt) {
        if (prompt == null) {
            return "";
        }

        return prompt.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private AnswerSubmission formatAnswer(QuestionDto question, AnswerDto answer) {
        if (question.type() == QuestionType.FREE_TEXT) {
            String answerText = answer.answer() == null ? "" : answer.answer().trim();
            if (answerText.isBlank()) {
                throw new IllegalArgumentException("Answer must not be blank.");
            }
            return new AnswerSubmission(answerText, List.of());
        }

        List<String> selectedIds = answer.selectedOptionIds().stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();

        if (selectedIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one option.");
        }

        if (question.type() == QuestionType.SINGLE_CHOICE && selectedIds.size() != 1) {
            throw new IllegalArgumentException("Select exactly one option.");
        }

        Map<String, QuestionOptionDto> optionsById = question.options().stream()
                .collect(Collectors.toMap(QuestionOptionDto::id, Function.identity()));

        List<QuestionOptionDto> selectedOptions = selectedIds.stream()
                .map(optionsById::get)
                .toList();

        if (selectedOptions.stream().anyMatch(option -> option == null)) {
            throw new IllegalArgumentException("Answer included an unknown option.");
        }

        boolean hasNone = selectedOptions.stream().anyMatch(option -> option.type() == QuestionOptionType.NONE_OF_THE_ABOVE);
        if (hasNone && selectedOptions.size() > 1) {
            throw new IllegalArgumentException("None of the above cannot be combined with other options.");
        }

        String answerText = selectedOptions.stream()
                .map(option -> {
                    if (option.type() == QuestionOptionType.ALL_OF_THE_ABOVE) {
                        String included = question.options().stream()
                                .filter(candidate -> candidate.type() == QuestionOptionType.STANDARD)
                                .map(QuestionOptionDto::label)
                                .collect(Collectors.joining("; "));
                        return "%s (includes: %s)".formatted(option.label(), included);
                    }
                    return option.label();
                })
                .collect(Collectors.joining("; "));

        return new AnswerSubmission(answerText, selectedIds);
    }

    private record AnswerSubmission(String answerText, List<String> selectedOptionIds) {
    }
}
