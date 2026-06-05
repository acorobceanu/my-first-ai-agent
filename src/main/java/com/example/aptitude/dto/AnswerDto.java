package com.example.aptitude.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AnswerDto(
        @Schema(example = "I enjoy breaking down messy problems, helping people understand options, and building small tools.")
        @Size(max = 2000)
        String answer,

        @Schema(example = "[\"analytical\", \"creative\"]")
        @Size(max = 20)
        List<@Size(max = 80) String> selectedOptionIds
) {
    public AnswerDto {
        selectedOptionIds = selectedOptionIds == null ? List.of() : List.copyOf(selectedOptionIds);
    }
}
