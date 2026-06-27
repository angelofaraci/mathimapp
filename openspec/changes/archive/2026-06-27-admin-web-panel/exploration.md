# Exploration: Admin Web Panel for MathApp

## Current State

The MathApp backend is a Ktor JVM server (`server/`) running on Netty at port 8080. It uses JWT bearer-token authentication with three roles encoded in the token claim `role`: `ADMIN`, `TEACHER`, `STUDENT`. AuthN/AuthZ helpers already exist: `requireSelfOrAdmin()`, `requireAdmin()`, `currentUserId()`, `currentRole()`. Passwords are hashed with bcrypt. Cross-module DTOs live in `shared/` and are `@Serializable` with kotlinx.serialization.

### Existing API Surface Relevant to Admin

| Endpoint | Auth | Access | Relevance |
|----------|------|--------|-----------|
| `POST /auth/register` | Open | Open | Creates `STUDENT` users only |
| `POST /auth/login` | Open | Open | Returns JWT token |
| `GET /users/{id}` | JWT | Self or Admin | View single user |
| `PUT /users/{id}` | JWT | Self or Admin | Update profile; **only ADMIN can change `role`** |
| `GET /progress/{userId}` | JWT | Self or Admin | View user progress |
| `GET /courses/official` | JWT | Any authenticated | Lists official courses by `schoolYear` |
| `GET /courses/{id}` | JWT | Enrolled / Creator / Admin | Single course |
| `GET /courses/creator/{creatorId}` | JWT | Self or Admin | Courses by creator |
| `GET /courses/enrolled/{userId}` | JWT | Self or Admin | Enrolled courses |
| `POST /courses` | JWT | Self or Admin | Create course |
| `PUT /courses/{id}` | JWT | Creator or Admin | Update course |
| `DELETE /courses/{id}` | JWT | Creator or Admin | Delete course |
| `POST /lessons` | JWT | Creator or Admin | Create lesson |
| `PUT /lessons/{id}` | JWT | Creator or Admin | Update lesson |
| `PUT /lessons/{id}/theory` | JWT | Admin or Teacher | Update theory content |
| `DELETE /lessons/{id}` | JWT | Creator or Admin | Delete lesson |
| `POST /exercises` | JWT | Creator or Admin | Create exercise |
| `PUT /exercises/{id}` | JWT | Creator or Admin | Update exercise |
| `DELETE /exercises/{id}` | JWT | Creator or Admin | Delete exercise |

### What Is Missing for a Full Admin Panel

- **No user listing endpoint**: There is no `GET /users` to list/paginate/filter all users.
- **No analytics/aggregation endpoints**: No aggregate stats (total users, completions per course, average scores, active students).
- **No bulk operations**: No way to batch-update roles or bulk-delete content.
- **No admin-specific course view**: `GET /courses/official` is filtered by `schoolYear`; there is no unfiltered "all courses" list for admins.
- **No audit log or activity feed**: Not a blocker, but useful for admin panels.

### Auth/Authorization Gaps

- The `role` claim in the JWT is a simple string. There is no scope/permission granularity beyond the three roles.
- `requireAdmin()` exists but is **unused** in current routes (only `requireSelfOrAdmin()` is used). This means there is no endpoint that is strictly admin-only today besides the implicit role check on `PUT /users/{id}` for role changes.
- Token validity is 24 hours. A web admin session may need refresh-token logic or shorter-lived tokens with sliding sessions.

---

## Affected Areas

- `server/src/main/kotlin/com/example/proyectofinal/routes/` — new admin routes needed (user list, analytics, bulk ops)
- `server/src/main/kotlin/com/example/proyectofinal/service/` — new service methods for aggregation and listing
- `server/src/main/kotlin/com/example/proyectofinal/plugins/Security.kt` — JWT/session considerations for web usage
- `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` — new DTOs for admin requests/responses if consumed by both app and backend
- `server/build.gradle.kts` — new dependencies depending on stack choice (HTML DSL, static serving, CORS changes)

---

## Approaches

### 1. Ktor HTML DSL (Server-Side Rendering)

Build the admin UI directly inside the Ktor server using `ktor-server-html-builder` (or `kotlinx-html`) and serve HTML pages. No separate frontend build. The admin panel is a set of routes (`/admin/*`) that render HTML forms and tables.

- **Pros**:
  - Single codebase, single deployment artifact.
  - No separate frontend framework to learn or maintain.
  - Reuses existing Kotlin expertise and JWT auth trivially (same cookie or bearer token).
  - Fast to scaffold; no build pipeline beyond Gradle.
  - Easy to protect routes with existing `requireAdmin()`.
- **Cons**:
  - Limited interactivity without HTMX/Alpine.js or heavy JS; modern admin UX (sortable tables, real-time charts) is harder.
  - Tight coupling between UI and backend release cycles.
  - Harder to attract frontend contributors or use rich component libraries.
  - HTML DSL can become verbose for complex UIs.
- **Effort**: Low
- **Code Sharing**: None with `composeApp`.

### 2. SPA (React / Vue / Svelte) + Ktor REST API

Build a standalone web frontend in a separate folder (e.g., `admin-web/`) using a modern JS/TS SPA framework. It consumes the existing Ktor REST API (plus new admin endpoints) via HTTP. The Ktor server serves the built static files or they are deployed separately.

- **Pros**:
  - Rich ecosystem: component libraries (shadcn, Material UI), charting (Recharts, Chart.js), table libraries.
  - Best UX for data-dense admin interfaces (sorting, filtering, virtual scrolling).
  - Decoupled frontend/backend release cycles.
  - Easiest to hire for or find open-source admin templates.
