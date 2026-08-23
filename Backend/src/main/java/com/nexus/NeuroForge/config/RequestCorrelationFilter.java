package com.nexus.NeuroForge.config;

// [M4][Jashanpreet] Step 7 — ELK now has somewhere real to send logs to
// (see logback-spring.xml's LOGSTASH appender + the elasticsearch/logstash/
// kibana services in docker-compose.yml) but almost nothing in the app
// actually logs anything domain-specific yet, so Kibana would be near-empty
// of anything useful. This filter is the first piece: it stamps every HTTP
// request with a requestId (and method/path) in MDC, which LogstashEncoder
// automatically promotes to structured fields on every log line written
// during that request — so a webhook call that triggers several log lines
// across different classes/threads can be grouped with one Kibana filter
// instead of grepping message text.
//
// CHANGED: two gaps found via the Releases & Monitoring "Open log trail in
// Kibana" link returning nothing for a real project.
//  1. `projectId` was only ever set in MDC by a handful of write/background
//     paths (release cut/rollback, alert eval, health poll, webhooks) —
//     never by the plain GET reads the dashboard itself makes
//     (getKpis/getHistory/getActiveRelease/getAlerts/observability/...),
//     even though every one of those takes `?projectId=` already. Now
//     pulled from the query param here, once, for every request, so it
//     doesn't need to be duplicated per-controller.
//  2. Those same GET endpoints don't log anything at all, so even with
//     projectId in MDC there was no line to find. Added a single INFO
//     access-log line per request (method, path, status, duration) so
//     "viewing the dashboard" actually produces something to filter on.
//
// Note: this only catches `projectId` passed as a query parameter, which
// covers every read this dashboard makes. A few other endpoints
// (e.g. /api/pipelines/trigger/{projectId}, /api/tasks/backlog/{projectId})
// carry it as a path segment instead and won't be tagged by this — those
// already set MDC themselves where it mattered, or can be added the same
// way if they need to show up in a project's log trail too.
//
// @Order(1) so this runs before Spring Security's filter chain — an
// unauthenticated/rejected request should still carry a requestId if it
// ever gets logged (e.g. a rejected webhook signature).

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class RequestCorrelationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        MDC.put("requestId", UUID.randomUUID().toString());
        long startNanos = System.nanoTime();
        try {
            if (request instanceof HttpServletRequest http) {
                MDC.put("httpMethod", http.getMethod());
                MDC.put("path", http.getRequestURI());

                String projectId = http.getParameter("projectId");
                if (projectId != null && !projectId.isBlank()) {
                    MDC.put("projectId", projectId);
                }
            }
            chain.doFilter(request, response);
        } finally {
            if (request instanceof HttpServletRequest http && response instanceof HttpServletResponse resp) {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                log.info("{} {} -> {} ({}ms)", http.getMethod(), http.getRequestURI(), resp.getStatus(), durationMs);
            }
            // Clear everything, not just the keys set above — this thread
            // may be reused from a pool, and a handler further down may
            // have added its own MDC entries (e.g. projectId) that must
            // not leak into the next unrelated request on the same thread.
            MDC.clear();
        }
    }
}
