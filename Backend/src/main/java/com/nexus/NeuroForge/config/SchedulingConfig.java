package com.nexus.NeuroForge.config;

// [M4][Jashanpreet] Without this, @EnableScheduling defaults to a single
// thread shared by every @Scheduled method. That was fine when the only
// scheduled work was DB-only (AlertMonitoringService, KpiSnapshotScheduler).
// ExternalHealthMonitorService adds outbound HTTP calls on the same 30s
// cadence as alert evaluation — a slow or hanging health check on one
// project would otherwise delay alert evaluation for every other project.
// A small dedicated pool keeps the schedulers independent.

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("neuroforge-scheduled-");
        scheduler.setErrorHandler(t ->
                System.err.println("[SchedulingConfig] Uncaught error in scheduled task: " + t.getMessage()));
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }
}
