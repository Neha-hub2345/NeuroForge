package com.nexus.NeuroForge.services;

import com.nexus.NeuroForge.events.TaskEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class KafkaConsumerService {

    

    @KafkaListener(topics = "task-events", groupId = "neuroforge-task-events-group")
    public void consumeTaskEvent(TaskEvent event) {
        System.out.println("=========================================");
        System.out.println("🔔 KAFKA LISTENER RECEIVED NEW EVENT!");
        System.out.println("Event Type: " + event.getEventType());
        System.out.println("Task ID:    " + event.getTaskId());
        System.out.println("Message:    " + event.getMessage());
        System.out.println("Time:       " + event.getTimestamp());
        System.out.println("=========================================");

      
    }
}
