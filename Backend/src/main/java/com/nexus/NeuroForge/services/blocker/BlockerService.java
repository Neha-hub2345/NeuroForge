package com.nexus.NeuroForge.services.blocker;

import com.nexus.NeuroForge.models.blocker.Blocker;
import com.nexus.NeuroForge.models.project.Project;
import com.nexus.NeuroForge.models.sprint.Sprint;
import com.nexus.NeuroForge.models.task.Task;
import com.nexus.NeuroForge.repositories.blocker.BlockerRepository;
import com.nexus.NeuroForge.repositories.project.ProjectRepository;
import com.nexus.NeuroForge.repositories.sprint.SprintRepository;
import com.nexus.NeuroForge.repositories.task.TaskRepository;
import com.nexus.NeuroForge.services.notification.NotificationService;
import com.nexus.NeuroForge.services.security.ProjectAccessGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BlockerService {

    @Autowired
    private BlockerRepository blockerRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ProjectAccessGuard projectAccessGuard;

    public List<Blocker> getBlockersBySprint(Long sprintId) {
        return blockerRepository.findBySprintId(sprintId);
    }

    @Transactional
    public Blocker raiseBlocker(Long sprintId, Blocker request,Jwt jwt) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));

        request.setSprint(sprint);
        Blocker savedBlocker = blockerRepository.save(request);

        // SYNC: Find the Task and mark it as blocked
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found"));

        projectAccessGuard.assertMember(jwt, resolveProject(task));

        task.setIsBlocked(true);
        taskRepository.save(task);

        // NOTIFICATION FIX: direct replacement for the old Kafka event publish.
        notificationService.createNotification(
                task,
                "BLOCKER_RAISED",
                "A blocker was raised on task: " + savedBlocker.getTaskTitle()
        );

        return savedBlocker;
    }

    @Transactional
    public Blocker resolveBlocker(Long sprintId, Long blockerId, Jwt jwt) {
        Blocker blocker = blockerRepository.findById(blockerId)
                .orElseThrow(() -> new RuntimeException("Blocker not found"));

        blocker.setResolved(true);
        Blocker savedBlocker = blockerRepository.save(blocker);

        // SYNC: Find the Task and unblock it
        Task task = taskRepository.findById(savedBlocker.getTaskId()).orElse(null);
        if (task != null) {
            task.setIsBlocked(false);
            taskRepository.save(task);

            // NOTIFICATION FIX: direct replacement for the old Kafka event publish.
            notificationService.createNotification(
                    task,
                    "BLOCKER_RESOLVED",
                    "The blocker on task was resolved: " + savedBlocker.getTaskTitle()
            );
        }

        return savedBlocker;
    }

    private Project resolveProject(Task task) {
        return task.getSprint() != null ? task.getSprint().getProject() : projectRepository.findById(task.getProject()).orElse(null);
    }
}