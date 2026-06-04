package com.example.aptitude.service;

import com.example.aptitude.model.AptitudeSession;

import java.util.Optional;
import java.util.UUID;

public interface AptitudeSessionStore {

    AptitudeSession save(AptitudeSession session);

    Optional<AptitudeSession> findById(UUID sessionId);
}
