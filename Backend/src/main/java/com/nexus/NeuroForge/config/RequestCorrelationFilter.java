package com.nexus.NeuroForge.config;

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
