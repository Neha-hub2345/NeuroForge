# NeuroForge Nexus 🚀

NeuroForge Nexus is an enterprise Agile/SDLC management platform featuring Kanban boards, sprint & milestone tracking, blocker management, CI/CD pipeline visualization, release/observability monitoring, and real-time event-driven notifications powered by Kafka.

This repository contains the full-stack Dockerized application: **Spring Boot (Java) + React (Vite) + Keycloak + Kafka + PostgreSQL**, plus an observability stack (**Prometheus + Grafana + ELK**).

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Backend Structure](#backend-structure)
- [Frontend Structure](#frontend-structure)
- [Services & Ports](#services--ports)
- [Environment Variables](#environment-variables)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Keycloak Configuration](#keycloak-configuration)
- [Contributing / Git Workflow](#contributing--git-workflow)

---

## Architecture Overview

NeuroForge Nexus is built as a **modular monolith** backend (single Spring Boot deployable, organized into feature-based packages) paired with a **React SPA** frontend, all orchestrated with Docker Compose. Identity and access is centralized through **Keycloak** (RBAC across 5 realm roles), inter-service events flow through **Kafka**, and system health is surfaced through **Prometheus/Grafana** (metrics) and the **ELK stack** (logs).

```
┌─────────────┐      ┌───────────────┐      ┌─────────────────┐
│   React SPA  │─────▶│  Spring Boot   │─────▶│   PostgreSQL     │
│  (Vite:5173) │◀─────│  API (:9000)   │◀─────│    (:5433)       │
└─────────────┘      └───────┬───────┘      └─────────────────┘
                             │
              ┌──────────────┼───────────────┬──────────────┐
              ▼              ▼               ▼              ▼
        ┌───────────┐  ┌───────────┐  ┌────────────┐  ┌───────────┐
        │ Keycloak   │  │ Prometheus │  │  Logstash  │  │  GitHub    │
        │  (:8080)   │  │  (:9090)   │  │ /Elastic/  │  │  Actions/  │
        │  RBAC/JWT  │  │  Grafana   │  │  Kibana    │  │  Groq API  │
        └───────────┘  └───────────┘  └────────────┘  └───────────┘
```

---

## Backend Structure

**Stack:** Spring Boot 3.2, Java, Spring Security (OAuth2 Resource Server), Spring Data JPA, Spring Kafka, PostgreSQL, Keycloak Admin Client, Micrometer/Prometheus, Logstash Logback Encoder.

The backend lives under `Backend/src/main/java/com/nexus/NeuroForge/` and is organized by **layer-then-feature**, so each layer (`config`, `controllers`, `services`, `repositories`, `models`, `dto`) contains a subfolder per feature domain. This keeps the codebase a single deployable unit while still separating concerns cleanly by module.

```
Backend/src/main/java/com/nexus/NeuroForge/
├── NeuroForgeApplication.java        # Spring Boot entrypoint
│
├── config/                           # Cross-cutting configuration
│   ├── SecurityConfig.java           # Filter chain, split JwtDecoder beans (container vs browser issuer), @PreAuthorize setup
│   ├── UserSyncFilter.java           # JIT user provisioning from Keycloak JWT on first request; handles race conditions
│   ├── CustomRoleConverter.java      # Extracts nested realm_access.roles claim into Spring authorities
│   ├── CorsConfig.java               # CORS rules (parameterized via @Value)
│   ├── KafkaConfig.java              # Kafka producer/consumer beans, JacksonJsonSerializer
│   ├── SchedulingConfig.java         # Enables @Scheduled jobs (KPI snapshots, health checks)
│   ├── RequestCorrelationFilter.java # Adds correlation/trace IDs to each request for log tracing
│   ├── RestTemplateConfig.java       # HTTP client bean(s) for outbound calls (GitHub, Groq, Prometheus)
│   ├── ProjectGaugeRegistrar.java    # Registers custom Micrometer gauges for observability
│   └── GlobalExceptionHandler.java   # Centralized @ControllerAdvice error handling
│
├── controllers/                      # REST API layer, one subpackage per domain
│   ├── project/                      # ProjectController, ProjectIntegrationController (GitHub repo linking)
│   ├── team/                         # TeamController
│   ├── user/                         # UserController
│   ├── sprint/                       # SprintController, SprintProgressController
│   ├── milestone/                    # MilestoneController
│   ├── Blocker/                      # BlockerController
│   ├── task/                         # TaskController
│   ├── pipeline/                     # PipelineController (receives CI/CD webhooks)
│   ├── releases/                     # ReleaseController
│   ├── alert/                        # AlertController (alert rules/thresholds)
│   ├── kpi/                          # KpiHistoryController
│   ├── analytics/                    # AnalyticsController
│   ├── observability/                # ObservabilityController (Prometheus/log queries)
│   ├── notification/                 # NotificationController
│   ├── ai/                           # AiAssistantController (Groq-powered chat assistant)
│   └── deploy/                       # RenderWebhookController (deployment webhooks)
│
├── services/                         # Business logic, mirrors controller domains
│   ├── project/, team/, user/, sprint/, milestone/, task/, releases/, notification/
│   ├── pipeline/                     # PipelineService, GithubActionsClient (dispatches workflow runs)
│   ├── kafka/                        # KafkaProducerService, KafkaConsumerService
│   ├── kpi/                          # KpiSnapshotScheduler, WorkspaceSnapshotService
│   ├── monitoring/                   # AlertMonitoringService, ExternalHealthMonitorService
│   ├── observability/                # LogSearchService, PrometheusQueryService
│   ├── security/                     # TokenEncryptionService, WebhookSignatureValidator
│   ├── aiservice/                    # AiAssistantService (Groq API integration)
│   └── deploy/                       # RenderWebhookService
│
├── repositories/                     # Spring Data JPA repositories, one per model, mirrors models/ domains
│
├── models/                           # JPA entities, one subpackage per domain
│   ├── project/, team/, user/, sprint/, task/, milestone/, blocker/, releases/, pipeline/
│   ├── alert/, deploy/, notification/, kpi/, monitoring/
│   └── interfaces/                   # Enums: Role, ProjectStatus, PipelineStatus, ReleaseStatus, AlertSeverity, etc.
│
├── dto/                              # Request/response DTOs, mirrors controller domains
│
└── events/
    └── TaskEvent.java                # Kafka event payload for task lifecycle changes
```

**Key architectural notes:**
- **RBAC**: 5 realm roles enforced via Keycloak — `ADMIN`, `PROJECT_MANAGER`, `DEVELOPER`, `TESTER`, `DEVOPS_ENGINEER` (see `models/interfaces/Role.java`), hardened with `@PreAuthorize` across controllers.
- **Dual JWT issuer resolution**: `SecurityConfig` defines separate `JwtDecoder` beans because the backend validates tokens against Keycloak's *container-internal* address (`http://keycloak:8080/...`) while the browser-issued token's `iss` claim points to `http://localhost:8080/...`. Both are reconciled so token validation works from inside and outside the Docker network.
- **JIT user provisioning**: `UserSyncFilter` creates a local `User` row on a user's first authenticated request, catching `DataIntegrityViolationException` to safely handle concurrent first-request race conditions.
- **Kafka**: event-driven task/notification pipeline via `TaskEventProducer`/`KafkaConsumerService`, writing to `EventLog`/`Notification` tables. (Currently commented out, not deleted, in `application.properties` for the lightweight demo build — a Kafka-free `NotificationService` path is used instead.)
- **CI/CD integration**: `PipelineController`/`PipelineService`/`GithubActionsClient` receive GitHub Actions webhook POSTs and can dispatch workflow runs via the GitHub API.
- **AI Assistant**: `AiAssistantController`/`AiAssistantService` proxy chat requests to the Groq API (OpenAI-compatible chat completions endpoint).
- **Observability**: Actuator + Micrometer expose Prometheus-scrapeable metrics; `ObservabilityController` also queries Prometheus/logs server-side for the dashboard.

---

## Frontend Structure

**Stack:** React 18 (Vite 7), React Router 6, Axios, Keycloak-JS, Framer Motion, Recharts, Lucide icons.

```
frontend/src/
├── main.jsx                  # App bootstrap
├── App.jsx                   # Route definitions
│
├── api/                      # Axios request functions per domain (client.js holds the base Axios instance/interceptors)
│   ├── client.js, auth.js, projects.js, teams.js, users.js
│   ├── sprints.js, milestones.js, pipeline.js, releases.js
│   ├── analytics.js, monitoring.js, ProjectIntegration.js
│
├── services/                 # Higher-level service wrappers used by hooks/pages
│   ├── taskService.js, blockerService.js, alertService.js
│   ├── pipelineService.js, ReleaseService.js, kpiHistoryService.js
│   ├── notificationService.js, observabilityService.js, analyticsService.js, aiService.js
│
├── hooks/                    # Data-fetching/state hooks per feature
│   ├── useBoardTasks.js, useDashboardData.js, useProject.js
│   ├── useProjectSprints.js, usePipelineDashboard.js, useReleaseDashboard.js
│
├── context/
│   ├── AuthContext.jsx        # Keycloak session state, roles, user context
│   └── ThemeContext.jsx       # Light/dark theme
│
├── lib/
│   └── keyclock.js            # Keycloak-JS client initialization
│
├── components/                # Reusable UI, grouped by feature
│   ├── AppLayout.jsx, ProjectLayout.jsx, ProtectedRoute.jsx, ThemeToggle.jsx
│   ├── SprintSelector.jsx, TaskDetailModal.jsx, ui.jsx (shared primitives)
│   ├── board/                 # KanbanBoard.jsx, BoardHeader.jsx, CreateTaskModal.jsx, FlagBlockerModal.jsx
│   ├── dashboard/              # WorkspaceStats.jsx, AnalyticsPanel.jsx, AiAssistantPanel.jsx, RecentProjectsPanel.jsx
│   ├── pipeline/               # BuildsTable.jsx, BuildDetailModal.jsx, BuildStagesTimeline.jsx, LiveBuildLogsPanel.jsx, PipelineKpiStats.jsx, DeploymentStatusCard.jsx, TestMetricsCard.jsx
│   ├── releases/               # ReleasesTable.jsx, ReleaseDetailModal.jsx, AlertsPanel.jsx, AlertRulesPanel.jsx, EnvironmentHealthPanel.jsx, KpiTrendChart.jsx, RecentLogsPanel.jsx, PrometheusStatusBadge.jsx
│   ├── teams/                  # TeamCard.jsx, CreateTeamModal.jsx, AddMemberModal.jsx
│   ├── settings/                # ProjectSettingsForm.jsx, GithubIntegrationForm.jsx, ProjectOverviewCard.jsx, DangerZone.jsx
│   ├── sprintsMilestones/ & projectDetail/  # SprintsPanel.jsx, MilestonesPanel.jsx
│   └── landing/                 # HeroSection.jsx, FeaturesSection.jsx, StackSection.jsx, RoadmapSection.jsx, TestimonialsSection.jsx, FaqSection.jsx, CtaFooter.jsx, LandingNav.jsx
│
├── pages/                     # Top-level routed pages
│   ├── Landing.jsx, Dashboard.jsx, Projects.jsx, Teams.jsx, Users.jsx, Notifications.jsx
│   ├── ProjectDetail.jsx       # Project shell/nav
│   └── project/                 # Board.jsx, Backlog.jsx, Blockers.jsx, SprintsMilestones.jsx, PipelineDashboard.jsx, ReleasesMonitoring.jsx, Reports.jsx, Settings.jsx
│
├── data/
│   └── landingContent.js       # Static marketing copy for the landing page
│
├── utils/
│   └── roles.js                # Frontend-side role/permission helpers matching backend Role enum
│
└── styles/
    └── index.css               # Global styles
```

The frontend is a **role-aware SPA**: `ProtectedRoute.jsx` + `AuthContext.jsx` gate routes based on the Keycloak realm role in the JWT, and `utils/roles.js` mirrors the backend's 5-role RBAC model.

---

## Services & Ports

| Service | Container Name | Host Port | Purpose |
| :--- | :--- | :--- | :--- |
| **Frontend (React/Nginx)** | `neuroforge-frontend` | `5173` → 80 | Web UI |
| **Backend (Spring Boot)** | `neuroforge-backend` | `9000` | REST API |
| **PostgreSQL** | `neuroforge-postgres` | `5433` → 5432 | Primary database |
| **Keycloak** | `neuroforge-keycloak` | `8080` | Identity/Auth (Admin Console + OIDC) |
| **Prometheus** | `prometheus` | `9090` | Metrics scraping |
| **Grafana** | `grafana` | `3000` | Metrics dashboards |
| **Elasticsearch** | `elasticsearch` | `9200` | Log storage/search |
| **Logstash** | `logstash` | `5000` | Log ingestion pipeline |
| **Kibana** | `kibana` | `5601` | Log visualization UI |

> Note: Postgres is mapped to host port **5433** (not the default 5432) to avoid clashing with a local Postgres install; internally, containers still talk to it on `5432`.

---

## Environment Variables

Create a `.env` file in the project root (this file is git-ignored — **never commit it**):

```dotenv
# --- Database ---
DB_PASSWORD=<your-choice>              # Any value works for local dev — do NOT reuse a real password

# --- App-level encryption (used to encrypt stored tokens/secrets, e.g. GitHub PAT at rest) ---
APP_ENCRYPTION_SECRET=<your-choice>    # Any long random string
APP_ENCRYPTION_SALT=<your-choice>      # Any random hex string

# --- GitHub Actions Integration (optional — only needed for the CI/CD pipeline dashboard) ---
GITHUB_PAT=<your-github-personal-access-token>
GITHUB_OWNER=<your-github-username-or-org>
GITHUB_REPO=<your-repo-name>

# --- Groq AI Assistant (optional — only needed for the in-app AI assistant panel) ---
GROQ_API_KEY=<your-groq-api-key>
GROQ_MODEL=openai/gpt-oss-120b         # or another Groq-hosted model

# --- Keycloak admin client secret (optional, only if you enable the Keycloak admin API integration) ---
KEYCLOAK_ADMIN_CLIENT_SECRET=<client-secret-from-keycloak>
```

### Where to get each value

| Variable | Where to get it |
| :--- | :--- |
| `DB_PASSWORD` | Any value you choose — used only to initialize your local Postgres container. |
| `APP_ENCRYPTION_SECRET` / `APP_ENCRYPTION_SALT` | Any value you choose (e.g. generate with `openssl rand -hex 32`). Used to encrypt sensitive fields (like the stored GitHub PAT) at rest in the database. |
| `GITHUB_PAT` | GitHub → click your profile picture → **Settings** → **Developer settings** (bottom of left sidebar) → **Personal access tokens** → **Tokens (classic)** or **Fine-grained tokens** → **Generate new token**. Grant it `repo` and `workflow` scopes (classic) so the backend can read repo info and dispatch/monitor GitHub Actions workflow runs. |
| `GITHUB_OWNER` | Your GitHub username or organization name that owns the repo you want to track (visible in the repo's URL: `github.com/<OWNER>/<REPO>`). |
| `GITHUB_REPO` | The repository name you want the pipeline dashboard to track (same URL as above). |
| `GROQ_API_KEY` | Sign up / log in at [console.groq.com](https://console.groq.com) → **API Keys** (left sidebar) → **Create API Key**. Free tier is available. |
| `GROQ_MODEL` | Any model name available in your Groq console under **Playground**/**Models** (defaults to `openai/gpt-oss-120b` if unset). |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | Only needed if you wire up the Keycloak Admin REST API integration. In the Keycloak Admin Console: **Clients** → select the relevant admin/service client → **Credentials** tab → copy the **Client Secret**. Not required for the default setup (defaults to `unused`). |

> Additional secrets like `GF_SECURITY_ADMIN_PASSWORD` (Grafana) and `KEYCLOAK_ADMIN_PASSWORD` (Keycloak bootstrap admin) are currently hardcoded to `admin`/`admin` in `docker-compose.yml` for local dev convenience — **change these before deploying anywhere beyond your own machine.**

---

## Prerequisites

- **Docker Desktop** installed and running
  - Download: https://www.docker.com/products/docker-desktop
  - Verify installation: `docker --version` and `docker compose version`
- **Git**

---

## Getting Started

### 1. Clone the repository
```bash
git clone <repo-url>
cd <repo-folder>
```

### 2. Set up `.env`
Create the `.env` file in the project root as described in [Environment Variables](#environment-variables) above.

> ⚠️ `.env` is git-ignored — never commit it to the repository.

### 3. Run the stack

**Start everything (first run / after code changes) — this is the only command you need:**
```bash
docker compose up --build
```

Other commands, for reference:

```bash
# Start in the background
docker compose up --build -d

# Rebuild just the backend after code changes
docker compose up --build backend

# View logs for a service
docker compose logs backend -f

# Stop everything (keeps data)
docker compose down

# Stop and wipe all data (fresh start — Postgres, Keycloak, Kafka all reset)
docker compose down -v
```
*Use `down -v` if you hit a Postgres authentication error after changing `DB_PASSWORD`, or if you want a totally clean slate.*

### 4. Access the services

| Service | URL |
| :--- | :--- |
| Frontend UI | http://localhost:5173 |
| Backend API | http://localhost:9000 |
| Keycloak Admin Console | http://localhost:8080 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Kibana | http://localhost:5601 |

---

## Keycloak Configuration

Keycloak is configured to **auto-import** the `neuroforge-nexus` realm on first boot (via `keycloak/import/neuroforge-nexus-realm.json`, mounted into the container and loaded with `start-dev --import-realm`), including the `neuroforge-backend` client pre-configured for `http://localhost:5173`.

If Keycloak does **not** configure itself automatically, open **http://localhost:8080** and follow the steps in `Keycloak_Configuration_Runbook.pdf` exactly as documented.

---

## Contributing / Git Workflow

Direct pushes to `main` are blocked. To contribute:

**1. Pull the latest code**
```bash
git checkout main
git pull origin main
```

**2. Create a new branch for your work**
```bash
git checkout -b feature/your-branch-name
```

**3. Make your changes and test them locally**
```bash
docker compose up --build
```

**4. Commit and push your branch**
```bash
git add .
git commit -m "Add your commit message here"
git push origin feature/your-branch-name
```

**5. Open a Pull Request (PR)**

Go to the GitHub repository → click **Compare & pull request** → once automated checks pass, your code will be merged into `main`.
