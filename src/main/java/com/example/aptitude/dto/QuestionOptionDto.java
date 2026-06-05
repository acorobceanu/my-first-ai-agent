package com.example.aptitude.dto;

import java.util.Locale;

public record QuestionOptionDto(
        String id,
        String label,
        QuestionOptionType type
) {
    public QuestionOptionDto {
        id = cleanId(id);
        label = label == null ? "" : label.trim();
        type = type == null ? QuestionOptionType.STANDARD : type;
    }

    private static String cleanId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }
}
