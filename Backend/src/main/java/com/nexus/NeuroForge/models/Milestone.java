package com.nexus.NeuroForge.models;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Entity
public class Milestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;       // "Release 2.3"
    private LocalDate dueDate;
    private boolean completed;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
}
