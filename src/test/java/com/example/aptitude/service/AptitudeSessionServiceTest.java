package com.example.aptitude.service;

import com.example.aptitude.dto.AnswerDto;
import com.example.aptitude.dto.AptitudeFindingsDto;
import com.example.aptitude.dto.ProfessionRecommendationDto;
import com.example.aptitude.dto.QuestionDto;
import com.example.aptitude.dto.QuestionOptionDto;
import com.example.aptitude.dto.QuestionOptionType;
import com.example.aptitude.dto.QuestionType;
import com.example.aptitude.model.AptitudeSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AptitudeSessionServiceTest {

    @Test
    void asksNextQuestionWhenAiIsNotReady() {
        AptitudeSessionService service = new AptitudeSessionService(
                new InMemoryStore(),
                new StubAiClient(false),
                new ObjectMapper(),
                15
        );

        var created = service.createSession();
        var response = service.answer(created.sessionId(), new AnswerDto("I like analytical and creative work.", List.of()));

        assertThat(response.status().name()).isEqualTo("IN_PROGRESS");
        assertThat(response.question().prompt()).isEqualTo("What kind of problems do you enjoy solving?");
        assertThat(response.currentQuestion()).isEqualTo("What kind of problems do you enjoy solving?");
        assertThat(response.answers()).hasSize(1);
    }

    @Test
    void completesWhenAiIsReady() {
        InMemoryStore store = new InMemoryStore();
        AptitudeSessionService service = new AptitudeSessionService(
                store,
                new StubAiClient(true),
                new ObjectMapper(),
                15
        );

        var created = service.createSession();
        var response = service.answer(created.sessionId(), new AnswerDto("I enjoy data, communication, and strategy.", List.of()));

        assertThat(response.status().name()).isEqualTo("COMPLETED");
        assertThat(response.currentQuestion()).isNull();
        assertThat(response.findings().recommendations()).hasSize(1);
        assertThat(response.findings().recommendations().getFirst().profession()).isEqualTo("Product Manager");
        assertThat(store.findById(created.sessionId())).isEmpty();
    }

    @Test
    void deletesSessionAfterCompletion() {
        AptitudeSessionService service = new AptitudeSessionService(
                new InMemoryStore(),
                new StubAiClient(true),
                new ObjectMapper(),
                15
        );

        var created = service.createSession();
        service.answer(created.sessionId(), new AnswerDto("I enjoy data, communication, and strategy.", List.of()));

        assertThatThrownBy(() -> service.answer(created.sessionId(), new AnswerDto("Another answer", List.of())))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void storesMultipleChoiceSelectionsAsAnswerEvidence() {
        QuestionDto question = new QuestionDto(
                QuestionType.MULTIPLE_CHOICE,
                "Which tasks energize you?",
                List.of(
                        new QuestionOptionDto("analysis", "Analyzing patterns", QuestionOptionType.STANDARD),
                        new QuestionOptionDto("teaching", "Teaching people", QuestionOptionType.STANDARD),
                        new QuestionOptionDto("none", "None of the above", QuestionOptionType.NONE_OF_THE_ABOVE)
                )
        );
        AptitudeSessionService service = new AptitudeSessionService(
                new InMemoryStore(),
                new QuestionStubAiClient(question),
                new ObjectMapper(),
                15
        );

        var created = service.createSession();
        var response = service.answer(created.sessionId(), new AnswerDto("", List.of("analysis", "teaching")));

        assertThat(response.answers()).hasSize(1);
        assertThat(response.answers().getFirst().answer()).isEqualTo("Analyzing patterns; Teaching people");
        assertThat(response.answers().getFirst().selectedOptionIds()).containsExactly("analysis", "teaching");
    }

    @Test
    void rejectsNoneOfTheAboveWithOtherSelections() {
        QuestionDto question = new QuestionDto(
                QuestionType.MULTIPLE_CHOICE,
                "Which tasks energize you?",
                List.of(
                        new QuestionOptionDto("analysis", "Analyzing patterns", QuestionOptionType.STANDARD),
                        new QuestionOptionDto("none", "None of the above", QuestionOptionType.NONE_OF_THE_ABOVE)
                )
        );
        AptitudeSessionService service = new AptitudeSessionService(
                new InMemoryStore(),
                new QuestionStubAiClient(question),
                new ObjectMapper(),
                15
        );

        var created = service.createSession();

        assertThatThrownBy(() -> service.answer(created.sessionId(), new AnswerDto("", List.of("analysis", "none"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("None of the above cannot be combined with other options.");
    }

    @Test
    void replacesRepeatedAiQuestionWithFallbackQuestion() {
        AptitudeSessionService service = new AptitudeSessionService(
                new InMemoryStore(),
                new RepeatingQuestionAiClient(),
                new ObjectMapper(),
                15
        );

        var created = service.createSession();
        var response = service.answer(created.sessionId(), new AnswerDto("Building practical tools.", List.of()));

        assertThat(response.currentQuestion()).isNotEqualTo("Which kinds of tasks reliably energize you?");
        assertThat(response.currentQuestion()).isEqualTo("What kind of work environment helps you do your best work?");
    }


    private static final class InMemoryStore implements AptitudeSessionStore {
        private final Map<UUID, AptitudeSession> sessions = new LinkedHashMap<>();

        @Override
        public AptitudeSession save(AptitudeSession session) {
            sessions.put(session.getId(), session);
            return session;
        }

        @Override
        public Optional<AptitudeSession> findById(UUID sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public void delete(AptitudeSession session) {
            sessions.remove(session.getId());
        }
    }

    private static final class StubAiClient implements AptitudeAiClient {
        private final boolean ready;

        private StubAiClient(boolean ready) {
            this.ready = ready;
        }

        @Override
        public QuestionDto firstQuestion() {
            return QuestionDto.freeText("What activities make you lose track of time?");
        }

        @Override
        public AptitudeAiDecision evaluate(List<com.example.aptitude.dto.AnsweredQuestionDto> answers, int maxQuestions) {
            if (!ready) {
                return new AptitudeAiDecision(
                        false,
                        QuestionDto.freeText("What kind of problems do you enjoy solving?"),
                        null
                );
            }
            var recommendation = new ProfessionRecommendationDto(
                    "Product Manager",
                    91,
                    List.of("Balances analytical thinking with communication."),
                    List.of("Systems thinking", "Prioritization"),
                    List.of("Deepen technical fluency"),
                    List.of("Interview product managers", "Build a small product case study")
            );
            var findings = new AptitudeFindingsDto(
                    "Strong fit for cross-functional product work.",
                    List.of(recommendation),
                    List.of("Communication", "Structured reasoning"),
                    List.of("Validate the recommendation with real-world exposure.")
            );
            return new AptitudeAiDecision(true, null, findings);
        }
    }

    private static final class QuestionStubAiClient implements AptitudeAiClient {
        private final QuestionDto question;

        private QuestionStubAiClient(QuestionDto question) {
            this.question = question;
        }

        @Override
        public QuestionDto firstQuestion() {
            return question;
        }

        @Override
        public AptitudeAiDecision evaluate(List<com.example.aptitude.dto.AnsweredQuestionDto> answers, int maxQuestions) {
            return new AptitudeAiDecision(false, QuestionDto.freeText("What did you like about that?"), null);
        }
    }

    private static final class RepeatingQuestionAiClient implements AptitudeAiClient {
        @Override
        public QuestionDto firstQuestion() {
            return QuestionDto.freeText("Which kinds of tasks reliably energize you?");
        }

        @Override
        public AptitudeAiDecision evaluate(List<com.example.aptitude.dto.AnsweredQuestionDto> answers, int maxQuestions) {
            return new AptitudeAiDecision(
                    false,
                    QuestionDto.freeText("Which kinds of tasks reliably energize you?"),
                    null
            );
        }
    }
}
