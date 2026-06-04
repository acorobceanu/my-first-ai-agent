package com.example.aptitude.service;

import com.example.aptitude.dto.AnsweredQuestionDto;
import com.example.aptitude.dto.AptitudeFindingsDto;
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
import java.util.List;
import java.util.UUID;

@Service
public class AptitudeSessionService {

    private static final TypeReference<List<AnsweredQuestionDto>> ANSWERS_TYPE = new TypeReference<>() {
    };

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
        AptitudeSession session = new AptitudeSession(UUID.randomUUID(), aiClient.firstQuestion());
        return toResponse(store.save(session));
    }

    @Transactional(readOnly = true)
    public SessionResponseDto getSession(UUID sessionId) {
        return store.findById(sessionId)
                .map(this::toResponse)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    @Transactional
    public SessionResponseDto answer(UUID sessionId, String answer) {
        AptitudeSession session = store.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new SessionAlreadyCompletedException(sessionId);
        }

        List<AnsweredQuestionDto> answers = new ArrayList<>(readAnswers(session));
        answers.add(new AnsweredQuestionDto(session.getCurrentQuestion(), answer));

        AptitudeAiDecision decision = aiClient.evaluate(answers, maxQuestions);
        boolean forcedCompletion = answers.size() >= maxQuestions;
        boolean completed = forcedCompletion || decision.ready();

        session.setAnswersJson(writeJson(answers));
        if (completed) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setCurrentQuestion(null);
            session.setFindingsJson(writeJson(decision.findings()));
        } else {
            session.setQuestionCount(answers.size() + 1);
            session.setCurrentQuestion(decision.nextQuestion());
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
}
