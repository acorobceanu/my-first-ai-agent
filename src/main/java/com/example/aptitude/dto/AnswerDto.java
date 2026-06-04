package com.example.aptitude.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerDto(
        @Schema(example = "I enjoy breaking down messy problems, helping people understand options, and building small tools.")
        @NotBlank
        @Size(max = 2000)
        String answer
) {
}
