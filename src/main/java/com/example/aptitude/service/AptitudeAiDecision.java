package com.example.aptitude.service;

import com.example.aptitude.dto.AptitudeFindingsDto;

public record AptitudeAiDecision(
        boolean ready,
        String nextQuestion,
        AptitudeFindingsDto findings
) {
}
