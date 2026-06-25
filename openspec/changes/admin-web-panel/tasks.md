# Tasks: Admin Web Panel

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~900–1000 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (backend) → PR 2 (frontend) |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Base | Notes |
|------|------|-----------|------|-------|
| 1 | Backend DTOs + services + routes + tests | PR 1 | feature/admin-web-panel | Fully testable standalone |
| 2 | React SPA (login + users + courses) | PR 2 | PR 1 branch | Consumes PR 1 endpoints |

## Phase 1: Backend DTOs & Service Methods (PR 1)

- [x] 1.1 Create `server/src/main/kotlin/com/example/proyectofinal/models/AdminDtos.kt` — `PageResponse<T>` (items, total, page, size) and `AdminCourse` (id, title, description, creatorId, creatorName, enrollmentCount, isOfficial, schoolYear). Omit `createdAt` from user response per design.
- [x] 1.2 Add `listUsers(query: String?, page: Int, size: Int): PageResponse<User>` to `UserService.kt` — Exposed query with `LIKE "%query%"` on name/email, `LIMIT`/`OFFSET`, separate `COUNT(*)`.
- [x] 1.3 Add `getAllCoursesAdmin(): List<AdminCourse>` to `CourseService.kt` — join `Courses.creatorId` with `Users.id` for `creatorName`, subquery count from `EnrolledCourses`.

## Phase 2: Admin Routes, Wiring & CORS (PR 1)

- [x] 2.1 Create `server/src/main/kotlin/com/example/proyectofinal/routes/adminRoutes.kt` — `GET /admin/users`, `GET /admin/courses`, `PUT /admin/users/{id}/role`, all under `authenticate("auth-jwt")` + `requireAdmin()`. Reuse `UpdateUserRequest` for role body.
- [x] 2.2 Wire `adminRoutes(userService, courseService)` in `Main.kt` after existing route calls.
- [x] 2.3 Add `allowHost("localhost:5173")` to `Cors.kt` for Vite dev server.

## Phase 3: Backend Tests (PR 1)

- [x] 3.1 Create `AdminIntegrationTest.kt` — `testApplication` + H2: test all 3 endpoints with ADMIN token (200), STUDENT token (403), no token (401), nonexistent user (404), invalid role (400), empty search results.
- [ ] 3.2 Create `AdminServiceTest.kt` — service-layer tests for `listUsers` offset/limit/search and `getAllCoursesAdmin` join + count, following existing `ServiceLayerTest.kt` pattern.

## Phase 4: SPA Shell & Auth (PR 2)

- [ ] 4.1 Scaffold `admin-web/` — Vite + React 18 + TypeScript + React Router v6 + TanStack Query. Create `package.json`, `vite.config.ts`, `tsconfig.json`, `index.html`, `src/main.tsx`, `src/App.tsx`.
- [ ] 4.2 Create API client in `src/lib/api.ts` — `fetch()` wrapper attaching `Authorization: Bearer <token>`, reads token from `sessionStorage`, clears on 401.
- [ ] 4.3 Create auth context in `src/lib/auth.tsx` — login via `POST /auth/login`, store token, check `user.role === "ADMIN"`, reject non-ADMIN with access-denied message.
- [ ] 4.4 Build login page at `src/pages/Login.tsx` — email/password form, calls auth context login, navigates to `/users` on success.

## Phase 5: SPA Pages (PR 2)

- [ ] 5.1 Build user list page at `src/pages/Users.tsx` — paginated table from `GET /admin/users?page=&size=&query=`, search input with debounce.
- [ ] 5.2 Build role update UI — inline dropdown per user row, calls `PUT /admin/users/{id}/role`, optimistically updates list.
- [ ] 5.3 Build course overview page at `src/pages/Courses.tsx` — table from `GET /admin/courses` displaying title, creatorName, enrollmentCount, isOfficial, schoolYear.
- [ ] 5.4 Wire routes in `App.tsx` — `/login` (public), `/users` (protected), `/courses` (protected), redirect unknown paths to `/users`. Protect with `AuthGuard` component requiring valid ADMIN token.
