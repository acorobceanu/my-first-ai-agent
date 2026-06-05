package com.example.aptitude.controller;

import com.example.aptitude.dto.AnswerDto;
import com.example.aptitude.dto.SessionResponseDto;
import com.example.aptitude.service.AptitudeSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/aptitude/sessions")
@Tag(name = "Aptitude Sessions")
public class AptitudeController {

    private final AptitudeSessionService service;

    public AptitudeController(AptitudeSessionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new aptitude interview session")
    public SessionResponseDto createSession() {
        return service.createSession();
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get an aptitude interview session")
    public SessionResponseDto getSession(@PathVariable UUID sessionId) {
        return service.getSession(sessionId);
    }

    @PostMapping("/{sessionId}/answers")
    @Operation(summary = "Submit an answer and receive either the next question or final findings")
    public SessionResponseDto answer(@PathVariable UUID sessionId, @Valid @RequestBody AnswerDto answer) {
        return service.answer(sessionId, answer);
    }
}
