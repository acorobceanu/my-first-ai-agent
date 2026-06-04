package com.example.aptitude.dto;

import java.util.List;

public record ProfessionRecommendationDto(
        String profession,
        int confidence,
        List<String> reasons,
        List<String> strengths,
        List<String> growthAreas,
        List<String> nextSteps
) {
}
