package com.example.aptitude.service;

import com.example.aptitude.dto.AptitudeFindingsDto;
import com.example.aptitude.dto.QuestionDto;

public record AptitudeAiDecision(
        boolean ready,
        QuestionDto nextQuestion,
        AptitudeFindingsDto findings
) {
}
