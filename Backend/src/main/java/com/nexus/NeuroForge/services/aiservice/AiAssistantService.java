package com.nexus.NeuroForge.services.aiservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.NeuroForge.dto.aiChat.ChatMessageDTO;
import com.nexus.NeuroForge.services.kpi.WorkspaceSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// [M4] Calls the Groq Chat Completions API (OpenAI-compatible schema)
// server-side, grounded in a fresh WorkspaceSnapshotService.buildSnapshot()
// every turn so answers always reflect live data, not a stale snapshot.
@Service
public class AiAssistantService {

    @Autowired private RestTemplate restTemplate;
    @Autowired private WorkspaceSnapshotService snapshotService;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    @Value("${groq.api.url}")
    private String apiUrl;
    @Value("${groq.max-tokens}")
    private int maxTokens;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String chat(String userMessage, List<ChatMessageDTO> history) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "AI assistant is not configured — set GROQ_API_KEY in your environment.");
        }

        String systemPrompt =
                "You are the NeuroForge Nexus workspace assistant. You help the user understand the " +
                        "current state of their projects, sprints, CI/CD pipelines, releases, and blockers. " +
                        "Only use the CURRENT WORKSPACE SNAPSHOT below as your source of truth — don't invent " +
                        "data that isn't in it. Be concise and specific (numbers, names, statuses). If asked for " +
                        "a general summary, cover: active projects, open blockers, pipeline health, and release/" +
                        "environment health, in a few short bullet points.\n\n" +
                        "CURRENT WORKSPACE SNAPSHOT:\n" + snapshotService.buildSnapshot();

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) {
            for (ChatMessageDTO m : history) {
                messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "max_tokens", maxTokens,
                "temperature", 0.4
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

        try {
            JsonNode root = MAPPER.readTree(response.getBody());
            String text = root.path("choices").path(0).path("message").path("content").asText();
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("AI assistant returned an empty response.");
            }
            return text;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse AI assistant response: " + e.getMessage());
        }
    }
}