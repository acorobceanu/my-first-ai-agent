package com.example.aptitude.service;

import com.example.aptitude.dto.AnsweredQuestionDto;
import com.example.aptitude.dto.QuestionDto;

import java.util.List;

public interface AptitudeAiClient {

    QuestionDto firstQuestion();

    AptitudeAiDecision evaluate(List<AnsweredQuestionDto> answers, int maxQuestions);
}
