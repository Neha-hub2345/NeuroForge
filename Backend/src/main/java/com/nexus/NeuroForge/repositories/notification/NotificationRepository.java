package com.nexus.NeuroForge.repositories.notification;

import com.nexus.NeuroForge.models.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // The underscore tells JPA to look for the "id" field inside the "User" entity
    List<Notification> findByUserId_IdOrderByCreatedAtDesc(Long id);
    List<Notification> findByUserId_IdAndReadFalse(Long id);
}