// src/main/java/com/nexus/NeuroForge/repositories/BlockerRepository.java
package com.nexus.NeuroForge.repositories.blocker;

import com.nexus.NeuroForge.models.blocker.Blocker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BlockerRepository extends JpaRepository<Blocker, Long> {
    List<Blocker> findBySprintId(Long sprintId);
    long countByResolvedFalse();

}