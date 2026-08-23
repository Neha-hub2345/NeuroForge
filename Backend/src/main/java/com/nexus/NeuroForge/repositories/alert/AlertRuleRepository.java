package com.nexus.NeuroForge.repositories.alert;

import com.nexus.NeuroForge.models.alert.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByProjectId(Long projectId);
    List<AlertRule> findByProjectIdAndEnabledTrue(Long projectId);
}