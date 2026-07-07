package com.nexus.NeuroForge.models;

import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import jakarta.persistence.*;

@Entity
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DeploymentEnvironment environment;

    private boolean success;

    public Deployment() {
    }

    public Deployment(Long id, DeploymentEnvironment environment, boolean success) {
        this.id = id;
        this.environment = environment;
        this.success = success;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DeploymentEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(DeploymentEnvironment environment) {
        this.environment = environment;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}