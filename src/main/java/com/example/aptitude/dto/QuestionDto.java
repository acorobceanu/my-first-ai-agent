package com.example.aptitude.dto;

import java.util.List;

public record QuestionDto(
        QuestionType type,
        String prompt,
        List<QuestionOptionDto> options
) {
    public QuestionDto {
        type = type == null ? QuestionType.FREE_TEXT : type;
        prompt = prompt == null ? "" : prompt.trim();
        options = options == null ? List.of() : List.copyOf(options);

        if (type == QuestionType.FREE_TEXT) {
            options = List.of();
        }
    }

    public static QuestionDto freeText(String prompt) {
        return new QuestionDto(QuestionType.FREE_TEXT, prompt, List.of());
    }
}
