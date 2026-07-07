package com.nexus.NeuroForge.models;

import com.nexus.NeuroForge.models.interfaces.PipelineStatus;
import jakarta.persistence.*;

@Entity
public class Pipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PipelineStatus status;

    private int duration;

    public Pipeline() {
    }

    public Pipeline(Long id, PipelineStatus status, int duration) {
        this.id = id;
        this.status = status;
        this.duration = duration;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PipelineStatus getStatus() {
        return status;
    }

    public void setStatus(PipelineStatus status) {
        this.status = status;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}