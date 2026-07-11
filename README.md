# NeuroForge Nexus — Local Setup Guide

## Prerequisites

- **Docker Desktop** installed and running
  - https://www.docker.com/products/docker-desktop
  - Verify: `docker --version` and `docker compose version`
- Git

## 1. Clone the repository

```bash
git clone <repo-url>
cd <repo-folder>
```

## 2. Set up `.env`

Create a `.env` file in the project root:

```dotenv
DB_PASSWORD=<choose a password for the local Postgres database>
```

This is used to initialize the local Postgres container on first run. Any value works — just don't reuse a real/sensitive password, since this is local dev only.

> ⚠️ `.env` is gitignored — never commit it.

## 3. Running the stack

**Start everything (first run / after code changes):**
```bash
docker compose up --build
```

**Start in the background:**
```bash
docker compose up --build -d
```

**Rebuild just the backend after code changes:**
```bash
docker compose up --build backend
```

**View logs for a service:**
```bash
docker compose logs backend -f
```

**Stop everything (keeps data):**
```bash
docker compose down
```

**Stop and wipe all data (fresh start — Postgres, Keycloak, Kafka all reset):**
```bash
docker compose down -v
```
Use this if you hit a Postgres auth error after changing `DB_PASSWORD`, or want a totally clean slate.

## 4. Accessing services

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:9000 |
| Keycloak Admin Console | http://localhost:8080 |


