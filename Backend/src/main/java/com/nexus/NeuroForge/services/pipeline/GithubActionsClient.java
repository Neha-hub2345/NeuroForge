// GithubActionsClient.java — [Jashanpreet]
// Thin wrapper around the GitHub Actions REST API, used to power the live
// "build in progress" log view. Kept separate from PipelineService's
// dispatchWorkflow() call because this only *reads* run/job state — it
// never triggers anything — so the two concerns (trigger vs observe) stay
// apart.
package com.nexus.NeuroForge.services.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.NeuroForge.models.project.ProjectIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class GithubActionsClient {

    @Autowired private RestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    public static class RunInfo {
        public long id;
        public String status;      // queued | in_progress | completed
        public String conclusion;  // null while status != completed
        public String headBranch;
        public String headSha;
        public String htmlUrl;
    }

    public static class JobInfo {
        public long id;
        public String status;
        public String conclusion;
        public List<StepInfo> steps = new ArrayList<>();
    }

    public static class StepInfo {
        public String name;
        public String status;
        public String conclusion;
    }

    /**
     * Most recent run for the integration's configured branch + workflow
     * file. Used both to detect a freshly-dispatched run and to know when
     * it finishes.
     */
    public RunInfo getLatestRun(ProjectIntegration integration, String token) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/actions/workflows/%s/runs?branch=%s&per_page=1",
                integration.getGithubOwner(), integration.getGithubRepo(),
                integration.getWorkflowFile(), integration.getGithubBranch());

        JsonNode body = get(url, token);
        JsonNode runs = body.path("workflow_runs");
        if (!runs.isArray() || runs.isEmpty()) return null;

        JsonNode run = runs.get(0);
        RunInfo info = new RunInfo();
        info.id = run.path("id").asLong();
        info.status = run.path("status").asText(null);
        info.conclusion = run.path("conclusion").isNull() ? null : run.path("conclusion").asText(null);
        info.headBranch = run.path("head_branch").asText(null);
        info.headSha = run.path("head_sha").asText(null);
        info.htmlUrl = run.path("html_url").asText(null);
        return info;
    }

    /** Our workflow (ci-cd.yml) only has one job — build-test-deploy. */
    public JobInfo getPrimaryJob(ProjectIntegration integration, long runId, String token) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/actions/runs/%d/jobs",
                integration.getGithubOwner(), integration.getGithubRepo(), runId);

        JsonNode body = get(url, token);
        JsonNode jobs = body.path("jobs");
        if (!jobs.isArray() || jobs.isEmpty()) return null;

        JsonNode job = jobs.get(0);
        JobInfo info = new JobInfo();
        info.id = job.path("id").asLong();
        info.status = job.path("status").asText(null);
        info.conclusion = job.path("conclusion").isNull() ? null : job.path("conclusion").asText(null);
        for (JsonNode s : job.path("steps")) {
            StepInfo step = new StepInfo();
            step.name = s.path("name").asText(null);
            step.status = s.path("status").asText(null);
            step.conclusion = s.path("conclusion").isNull() ? null : s.path("conclusion").asText(null);
            info.steps.add(step);
        }
        return info;
    }

    /**
     * Raw plain-text logs for a job. This works while the job is still
     * running — GitHub streams back whatever output has been captured so
     * far, not just logs for completed jobs — which is what lets us tail a
     * build live instead of waiting for the finish webhook. Returns "" if
     * GitHub hasn't produced any output yet (e.g. the job is still queued
     * on a runner).
     */
    public String getJobLogs(ProjectIntegration integration, long jobId, String token) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/actions/jobs/%d/logs",
                integration.getGithubOwner(), integration.getGithubRepo(), jobId);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
            return resp.getBody() != null ? resp.getBody() : "";
        } catch (HttpClientErrorException.NotFound e) {
            return "";
        }
    }

    private JsonNode get(String url, String token) {
        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
        try {
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse GitHub API response from " + url, e);
        }
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        return headers;
    }
}
