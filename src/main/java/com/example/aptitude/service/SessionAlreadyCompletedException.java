package com.example.aptitude.service;

import java.util.UUID;

public class SessionAlreadyCompletedException extends RuntimeException {

    public SessionAlreadyCompletedException(UUID sessionId) {
        super("Aptitude session is already completed: " + sessionId);
    }
}