- **Cons**:
  - Two build systems, two dependency trees, potential version drift.
  - Requires CORS configuration for local development (already exists, may need expansion).
  - Auth flow needs design: bearer tokens in `localStorage`/`sessionStorage` or cookie-based sessions.
  - Team needs JS/TS knowledge.
- **Effort**: Medium
- **Code Sharing**: Can reuse `shared` DTOs if the SPA is built with Kotlin/JS (see Option 3), but a pure JS/TS SPA would duplicate type shapes or generate from OpenAPI.

### 3. Compose for Web (Kotlin/JS or Kotlin/Wasm)

Use JetBrains Compose for Web (Kotlin/JS or the newer Kotlin/Wasm target) to build the admin UI in Kotlin. Could be a separate module (`admin-web/`) that shares code with `composeApp`.

- **Pros**:
  - Maximum code sharing with `composeApp` (composables, ViewModels, business logic, `shared` models).
  - Single language across the whole stack.
  - Reuses existing Kotlin coroutines and Ktor client code from `composeApp`.
- **Cons**:
  - Compose for Web is still evolving; Kotlin/Wasm browser support is improving but not as mature as JS.
  - Smaller ecosystem than React/Vue — fewer ready-made admin components.
  - Build tooling and bundle sizes can be larger and less optimized than JS SPAs.
  - Debugging in the browser is less mature.
- **Effort**: Medium–High (due to toolchain maturity and component scarcity)
- **Code Sharing**: High — can share ViewModels, repositories, and composables with `composeApp`.

### 4. Full-Stack Kotlin Framework (e.g., KVision, Vaadin)

Use a full-stack Kotlin framework that abstracts frontend/backend communication.

- **Pros**:
  - Single Kotlin codebase with automatic RPC-like communication.
  - Vaadin has mature, enterprise-grade components (grids, forms, charts).
- **Cons**:
  - Heavy lock-in to the framework's component model and lifecycle.
  - Vaadin is JVM-based and would run inside the Ktor server or alongside it, increasing deployment complexity.
  - KVision is smaller and less battle-tested than mainstream options.
  - Adds significant dependencies and learning curve.
- **Effort**: High
- **Code Sharing**: Moderate — shares backend models, but UI layer is framework-specific.

---

## Comparison Matrix

| Criterion | Ktor HTML DSL | SPA (React/Vue) | Compose for Web | Full-Stack (Vaadin/KVision) |
|-----------|---------------|-----------------|-----------------|-----------------------------|
| Setup Complexity | Low | Medium | Medium | High |
| Interactivity / UX | Low–Medium | High | Medium | High |
| Kotlin Code Sharing | None | Low (unless K/JS) | High | Moderate |
| Team Skill Fit | Best if team is Kotlin-only | Best if team has JS/TS skills | Best if team wants Kotlin everywhere | Moderate |
| Maintenance Burden | Low (1 artifact) | Medium (2 artifacts) | Medium–High (tooling churn) | High (framework lock-in) |
| Admin Component Ecosystem | Poor | Excellent | Poor–Medium | Excellent (Vaadin) |
| Integration with Existing JWT | Trivial | Needs design (bearer vs cookie) | Trivial (reuse client code) | Trivial |
| Deployment | Same server | Static files or separate host | Static files or separate host | JVM server |

---

## Recommendation

**No final stack choice is made here — the decision remains PENDING for the user.**

However, the exploration suggests two viable paths depending on team context:

1. **If the team is Kotlin-only and needs something working quickly**: Ktor HTML DSL is the fastest path. It requires zero new languages or build tools and fits naturally into the existing server module. The UX ceiling is lower, but for basic CRUD and user management it is sufficient.

2. **If the team wants a modern, data-dense admin UX and is comfortable with JS/TS**: A React/Vue SPA is the safest long-term bet. It provides the richest component ecosystem and decouples the admin frontend from backend releases. The tradeoff is maintaining a second build pipeline.

3. **If the team wants to bet on Kotlin across the stack and tolerates some toolchain risk**: Compose for Web offers the highest code-sharing potential with `composeApp`, but the ecosystem for admin-specific components is still immature.

**Decision required from the user**: Which stack to adopt (or a hybrid, e.g., HTML DSL for MVP, SPA for v2).

---

## Risks

- **Auth model is too coarse**: The current `ADMIN/TEACHER/STUDENT` tri-state may not scale if the admin panel needs fine-grained permissions (e.g., content moderator vs super-admin). Any admin panel built now should assume at least one hard `ADMIN` gate.
- **Missing endpoints are non-trivial**: Adding user listing, analytics, and bulk operations requires new service-layer queries with Exposed. Aggregation queries on PostgreSQL need indexing review to avoid performance issues at scale.
- **CORS and cookie security**: If the admin panel is served from a different origin than the API, CORS must be explicitly configured. If cookie-based sessions are introduced for the web panel, CSRF protection must be considered.
- **No existing web assets pipeline**: The server currently has no static file serving or frontend build integration. Any option beyond Ktor HTML DSL will require adding static resource handling or a separate hosting strategy.
- **JWT 24h expiry may frustrate web admins**: Browser-based admin sessions typically expect longer-lived sessions or refresh mechanisms.

---

## Ready for Proposal

**Yes.**

The orchestrator should tell the user:
- The exploration identified four viable stack options with tradeoffs.
- A **decision on the web stack is required** before proceeding to `sdd-propose`.
- Once the stack is chosen, the next step is `sdd-propose` to define scope, followed by `sdd-spec` to enumerate the missing endpoints and admin features.
