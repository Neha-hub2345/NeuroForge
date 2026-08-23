package com.nexus.NeuroForge.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Used for both the GitHub Actions dispatch calls (PipelineService)
        // and the health-check poller (ExternalHealthMonitorService). 5s
        // connect / 10s read is generous for either, and caps how long a
        // dead monitor target can occupy a scheduler thread.
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}