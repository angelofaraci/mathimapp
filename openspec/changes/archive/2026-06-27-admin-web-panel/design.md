# Design: Admin Web Panel

## Technical Approach

React SPA at `admin-web/` consumes 3 new Ktor REST endpoints — `GET /admin/users`, `GET /admin/courses`, `PUT /admin/users/{id}/role` — each guarded by the existing `requireAdmin()` in `server/.../plugins/Security.kt` (line 77). Bearer-token auth via existing `POST /auth/login`; token stored in `sessionStorage`. First slice: login gate, paginated user list with search, role update, all-courses list with creator name and enrollment count. No analytics, bulk ops, or refresh tokens (deferred to future).

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|----------|----------|----------|--------|-----------|
| Admin DTOs location | `server/models/` | `shared/` | `server/models/` | `AdminCourse` and `PageResponse<T>` are server-only — no mobile client consumes them. Keeps `shared` minimal per AGENTS.md. |
| Pagination wrapper | Generic `PageResponse<T>` | Inline fields in each response | `PageResponse<T>` | Clean reusable shape for future admin list endpoints. |
| Role update body | Reuse `UpdateUserRequest` | New `RoleUpdateRequest` | New `RoleUpdateRequest` | A dedicated `RoleUpdateRequest { role: String }` lets `UserRole.parse()` return `null` and drive a clean 400 for invalid roles. Reusing `UpdateUserRequest { role: UserRole? }` would throw a serialization exception on invalid enum values before reaching the parse. |
| SPA routing | React Router v6 | State-based switching | React Router v6 | 3 routes (login, users, courses) justify declarative routing. Minimal. |
| SPA server state | TanStack Query only | Redux/Zustand | TanStack Query only | Admin panel is read-heavy CRUD — server state is the only state. No client-side derived state needed for v1. |
| Token gate (SPA) | Role from `POST /auth/login` response | New `/auth/me` endpoint | Role from login response | `AuthResponse` already includes `user.role` (Models.kt:28). Reject non-ADMIN client-side. |

## Data Flow

```
 Browser (SPA)                Ktor :8080              PostgreSQL
     │                            │                       │
     │ POST /auth/login           │                       │
     ├───────────────────────────►│ validateCredentials()  │
     │ {token, user.role:ADMIN}   │───────────────────────┤
     │◄───────────────────────────┤                       │
     │                            │                       │
     │ GET /admin/users?page=0&size=20&query=john         │
     │ Authorization: Bearer tok  │                       │
     ├───────────────────────────►│ requireAdmin() ✓      │
     │                            │ listUsers(q,page,sz)  │
     │ {items:[User...],total:42} │───────────────────────┤
     │◄───────────────────────────┤                       │
     │                            │                       │
     │ PUT /admin/users/X/role    │                       │
     │ {"role":"TEACHER"}         │                       │
     ├───────────────────────────►│ requireAdmin() ✓      │
     │                            │ updateUser(id,req)    │
     │ {id,name,email,role:TEACH}│───────────────────────┤
     │◄───────────────────────────┤                       │
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `server/.../models/AdminDtos.kt` | Create | `PageResponse<T>`, `AdminUserResponse`, `AdminCourseResponse`, `RoleUpdateRequest` |
| `server/.../routes/adminRoutes.kt` | Create | `GET /admin/users`, `GET /admin/courses`, `PUT /admin/users/{id}/role` — all under `authenticate("auth-jwt")` with `requireAdmin()` |
| `server/.../service/UserService.kt` | Modify | Add `listUsers(query: String?, page: Int, size: Int): PageResponse<AdminUserResponse>` |
| `server/.../service/CourseService.kt` | Modify | Add `getAllCoursesAdmin(): List<AdminCourseResponse>` (resolve creator names + count enrollments) |
| `server/.../plugins/Cors.kt` | Modify | Add `allowHost("localhost:5173")` for Vite dev |
| `server/Main.kt` | Modify | Wire `adminRoutes(userService, courseService)` |
| `admin-web/` (~10 files) | Create | Vite + React 18 + React Router + TanStack Query SPA |
| `server/src/test/.../AdminIntegrationTest.kt` | Create | `testApplication` + H2 tests for 3 endpoints |
| `server/src/test/.../AdminServiceTest.kt` | Create | Service-layer tests following `ServiceLayerTest.kt` pattern |

## Interfaces / Contracts

### `GET /admin/users`
- **Query**: `page` (Int, default 0), `size` (Int, default 20, max 100), `query` (String?, optional)
- **200**: `{ items: AdminUserResponse[], page: Int, size: Int, totalElements: Long, totalPages: Int }`
- **401/403**: standard (Security.kt:61,81)
- **Service**: `dbQuery` with `LIKE "%query%"` on name/email, `ORDER BY name`, `LIMIT` + `OFFSET`, and separate `COUNT(*)`.

### `GET /admin/courses`
- **No query params**
- **200**: `AdminCourseResponse[]` — each element carries `creatorName: String` and `enrollmentCount: Int`
- **401/403**: standard

### `PUT /admin/users/{id}/role`
- **Body**: `{ "role": "STUDENT" | "TEACHER" | "ADMIN" }` — uses a dedicated `RoleUpdateRequest { role: String }`; `UserRole.parse()` returns `null` for invalid values → 400
- **200**: updated `User`
- **400**: invalid role (`UserRole.parse` returns null — Models.kt:39-44)
- **401/403/404**: standard

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Backend integration | Auth gates (200/403/401), pagination, search, role update, course aggregation | `testApplication` + H2 pattern from `ServerIntegrationTest.kt`. Generate tokens via `Security.generateToken(userId, role.name)`. |
| Service layer | listUsers offset/limit, getAllCoursesAdmin join + count | Follow `ServiceLayerTest.kt` pattern: init H2, insert fixture data, assert results. |
| SPA | Login gate, API calls, route protection | **Deferred to future change.** No JS test harness exists in repo. Risk: SPA correctness relies on manual QA for v1. |

## Migration / Rollout

**No database migration required** — `Users`, `Courses`, `EnrolledCourses` already have all needed columns. CORS change is additive. Rollback: remove `admin-web/`, delete `adminRoutes.kt` and its `Main.kt` wiring, revert CORS line.

## Open Questions

- [x] `Users` table lacks `createdAt` column; spec required it in `GET /admin/users` response. **Resolved for v1: drop `createdAt` from `AdminUserResponse`** — no Flyway migration in this slice (spec reconciled to omit the field). A future change may add the column + migration if audit timestamps become required.
- [ ] Production deployment: serve `admin-web/dist` via Ktor static-content plugin, or separate host with reverse proxy?
- [ ] Should `admin-web/` be a pnpm workspace member in the monorepo, or a standalone project?
