package com.example.aptitude.service;

import com.example.aptitude.dto.AnsweredQuestionDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAiAptitudeClient implements AptitudeAiClient {

    private static final String SYSTEM_PROMPT = """
            You are an evidence-based career aptitude interviewer.
            Your job is to infer profession fit from the user's answers.
            Ask concise, neutral questions that reveal interests, work style, strengths, constraints, values, and learning appetite.
            Stop when you have a strong recommendation or when the max question count is reached.
            Do not diagnose medical, psychological, or protected-class traits.
            Return only structured data requested by the caller.
            """;

    private final ChatClient chatClient;

    public OpenAiAptitudeClient(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @Override
    public String firstQuestion() {
        return chatClient.prompt()
                .user("""
                        Create the first question for a career aptitude interview.
                        It should be broad, friendly, and answerable in a few sentences.
                        Return only the question text.
                        """)
                .call()
                .content();
    }

    @Override
    public AptitudeAiDecision evaluate(List<AnsweredQuestionDto> answers, int maxQuestions) {
        String userPrompt = """
                Review this career aptitude interview so far.

                Max questions allowed: %d
                Questions already asked: %d
                Answers:
                %s

                Decide whether you have enough evidence for a strong profession suggestion.
                If Questions already asked is equal to or greater than Max questions allowed, ready must be true.
                If ready is false, provide exactly one nextQuestion.
                If ready is true, provide findings with:
                - a concise summary
                - 3 profession recommendations ranked best first
                - confidence from 0 to 100 for each recommendation
                - reasons, strengths, growth areas, next steps
                - cross-cutting strengths and cautions

                Return a JSON object matching this shape:
                {
                  "ready": true,
                  "nextQuestion": null,
                  "findings": {
                    "summary": "...",
                    "recommendations": [
                      {
                        "profession": "...",
                        "confidence": 90,
                        "reasons": ["..."],
                        "strengths": ["..."],
                        "growthAreas": ["..."],
                        "nextSteps": ["..."]
                      }
                    ],
                    "crossCuttingStrengths": ["..."],
                    "cautions": ["..."]
                  }
                }
                """.formatted(maxQuestions, answers.size(), answers);

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .entity(AptitudeAiDecision.class);
    }
}
