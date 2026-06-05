package com.example.aptitude.dto;

import com.example.aptitude.model.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionResponseDto(
        UUID sessionId,
        SessionStatus status,
        int questionCount,
        int maxQuestions,
        QuestionDto question,
        String currentQuestion,
        List<AnsweredQuestionDto> answers,
        AptitudeFindingsDto findings,
        Instant createdAt,
        Instant updatedAt
) {
}
