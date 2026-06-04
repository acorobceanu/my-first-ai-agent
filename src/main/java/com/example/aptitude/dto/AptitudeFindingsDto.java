package com.example.aptitude.dto;

import java.util.List;

public record AptitudeFindingsDto(
        String summary,
        List<ProfessionRecommendationDto> recommendations,
        List<String> crossCuttingStrengths,
        List<String> cautions
) {
}
