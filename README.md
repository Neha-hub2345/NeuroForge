# NeuroForge Nexus

Enterprise SDLC management platform — Spring Boot + PostgreSQL backend, Keycloak for identity and access management, React frontend.

## Architecture

- **Backend**: Spring Boot, port `9000`, validates JWTs issued by Keycloak (OAuth2 Resource Server)
- **Auth**: Keycloak — owns login, registration, and role assignment. The backend never sees a password.
- **Database**: PostgreSQL — stores projects, teams, sprints, milestones, and a local `users` shadow profile synced from each user's Keycloak token on first request
- **Frontend**: React + Vite, port `5173`

---

## 1. Prerequisites

- Java 25
- Maven
- Node.js 18+
- PostgreSQL 14+ (or Docker)
- Docker (for running Keycloak — recommended over a manual install)

---

## 2. PostgreSQL setup

### Option A — Docker (recommended)

```bash
docker run -d --name neuroforge-postgres \
  -e POSTGRES_DB=neuroforge_nexus \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=yourpassword \
  -p 5432:5432 \
  postgres:16
```

### Option B — Local install

1. Install PostgreSQL for your OS.
2. Create the database:
   ```sql
   CREATE DATABASE neuroforge_nexus;
   ```
3. Confirm a `postgres` role exists with a password set (or create a dedicated app user and grant it access to `neuroforge_nexus`).

You'll need the database name, username, and password for `application.properties` below — they must match exactly.

---

## 3. Keycloak setup

### Run Keycloak

Docker Method-> download docker desktop then in bash run this command->
```bash
docker run -d --name neuroforge-keycloak -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:25.0 start-dev
```

this will start a local server at port 8080

Log in at `http://localhost:8080/admin` with  username = admin And password= admin.

### 3.1 Create the realm

1. Click the realm dropdown (top-left, says "master") → **Create realm**
2. Realm name: `neuroforge-nexus`
3. Click **Create**, and make sure you're now working inside it (check the dropdown again)

### 3.2 Create the frontend client

1. **Clients** → **Create client**
2. Client ID: `neuroforge-backend`
   *(this is the client the React app logs in through — the name is a bit confusing but keep it, since the app's `keycloak.js` already points at this exact ID)*
3. Click **Next**, leave **Standard flow** enabled, click **Save**
4. On the client's settings page, find **Capability config** and set:
   - **Client authentication: OFF**
    
5. Scroll to **Access settings** and set:
   - Valid redirect URIs: `http://localhost:5173/*`
   - Valid post logout redirect URIs: `http://localhost:5173/*`
   - Web origins: `http://localhost:5173`
6. Click **Save**

### 3.3 Enable self-service registration

1. **Realm settings** → **Login** tab
2. Toggle **User registration** to ON
3. Click **Save**

This adds a "Register" link to the hosted Keycloak login page. Users can now create their own accounts without any custom form.

### 3.4 Create the Custom Role and User Profile
By default, Keycloak doesn't ask for a user's role. We must enable the User Profile
feature to capture it during registration.

1. Go to **Realm settings** then **User Profile** tab
2. If disabled, click the toggle to enable User Profile Enabled.
3. Click Add attribute.
4. Set Name to: **user_role**
5. Set Display name to: **Select Your Role**
6. Under Permissions, check both User and Admin so the user can see and edit it.

7. Under Required settings, toggle Required to ON.
8. (Optional but recommended) Under Validations, add an option validator to restrict choices to
specific SDLC roles (e.g., DEVELOPER, SCRUM_MASTER).
9. Click Save.

### 3.5 Token Mapping (JWT Configuration)
This final step ensures the custom role captured in Step 4 is actually placed inside the
JWT so your Spring Boot backend can read it

1. Go to Clients > select neuroforge-backend.
2. Click the Client scopes tab.
3.  Click on the dedicated scope link named: 
   neuroforge-backend-dedicated
4. Click Add mapper > By configuration.
5. Select User Attribute from the list.

6. Fill in the exact following details: 
    **Name: role-mapper**
    **User Attribute: user_role (Must match the name from Step 4 exactly)**
    **Token Claim Name: app_role**
    **Claim JSON Type: String**
7. Ensure Add to ID token and Add to access token are toggled ON.
8. Click Save


### 4 `application.properties file in Backend/src/main/resources/application.properties`

```properties
//put as it is
spring.application.name=NeuroForge
server.port=9000
spring.security.oauth2.resourceserver.jwt.issuer-uri=${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/neuroforge-nexus}

//change according to local db configuration
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/neuroforge_nexus}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}

//put as it is
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect



```
  ### 4.3 the backend run

1.   ./mvnw spring-boot:run
or
1.go to Backend/src/main/java/com/nexus/NeuroForge/NeuroForgeApplication.java
then right click run application
The backend will be live at `http://localhost:9000`.

---

## 5. Frontend setup

```bash
cd frontend
npm install
npm run dev
```

Runs at `http://localhost:5173` — matching the redirect URIs configured in Section 3.2.

On load, the app redirects straight to public landing page which has login/register option clicking on will take u to Keycloak's login page. From there, "Register" a user,and remember credentials for logging in next time.

---

## 6. How registration and user provisioning actually work here

This project uses **two separate mechanisms working together** — don't confuse them:

**Registration (Section 3.3)** — Keycloak's own hosted form is the *only* way anyone creates an account. There is no custom registration screen and no `/api/auth/register` endpoint in this project's actual flow. Keycloak owns usernames, passwords, and email — the backend never touches any of it.

**Just-in-time (JIT) provisioning (`UserSyncFilter`)** — a separate, automatic step on the backend. The first time a logged-in user hits *any* authenticated endpoint, the filter reads their JWT and, if no local `users` row exists yet for their `keycloakId`, creates one — mirroring their username, email, and current realm role into the local database. This is what lets the rest of the app (projects, teams, sprints, milestones) reference "this user" with a normal foreign key, without the backend ever needing its own login system.

**What this means in practice:**
1. Someone registers via Keycloak's form (Section 3.3) → they exist in Keycloak, but have no realm role yet and no local DB row yet.
2. They log in → the frontend gets a JWT → the first API call they make triggers `UserSyncFilter`, which creates their local profile row.
3. They still can't do anything role-gated yet, because they have no realm role (Section 3.5) — this is intentional, not a bug.
4. An Admin assigns them a realm role via Role Mapping (Section 3.5) → on their next login/token refresh, the new role appears in their JWT, `@PreAuthorize` checks start passing, and their local row's role field gets updated the next time they hit an authenticated endpoint (since `UserSyncFilter` re-checks role on every sync, not just on first creation).

There's no separate "sync" button or endpoint to click — this all happens transparently on normal API traffic.

---

## 7. Suggested startup order

Keycloak and PostgreSQL both need to be reachable *before* the backend starts, or you'll get confusing connection errors on boot rather than a clean failure:

1. PostgreSQL
2. Keycloak — wait until `http://localhost:8080/realms/neuroforge-nexus` returns `200`
3. Backend — wait until port `9000` is listening
4. Frontend

---
