package com.nexus.NeuroForge.models;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Entity
public class Sprint {
     @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;       // "Sprint 12"
    private int taskCount;
    private int storyPoints;
    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne @JoinColumn(name="project_id")
    private Project project;



}
