package com.example.aptitude.service;

import com.example.aptitude.dto.AnsweredQuestionDto;

import java.util.List;

public interface AptitudeAiClient {

    String firstQuestion();

    AptitudeAiDecision evaluate(List<AnsweredQuestionDto> answers, int maxQuestions);
}
