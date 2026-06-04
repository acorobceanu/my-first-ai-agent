package com.example.aptitude.repository;

import com.example.aptitude.model.AptitudeSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AptitudeSessionRepository extends JpaRepository<AptitudeSession, UUID> {
}
