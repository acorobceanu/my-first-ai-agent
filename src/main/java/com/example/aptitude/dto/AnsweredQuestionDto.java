package com.example.aptitude.dto;

import java.util.List;

public record AnsweredQuestionDto(
        String question,
        String answer,
        QuestionDto questionDetails,
        List<String> selectedOptionIds
) {
    public AnsweredQuestionDto(String question, String answer) {
        this(question, answer, null, List.of());
    }

    public AnsweredQuestionDto {
        selectedOptionIds = selectedOptionIds == null ? List.of() : List.copyOf(selectedOptionIds);
    }
}
