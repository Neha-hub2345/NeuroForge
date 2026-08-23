package com.nexus.NeuroForge.services.kpi;

import com.nexus.NeuroForge.dto.pipeline.PipelineKpiDTO;
import com.nexus.NeuroForge.dto.pipeline.PipelineResponse;
import com.nexus.NeuroForge.dto.project.ProjectResponse;
import com.nexus.NeuroForge.dto.release.ReleaseKpiDTO;
import com.nexus.NeuroForge.dto.release.ReleaseResponse;
import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import com.nexus.NeuroForge.repositories.blocker.BlockerRepository;
import com.nexus.NeuroForge.repositories.team.TeamRepository;
import com.nexus.NeuroForge.repositories.user.UserRepository;
import com.nexus.NeuroForge.services.pipeline.PipelineService;
import com.nexus.NeuroForge.services.project.ProjectService;
import com.nexus.NeuroForge.services.releases.ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// [M4] Builds the textual "world state" the AI assistant is grounded in.
// Pipeline/Release data is scoped per-project, so this loops over every
// project and aggregates — kept compact (a handful of KPIs + last few items)
// rather than dumping full entity lists, to keep token usage low.
@Service
public class WorkspaceSnapshotService {

    @Autowired private ProjectService projectService;
    @Autowired private TeamRepository teamRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BlockerRepository blockerRepository;
    @Autowired private PipelineService pipelineService;
    @Autowired private ReleaseService releaseService;

    public String buildSnapshot() {
        StringBuilder sb = new StringBuilder();

        List<ProjectResponse> projects = projectService.getAll();
        long activeProjects = projects.stream().filter(p -> "ACTIVE".equals(p.getStatus().name())).count();
        sb.append("PROJECTS (").append(projects.size()).append(" total, ")
                .append(activeProjects).append(" active):\n");
        projects.stream().limit(8).forEach(p -> sb.append("- ")
                .append(p.getName()).append(" [").append(p.getStatus()).append("] team=")
                .append(p.getTeamName()).append(" manager=").append(p.getManagerUsername())
                .append("\n"));

        sb.append("\nWORKSPACE: ").append(teamRepository.count()).append(" teams, ")
                .append(userRepository.count()).append(" users.\n");

        sb.append("\nOPEN BLOCKERS: ").append(blockerRepository.countByResolvedFalse()).append("\n");

        appendPipelineSummary(sb, projects);
        appendReleaseSummary(sb, projects);

        return sb.toString();
    }

    // --- Pipelines: aggregate KPIs across all projects, tag recent builds by project name ---
    private void appendPipelineSummary(StringBuilder sb, List<ProjectResponse> projects) {
        long totalBuilds = 0;
        long buildsToday = 0;
        double weightedSuccessSum = 0; // successRate * totalBuilds, so the aggregate isn't skewed toward small projects
        double weightedDeploySum = 0;
        List<String> recentBuildLines = new ArrayList<>();

        for (ProjectResponse project : projects) {
            try {
                PipelineKpiDTO pk = pipelineService.getKpis(project.getId());
                totalBuilds += pk.getTotalBuilds();
                buildsToday += pk.getBuildsToday();
                weightedSuccessSum += pk.getSuccessRate() * pk.getTotalBuilds();
                weightedDeploySum += pk.getAvgDeployTimeMinutes() * pk.getTotalBuilds();

                List<PipelineResponse> history = pipelineService.getHistory(project.getId());
                history.stream()
                        .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                        .limit(2) // a couple per project keeps the overall list short
                        .forEach(b -> recentBuildLines.add("- [" + project.getName() + "] #" + b.getId()
                                + " " + b.getStatus() + " on " + b.getBranch()
                                + " (" + b.getEnvironment() + ") started " + b.getStartedAt()));
            } catch (Exception ignored) {
                // project has no pipeline data yet — skip it silently
            }
        }

        if (totalBuilds == 0) {
            sb.append("\nPIPELINE KPIs: no builds yet across any project\n");
            return;
        }

        double aggSuccessRate = weightedSuccessSum / totalBuilds;
        double aggAvgDeploy = weightedDeploySum / totalBuilds;

        sb.append("\nPIPELINE KPIs (all projects): total builds=").append(totalBuilds)
                .append(", success rate=").append(String.format("%.1f", aggSuccessRate)).append("%")
                .append(", avg deploy=").append(String.format("%.1f", aggAvgDeploy)).append("min")
                .append(", builds today=").append(buildsToday).append("\n");

        sb.append("Recent builds:\n");
        recentBuildLines.stream().limit(6).forEach(line -> sb.append(line).append("\n"));
    }

    // --- Releases: aggregate KPIs across all projects, environment health per project ---
    private void appendReleaseSummary(StringBuilder sb, List<ProjectResponse> projects) {
        long totalReleases = 0;
        long releasesThisMonth = 0;
        long rolledBack = 0;
        double weightedUptimeSum = 0;
        double weightedMttrSum = 0;
        List<String> envHealthLines = new ArrayList<>();
        List<String> recentReleaseLines = new ArrayList<>();

        for (ProjectResponse project : projects) {
            try {
                ReleaseKpiDTO rk = releaseService.getKpis(project.getId());
                totalReleases += rk.totalReleases;
                releasesThisMonth += rk.releasesThisMonth;
                rolledBack += rk.rolledBackReleases;
                weightedUptimeSum += rk.uptimePercent * rk.totalReleases;
                weightedMttrSum += rk.mttrMinutes * rk.totalReleases;

                for (DeploymentEnvironment env : DeploymentEnvironment.values()) {
                    try {
                        ReleaseResponse active = releaseService.toResponse(
                                releaseService.getActiveRelease(project.getId(), env));
                        envHealthLines.add("- [" + project.getName() + "] " + env + ": " + active.version
                                + " [" + active.status + "] slot=" + active.slot);
                    } catch (Exception noneActive) {
                        // no active release in this env for this project — normal, skip
                    }
                }

                List<ReleaseResponse> history = releaseService.getHistory(project.getId());
                history.stream().limit(2).forEach(r -> recentReleaseLines.add("- [" + project.getName() + "] "
                        + r.version + " " + r.environment + " [" + r.status + "] active=" + r.active));
            } catch (Exception ignored) {
                // project has no release data yet — skip it silently
            }
        }

        if (totalReleases == 0) {
            sb.append("\nRELEASE KPIs: no releases yet across any project\n");
            return;
        }

        double aggUptime = weightedUptimeSum / totalReleases;
        double aggMttr = weightedMttrSum / totalReleases;

        sb.append("\nRELEASE KPIs (all projects): uptime=").append(String.format("%.2f", aggUptime)).append("%")
                .append(", MTTR=").append(String.format("%.1f", aggMttr)).append("min")
                .append(", releases this month=").append(releasesThisMonth)
                .append(", rolled back=").append(rolledBack).append("/").append(totalReleases)
                .append("\n");

        sb.append("Environment health:\n");
        if (envHealthLines.isEmpty()) {
            sb.append("- no active releases in any environment\n");
        } else {
            envHealthLines.forEach(line -> sb.append(line).append("\n"));
        }

        sb.append("Recent releases:\n");
        recentReleaseLines.stream().limit(6).forEach(line -> sb.append(line).append("\n"));
    }
}