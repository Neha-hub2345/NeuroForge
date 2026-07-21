package com.nexus.NeuroForge.services;

import com.nexus.NeuroForge.dto.SprintProgressDTO;
import com.nexus.NeuroForge.models.Task;
import com.nexus.NeuroForge.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SprintProgressService {

    @Autowired
    private TaskRepository taskRepository;

    public SprintProgressDTO getSprintProgress(Long sprintId) {
        List<Task> tasks = taskRepository.findBySprintId(sprintId);
        
        int totalTasks = tasks.size();
        int completedTasks = 0;
        int blockedTasks = 0;
        int totalPoints = 0;
        int completedPoints = 0;

        for (Task task : tasks) {
            totalPoints += task.getPoints();
            
            if (Boolean.TRUE.equals(task.getIsBlocked())) {
                blockedTasks++;
            }
            
            if ("DONE".equalsIgnoreCase(task.getStatus()) || "COMPLETED".equalsIgnoreCase(task.getStatus())) {
                completedTasks++;
                completedPoints += task.getPoints();
            }
        }

        double progressPercentage = totalPoints == 0 ? 0 : ((double) completedPoints / totalPoints) * 100;

        SprintProgressDTO dto = new SprintProgressDTO();
        dto.setTotalTasks(totalTasks);
        dto.setCompletedTasks(completedTasks);
        dto.setBlockedTasks(blockedTasks);
        dto.setTotalPoints(totalPoints);
        dto.setCompletedPoints(completedPoints);
        dto.setProgressPercentage(progressPercentage);

        return dto;
    }
}