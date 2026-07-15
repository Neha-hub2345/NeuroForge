package com.nexus.NeuroForge.services;

import com.nexus.NeuroForge.dto.TaskRequest;
import com.nexus.NeuroForge.events.TaskEvent;
import com.nexus.NeuroForge.models.Sprint;
import com.nexus.NeuroForge.models.Task;
import com.nexus.NeuroForge.repositories.SprintRepository;
import com.nexus.NeuroForge.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;

    @Autowired
    private  KafkaProducerService kafkaProducer;

    public TaskService(TaskRepository taskRepository, SprintRepository sprintRepository) {
        this.taskRepository = taskRepository;
        this.sprintRepository = sprintRepository;
    }

    public Task createTask(TaskRequest request) {
        Sprint sprint = sprintRepository.findById(request.getSprintId())
                .orElseThrow(() -> new RuntimeException("Sprint not found"));

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setPoints(request.getPoints());
        task.setStatus(request.getStatus());
        task.setAssigneeId(request.getAssigneeId());
        task.setSprint(sprint);

        return taskRepository.save(task);
    }

    public Task updateTaskStatus(Long taskId, String newStatus) {
    Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
    
    task.setStatus(newStatus);
        Task updatedTask = taskRepository.save(task);
        TaskEvent event = new TaskEvent(updatedTask.getId().toString(), "TASK_STATUS_UPDATED", "Task " + updatedTask.getId() + " is now " + newStatus);
        kafkaProducer.publishTaskEvent(event);

        return updatedTask;

}

    public Task assignUserToTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        task.setAssigneeId(userId);
        
        return taskRepository.save(task);
    }
    
    public List<Task> getTasksForSprint(Long sprintId) {
        return taskRepository.findBySprintId(sprintId);

    }
}