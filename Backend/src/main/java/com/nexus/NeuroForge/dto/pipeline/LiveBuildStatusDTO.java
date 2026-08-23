// LiveBuildStatusDTO.java — [Jashanpreet]
// Powers the "build in progress" log view on the Pipeline dashboard.
// Polled by the frontend (GET /api/pipelines/live?projectId=) while a build
// is running. Once runStatus == "completed" the frontend stops polling and
// falls back to the normal history table/detail view, which is populated
// separately by the finish webhook (PipelineWebhookRequest / recordBuildResult).
package com.nexus.NeuroForge.dto.pipeline;

public class LiveBuildStatusDTO {

    // false when there's no run to report on at all (nothing dispatched,
    // or GitHub hasn't registered the new run yet)
    public boolean active;

    public Long runId;
    public String runStatus;       // queued | in_progress | completed
    public String conclusion;      // success | failure | null while running
    public String currentStepName; // the step GitHub currently reports as in_progress
    public String branch;
    public String commitHash;
    public String htmlUrl;         // link to the run on GitHub

    public String logs;            // tail of the running job's raw log output
    public boolean truncated;      // true if logs were trimmed to the tail
}
