package com.nexus.NeuroForge.controllers.ai;

import com.nexus.NeuroForge.dto.aiChat.ChatRequest;
import com.nexus.NeuroForge.dto.aiChat.ChatResponse;
import com.nexus.NeuroForge.services.aiservice.AiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AiAssistantController {

    @Autowired private AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String reply = aiAssistantService.chat(request.getMessage(), request.getHistory());
        return new ChatResponse(reply);
    }
}