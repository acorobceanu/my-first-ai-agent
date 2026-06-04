package com.example.aptitude.service;

import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("Aptitude session not found: " + sessionId);
    }
}
