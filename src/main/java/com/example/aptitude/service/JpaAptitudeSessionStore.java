package com.example.aptitude.service;

import com.example.aptitude.model.AptitudeSession;
import com.example.aptitude.repository.AptitudeSessionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaAptitudeSessionStore implements AptitudeSessionStore {

    private final AptitudeSessionRepository repository;

    public JpaAptitudeSessionStore(AptitudeSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public AptitudeSession save(AptitudeSession session) {
        return repository.save(session);
    }

    @Override
    public Optional<AptitudeSession> findById(UUID sessionId) {
        return repository.findById(sessionId);
    }
}
