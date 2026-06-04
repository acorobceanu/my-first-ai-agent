package com.example.aptitude.service;

import com.example.aptitude.dto.AptitudeFindingsDto;
import com.example.aptitude.dto.ProfessionRecommendationDto;
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
        var response = service.answer(created.sessionId(), "I like analytical and creative work.");

        assertThat(response.status().name()).isEqualTo("IN_PROGRESS");
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
        var response = service.answer(created.sessionId(), "I enjoy data, communication, and strategy.");

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
        service.answer(created.sessionId(), "I enjoy data, communication, and strategy.");

        assertThatThrownBy(() -> service.answer(created.sessionId(), "Another answer"))
                .isInstanceOf(SessionNotFoundException.class);
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
        public String firstQuestion() {
            return "What activities make you lose track of time?";
        }

        @Override
        public AptitudeAiDecision evaluate(List<com.example.aptitude.dto.AnsweredQuestionDto> answers, int maxQuestions) {
            if (!ready) {
                return new AptitudeAiDecision(false, "What kind of problems do you enjoy solving?", null);
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
}
