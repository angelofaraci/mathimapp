# Verification Report: Admin Web Panel — PR 1 (Backend Slice)

## Change
`admin-web-panel` — PR 1 slice (Phases 1–3: backend DTOs, services, routes, CORS, integration tests).
PR 2 (Phases 4–5: React SPA) is explicitly out of scope and verified absent.

- **Slot**: PR 1 / stacked-to-main / slice: `feature/admin-web-panel`
- **Verifying commits**: `6f88cde`, `91b1b73`, `6a3e814`
- **Persistence mode**: openspec
- **Strict TDD**: `false` — runtime evidence required and collected.
- **Date**: 2026-06-25

---

## Overall Verdict
**PASS WITH WARNINGS**

- `:server:test` → **42 tests, 0 failures, 0 errors, 0 skipped** (BUILD SUCCESSFUL).
- 12 of 12 new `AdminIntegrationTest` cases pass; 30 pre-existing tests unaffected.
- All 11 in-scope backend spec scenarios have passing runtime evidence at the API level.
- 2 SPA-gate scenarios (admin SPA login) are NOT-COVERED — they belong to PR 2 and the SPA scaffold does not exist, as expected.
- 3 design deviations assessed: 1 requires spec reconciliation before archive (createdAt), 2 are acceptable design-coherence notes.
- Task 3.2 (`AdminServiceTest.kt`) remains unchecked → WARNING (blocks archive readiness, not PR 1 mergeability).

---

## Build / Test / Coverage Evidence

| Command | Result |
|---|---|
| `./gradlew :server:test --console=plain` | BUILD SUCCESSFUL; task `:server:test` executed; 8 actionable tasks. |
| `TEST-*.xml` (test-results) | 6 suites, 42 tests total, 0 failures, 0 errors, 0 skipped. |

Per-suite counts (from `server/build/test-results/test/TEST-*.xml`):

| Suite | tests | failures | errors | skipped |
|---|---|---|---|---|
| `AdminIntegrationTest` | 12 | 0 | 0 | 0 |
| `AuthServiceTest` | 2 | 0 | 0 | 0 |
| `CourseServiceTest` | 2 | 0 | 0 | 0 |
| `LessonExerciseServiceTest` | 7 | 0 | 0 | 0 |
| `ServerIntegrationTest` | 17 | 0 | 0 | 0 |
| `UserServiceTest` | 2 | 0 | 0 | 0 |
| **Total** | **42** | **0** | **0** | **0** |

The 12 new `AdminIntegrationTest` cases:
1. `admin can list users with pagination`
2. `admin can search users by name or email`
3. `non-admin cannot access user list`
4. `user list rejects missing token`
5. `admin can list all courses with creator name and enrollment count`
6. `empty course list returns 200 with empty array`
7. `non-admin cannot access course list`
8. `admin can update user role`
9. `role update returns 404 for nonexistent user`
10. `role update returns 400 for invalid role`
11. `non-admin cannot update roles`
12. `role update rejects missing token`

Coverage command: none configured for `:server`. Static analysis only as build-time Kotlin compile. Source inspection plus runtime integration tests form the compliance evidence.

---

## Completeness Table (Phase 1–3 tasks)

| Task | Status | Evidence |
|---|---|---|
| 1.1 AdminDtos.kt — `PageResponse<T>`, `AdminUserResponse`, `AdminCourseResponse`, `RoleUpdateRequest` | DONE | `models/AdminDtos.kt:5–37` |
| 1.2 `UserService.listUsers` — search + pagination + COUNT(*) | DONE | `service/UserService.kt:43–78` |
| 1.3 `CourseService.getAllCoursesAdmin` — creatorName + enrollmentCount | DONE | `service/CourseService.kt:41–64` |
| 2.1 `adminRoutes.kt` — 3 routes under `authenticate("auth-jwt")` + `requireAdmin()` | DONE | `routes/adminRoutes.kt:16–52` |
| 2.2 `Main.kt` wire `adminRoutes(userService, courseService)` | DONE | `Main.kt:61` |
| 2.3 `Cors.kt` add `allowHost("localhost:5173")` | DONE | `plugins/Cors.kt:12` |
| 3.1 `AdminIntegrationTest.kt` — 12 integration tests | DONE | `test/AdminIntegrationTest.kt` (12 cases, all pass) |
| 3.2 `AdminServiceTest.kt` — service-layer tests | **NOT DONE** | Deferred; task unchecked in `tasks.md:41`. See WARNING W-04. |

---

## Spec Compliance Matrix

### `admin-user-management` (8 scenarios; 6 backend in PR 1, 2 SPA-Gate belong to PR 2)

| # | Scenario | Status | Evidence |
|---|---|---|---|
| U1 | Admin retrieves first page (`page=0&size=20`, returns up to 20 + metadata) | PASS | `AdminIntegrationTest` #1 — asserts `totalElements=4, totalPages=1, page=0, size=10, items.size=4`. Source: `UserService.listUsers:43–78` returns `PageResponse`. |
| U2 | Non-admin receives 403 on `GET /admin/users` | PASS | `AdminIntegrationTest` #3 — STUDENT token → 403. `adminRoutes.kt:20` `requireAdmin()` returns false → 403. |
| U3 | Empty search returns no results (count 0) | PASS | `AdminIntegrationTest` #2 — `query=nonexistent` → `totalElements=0, items.isEmpty()`. |
| U4 | Admin promotes user to TEACHER | PASS | `AdminIntegrationTest` #8 — 200, `updated.role == TEACHER`, persisted `Users.role == "TEACHER"`. Source `adminRoutes.kt:37–49`. |
| U5 | Non-existent user returns 404 | PASS | `AdminIntegrationTest` #9 — `nonexistent-id/role` → 404. `updateUser` returns null → `adminRoutes.kt:47` responds 404. |
| U6 | Invalid role value returns 400 | PASS | `AdminIntegrationTest` #10 — `role=SUPERHERO` → 400. `UserRole.parse` null → `adminRoutes.kt:44` responds 400 "Invalid role". |
| U7 | Admin SPA login navigates to dashboard | NOT-COVERED | PR 2 scope (SPA). No `admin-web/` directory exists (confirmed). |
| U8 | Non-admin SPA login blocked (clears token, access-denied msg) | NOT-COVERED | PR 2 scope (SPA). |

### `admin-course-overview` (3 scenarios)

| # | Scenario | Status | Evidence |
|---|---|---|---|
| C1 | Admin views all courses with creator name + enrollment count, unfiltered by schoolYear | PASS | `AdminIntegrationTest` #5 — math (official, year 3, 2 enrollments, creatorName="Teacher John") + art (non-official, year 0, 0 enrollments) both returned. `CourseService.getAllCoursesAdmin:41–64` returns all rows regardless of schoolYear/isOfficial. |
| C2 | Non-admin receives 403 on `GET /admin/courses` | PASS | `AdminIntegrationTest` #7 — TEACHER token → 403 (`adminRoutes.kt:32`). |
| C3 | Empty course list → 200 with empty array | PASS | `AdminIntegrationTest` #6 — zero courses → 200, body `[]`. |

### `backend-auth-security` (2 scenarios — ADDED requirement)

| # | Scenario | Status | Evidence |
|---|---|---|---|
| A1 | Admin request with valid ADMIN token is allowed on `/admin/*` | PASS | `AdminIntegrationTest` #1, #5, #8 (200); `Security.kt:77–86` `requireAdmin()` returns true for ADMIN. All three routes call `call.requireAdmin()` with early `return@{verb}` on false: `adminRoutes.kt:20, 32, 38`. |
| A2 | Non-ADMIN (STUDENT/TEACHER) token rejected with 403 on `/admin/*` | PASS | `AdminIntegrationTest` #3 (STUDENT 403 on users), #7 (TEACHER 403 on courses), #11 (STUDENT 403 on PUT role). `Security.kt:80–82` responds 403 when role ≠ ADMIN. |

---

## Correctness (Design → Implementation)

| Design Decision | Implementation | Coherence |
|---|---|---|
| Admin DTOs in `server/models/` (not `shared`) | `models/AdminDtos.kt` created in `server/` | OK — `shared` kept minimal per `server/AGENTS.md`. |
| Generic `PageResponse<T>` | `AdminDtos.kt:5–12` | OK. |
| Role update body reusing `UpdateUserRequest` | New `RoleUpdateRequest(role: String)` created instead | DEVIATION (see W-02 below). Behavior equivalent; design rationale preserved. |
| `requireAdmin()` from `Security.kt:77` reused | `adminRoutes.kt` imports `plugins.requireAdmin`; confirmed at `Security.kt:77–86` | OK — wired into all 3 routes (lines 20, 32, 38). |
| CORS `localhost:5173` | `Cors.kt:12` added | OK. |
| H2 + testApplication for backend tests | `AdminIntegrationTest` follows existing pattern with `module(initDatabase=false, seedData=false)` | OK. |

---

## Deviation Assessment

### D-01: `createdAt` omitted from `AdminUserResponse` — acceptable for v1, requires spec reconciliation before archive

- **Spec source**: `admin-user-management` Requirement: Paginated User Listing — "MUST return ... `createdAt`".
- **Implementation**: `AdminUserResponse(id, name, email, role)` — no `createdAt` field (`AdminDtos.kt:15–20`).
- **Design artifact**: `design.md` Open Question 90 — flagged but not resolved through a formal decision; `tasks.md:28` instructs omission.
- **Verdict**: **WARNING, not CRITICAL**. The `Users` table has no `createdAt` column; adding a migration is out-of-scope per `proposal.md` ("no schema migrations unless required"). The data cannot be returned because it does not exist. For v1 functionality (admin CRUD oversight) the field is metadata, not behavioral. CRITICAL would require a broken user-facing flow, which does not happen here. **However**, the spec uses normative `MUST`, so the divergence must be reconciled by amending the delta spec (drop `createdAt` from `admin-user-management` with rationale, or add a follow-up change for the migration) **before `sdd-archive`**. Leaving the spec and the code permanently divergent is the actual risk — not the omission itself.

### D-02: Dedicated `RoleUpdateRequest` instead of reusing `UpdateUserRequest` — acceptable, satisfies spec

- **Spec source**: Role Update requirement — invalid role returns 400. Scenarios U4, U5, U6 all PASS at runtime.
- **Design**: reusing `UpdateUserRequest` (which has nullable `role: UserRole?` typed).
- **Implementation**: `RoleUpdateRequest(role: String)` + route-level `UserRole.parse(request.role) ?: 400`.
- **Verdict**: **ACCEPTABLE — SUGGESTION**. Behavior satisfies all 3 spec scenarios (PASS evidence). The design rationale ("clean 400 semantics for invalid roles") is preserved: the String-typed field forces an explicit `parse` step that yields 400 on unknown values, whereas a typed `UserRole?` DTO would have deserialization throw 400 BadRequest on serialization mismatch only — less explicit control. Minor design-document drift; recommend updating `design.md` decision row to reflect actual choice for archival honesty.

### D-03: Subquery for `creatorName` instead of `innerJoin` — functionally correct, perf note

- **Spec source**: `admin-course-overview` — response includes `creatorName`; scenario C1 asserts `creatorName == "Teacher John"` → PASS.
- **Design**: "join `Courses.creatorId` with `Users.id` for `creatorName`".
- **Implementation**: `CourseService.getAllCoursesAdmin:44–47` performs a per-course `Users.selectAll().where { id eq creatorId }` lookup.
- **Rationale from apply**: `Courses.creatorId` is `varchar`, not declared as Exposed `reference()`, so an `innerJoin` against `Users.id` would not auto-type. Lookup is technically valid.
- **Verdict**: **ACCEPTABLE — SUGGESTION**. Functional compliance is proven by test #5. The pattern is N+1 (one query per course for creator + one per course for enrollment count = 2N+1 queries); acceptable at v1 admin-panel scale (catalogs small), but worth noting for performance hardening before the course catalog grows. No spec violation.

### D-04: Task 3.2 (`AdminServiceTest.kt`) deferred — WARNING, blocks archive readiness

- **Spec source**: design's Testing Strategy explicitly lists a service-layer test block. `tasks.md:41` shows `[ ] 3.2`.
- **Coverage**: All `admin-user-management`, `admin-course-overview`, `backend-auth-security` spec scenarios have passing **runtime** evidence via `AdminIntegrationTest` (11 covered; 2 SPA out of scope). The service-layer method-level test would add defense-in-depth (e.g., direct LIMIT/OFFSET boundary cases) but does not unlock any spec scenario that integration tests miss.
- **Verdict**: **WARNING**. Per SDD hard rules, an unchecked implementation task normally blocks archive readiness as **CRITICAL**. Here it is a *supplementary test task* (3.2), the integration suite already satisfies every in-scope spec scenario with passing runtime tests, and the orchestrator explicitly asked for an assessment between WARNING and acceptable. Classifying as **WARNING** (not CRITICAL for PR 1) because:
  - PR 1 mergeability: not blocked — every required spec scenario is runtime-proven.
  - Archive readiness: blocked — leaving `[ ] 3.2` in `tasks.md` unresolved is not acceptable for `sdd-archive`. Either implement `AdminServiceTest.kt` or formally amend the task list (mark 3.2 as `(deferred to follow-up change)` with a stated reason and remove the open checkbox) before archiving.

---

## Issues

### CRITICAL
None.

### WARNING
- **W-01 (spec divergence — `createdAt`)**: `AdminUserResponse` omits `createdAt` while `admin-user-management` spec mandates it. Acceptable for v1 functionality, but the delta spec MUST be reconciled before `sdd-archive` (either drop the field from the spec with rationale, or open a follow-up change for the migration). Until reconciliation, spec and code remain divergent.
- **W-04 (unchecked task 3.2)**: `tasks.md:41` `[ ] 3.2 AdminServiceTest.kt` remains incomplete. Blocks archive readiness. PR 1 mergeability is unaffected because integration tests cover all in-scope spec scenarios. Resolve before archive (implement or formally defer).

### SUGGESTION
- **S-01 (design coherence for `RoleUpdateRequest`)**: `design.md` decision row says "Reuse `UpdateUserRequest`"; the actual code introduces `RoleUpdateRequest`. Behavior is equivalent and satisfies all spec scenarios. Update the design decision row in archive so docs and code match.
- **S-02 (N+1 query in `getAllCoursesAdmin`)**: `CourseService.getAllCoursesAdmin` issues 2 sub-queries per course row (creator lookup + enrollment count). Functionally correct; consider a batched `Users.id inList (...)` lookup or a manual `innerJoin` once a foreign key is introduced, to keep admin course listing efficient as the catalog grows.

---

## Slice Boundary Check
- `admin-web/` directory: **does not exist** (verified via `ls` and glob). No drift into Phases 4–5.
- All 3 admin routes live in `server/`; no SPA code anywhere. SPA scenarios U7, U8 are correctly NOT-COVERED at this slice and will be verified at PR 2.
- CORS for `localhost:5173` is added preemptively (acceptable; Vite default port for the upcoming SPA).

---

## Final Verdict
**PASS WITH WARNINGS**

- All in-scope spec scenarios (11 of 11 backend scenarios across the 3 specs) have passing runtime tests.
- `:server:test` → 42/42 PASS (12 new admin + 30 existing).
- `requireAdmin()` is wired into all 3 routes (`adminRoutes.kt:20, 32, 38`), backed by `Security.kt:77–86`.
- No drift into Phases 4–5.
- 2 acceptable WARNINGs (createdAt spec reconciliation, unchecked task 3.2) must be resolved before `sdd-archive` but do not block PR 1.
- 2 SUGGESTIONs for design-doc honesty and a future perf cleanup.

`next_recommended`: **commit-sdd-artifacts-and-pr** — orchestrator commits the untracked `design.md` + `specs/` + this verify-report, pushes `feature/admin-web-panel`, and opens PR 1.

---

# Verification Report: Admin Web Panel — PR 2 (Frontend SPA Slice)

## Change
`admin-web-panel` — PR 2 slice (Phases 4–5: React SPA shell, auth gate, user management page, course overview page).
PR 1 (Phases 1–3: backend) was verified separately above and is not re-verified here, except for contract alignment against the backend DTOs.

- **Slot**: PR 2 / stacked-to-main / base: `feat/admin-web-panel` / slice branch: `feat/admin-web-panel-spa`
- **Verifying commits**: `471172f`, `808f873`, `9030fa9`, `fae31c9`, `6d2cf61` (5 commits; 15 files, +1034 / −8)
- **Persistence mode**: openspec
- **Strict TDD**: `false` — no JS test harness exists in the repo. SPA behavioral verification is by source inspection + successful production build + a documented manual-QA procedure (the project-sanctioned path per `design.md:82`).
- **Date**: 2026-06-25

---

## Overall Verdict (PR 2)
**PASS WITH WARNINGS**

- `npm run build` (`tsc -b && vite build`) → **0 errors, 83 modules transformed, exit code 0** (BUILD SUCCESSFUL).
- Contract alignment against `server/.../models/AdminDtos.kt` and `shared/.../models/Models.kt` → **all 4 contracts align, zero field/type mismatches**.
- All in-scope SPA spec scenarios (admin login gate U7/U8, course list display, user list + role-update UI, route protection, 401 handling) are implemented and traceable in source; runtime automated SPA tests are unavailable by design (deferred), so manual QA is the sanctioned evidence path and is documented.
- Slice boundary is clean: PR 2 touches only `admin-web/` + `openspec/.../tasks.md`; **no `server/`, `shared/`, `composeApp/`, or `iosApp/` files were modified**.
- 2 new WARNINGs (non-functional debounce, nav links causing full reloads) and 5 SUGGESTIONs. None break a spec scenario or block PR 2 merge; W-06 is a visible SPA defect worth fixing before or as a fast-follow.

---

## Build Evidence

| Command | Result |
|---|---|
| `npm run build` (in `admin-web/`) | `tsc -b && vite build` → exit **0**. `vite v6.4.3 building for production... ✓ 83 modules transformed.` Output: `dist/index.html 0.41 kB`, `dist/assets/index-*.css 3.78 kB`, `dist/assets/index-*.js 212.94 kB (gzip: 68.04 kB)`. `✓ built in 2.96s`. |
| `tsc -b` (type-check) | Pass — `tsconfig.json` is `strict` + `noUnusedLocals` + `noUnusedParameters`; no type errors. |
| Node/npm | Node `v22.22.2`, npm `10.9.7`. `node_modules` present. |

No JS test runner is configured (`package.json` has no `test` script). Per `design.md:82`, SPA automated tests are explicitly deferred to a future change; manual QA is the v1 verification strategy. Build success + strict type-check + source inspection form the evidence set, supplemented by the documented manual-QA checklist.

---

## Contract Alignment (Backend DTO ↔ SPA TypeScript)

All shapes compared field-by-field. **No mismatches found.** The SPA will not break at runtime against the real backend on contract grounds.

### Contract 1 — `POST /auth/login` → `AuthResponse` (login gate)

| Backend field (`shared/.../models/Models.kt:27-30, 6-11`) | TS field (`admin-web/src/lib/auth.tsx:19-22, 12-17`) | Align |
|---|---|---|
| `AuthResponse.token: String` | `AuthResponse.token: string` | ✅ |
| `AuthResponse.user: User` | `AuthResponse.user: User` | ✅ |
| `User.id: String` | `User.id: string` | ✅ |
| `User.name: String` | `User.name: string` | ✅ |
| `User.email: String` | `User.email: string` | ✅ |
| `User.role: UserRole` (serializes as enum name `ADMIN`/`TEACHER`/`STUDENT`) | `User.role: 'STUDENT' \| 'TEACHER' \| 'ADMIN'` | ✅ |
| `LoginRequest { email: String, password: String }` (`Models.kt:21-24`) | sends `{ email, password }` (`auth.tsx:70`) | ✅ |

### Contract 2 — `GET /admin/users` → `PageResponse<AdminUserResponse>`

| Backend field (`AdminDtos.kt:6-20`) | TS field (`Users.tsx:7-20`) | Align |
|---|---|---|
| `PageResponse.items: List<T>` | `items: T[]` | ✅ |
| `PageResponse.page: Int` | `page: number` | ✅ |
| `PageResponse.size: Int` | `size: number` | ✅ |
| `PageResponse.totalElements: Long` | `totalElements: number` | ✅ (Long→JSON number; JS `number` safe for user counts) |
| `PageResponse.totalPages: Int` | `totalPages: number` | ✅ |
| `AdminUserResponse.id: String` | `AdminUser.id: string` | ✅ |
| `AdminUserResponse.name: String` | `AdminUser.name: string` | ✅ |
| `AdminUserResponse.email: String` | `AdminUser.email: string` | ✅ |
| `AdminUserResponse.role: UserRole` | `AdminUser.role: string` | ✅ (string accepts enum name; bound to a 3-option `<select>`) |

> Note: `design.md:61` documented the page wrapper as `{ items, total, page, size }`, but the actual backend (`AdminDtos.kt:6-12`) emits `totalElements` + `totalPages`. The SPA consumes `totalElements`/`totalPages` — so **code-to-code aligns**; only the design doc is stale. This is a pre-existing design-doc drift (PR 1), not a PR 2 contract defect.

### Contract 3 — `GET /admin/courses` → `List<AdminCourseResponse>`

| Backend field (`AdminDtos.kt:23-32`) | TS field (`Courses.tsx:6-15`) | Align |
|---|---|---|
| `id: String` | `id: string` | ✅ |
| `title: String` | `title: string` | ✅ |
| `description: String` | `description: string` | ✅ |
| `creatorId: String` | `creatorId: string` | ✅ |
| `creatorName: String` | `creatorName: string` | ✅ |
| `enrollmentCount: Int` | `enrollmentCount: number` | ✅ |
| `isOfficial: Boolean` | `isOfficial: boolean` | ✅ |
| `schoolYear: Int` | `schoolYear: number` | ✅ |

### Contract 4 — `PUT /admin/users/{id}/role` → `AdminUserResponse` (body `RoleUpdateRequest`)

| Backend (`AdminDtos.kt:35-37`) | TS (`Users.tsx:41-49`) | Align |
|---|---|---|
| Request body `RoleUpdateRequest.role: String` | sends `JSON.stringify({ role })` (`Users.tsx:47`) | ✅ |
| Response `AdminUserResponse` (id, name, email, role) | expects `AdminUser` (`Users.tsx:44`) | ✅ |

The `<select>` options are exactly `['STUDENT','TEACHER','ADMIN']` (`Users.tsx:22`), all valid for `UserRole.parse`. No invalid value can be sent from the UI.

---

## Spec Scenario Coverage (SPA-relevant)

Runtime automated SPA tests are unavailable by design (`design.md:82`); coverage is proven by source inspection + build + a documented manual-QA procedure. Statuses below reflect implementation traceability. The manual-QA checklist (10 steps, recorded in apply-progress) covers every in-scope SPA scenario.

### `admin-user-management` — SPA Login Gate (PR 2 scope)

| # | Scenario | Status | Evidence |
|---|---|---|---|
| U7 | Admin login navigates to dashboard (stores token, navigates to dashboard) | PASS | `auth.tsx:68` `POST /auth/login`; `auth.tsx:73` ADMIN check passes; `auth.tsx:83` `sessionStorage.setItem('admin_token', …)` (stores bearer); `auth.tsx:85-86` `setToken`/`setUser`; `App.tsx:46-51` `/login` route redirects an authenticated user via `<Navigate to="/users" replace />`. `/users` renders the dashboard (user list). Manual QA steps 3–4. |
| U8 | Non-admin login is blocked (clears token, displays "Access denied — ADMIN role required") | PASS | `auth.tsx:73` `res.user.role !== 'ADMIN'` → `auth.tsx:75-76` removes `admin_token` + `admin_user` (clears token); `auth.tsx:79` `setError('Access denied — ADMIN role required')` (**exact spec string**); `Login.tsx:29` renders the error banner. Manual QA step 9. |

### `admin-user-management` — User List + Role Update UI (PR 2 scope)

| # | Scenario | Status | Evidence |
|---|---|---|---|
| UL1 | Paginated user list display | PASS | `Users.tsx:26-39` `GET /admin/users?page&size&query`; `Users.tsx:148-219` table + pagination; `Users.tsx:201-218` Previous/Next + `totalElements`/`totalPages` display. Manual QA step 4. |
| UL2 | Search filters results | PASS (functional) | `Users.tsx:134-141` search input; `Users.tsx:35-37` attaches `query`. **Debounce is non-functional — see W-05.** Search itself works; just not debounced. Manual QA step 5. |
| UL3 | Role update UI calls `PUT /admin/users/{id}/role`, reflects change | PASS | `Users.tsx:120-125` `handleRoleChange` → `roleMutation.mutate`; `Users.tsx:41-49` `PUT` request; `Users.tsx:85-98` optimistic cache update; `Users.tsx:104-117` handles 400 ("Invalid role value") and 404 ("User no longer exists"). Manual QA step 6. |

### `admin-course-overview` — Course List Display (PR 2 scope)

| # | Scenario | Status | Evidence |
|---|---|---|---|
| CL1 | Course list displays title, creatorName, enrollmentCount, isOfficial, schoolYear | PASS | `Courses.tsx:19-21` `GET /admin/courses`; `Courses.tsx:40-49` headers; `Courses.tsx:60-68` renders `creatorName`, `enrollmentCount`, `isOfficial` (Yes/No), `schoolYear`; `Courses.tsx:52-57` empty state. Backend scenarios C1/C2/C3 already PASS (PR 1). Manual QA step 7. |

### Cross-cutting SPA behavior

| # | Behavior | Status | Evidence |
|---|---|---|---|
| AG1 | Route protection — non-ADMIN / no token redirected to `/login` | PASS | `App.tsx:7-15` `AuthGuard`: `if (!token \|\| !user \|\| user.role !== 'ADMIN') return <Navigate to="/login" replace />`; applied to `/users` and `/courses` (`App.tsx:53-68`). Manual QA step 10. |
| AG2 | 401 handling — clear token + redirect | PASS | `api.ts:40-44`: on `response.status === 401`, `clearToken()` + `window.location.href = '/login'` + throw `ApiError(401, 'Session expired')`. |
| AG3 | Logout clears session | PASS | `auth.tsx:101-107` `logout()` → `clearToken()` + remove `admin_user` + reset state. Manual QA step 8. |

---

## Task Consistency

| Phase | Tasks | State |
|---|---|---|
| Phase 4 (SPA shell & auth) | 4.1, 4.2, 4.3, 4.4 | all `[x]` ✅ |
| Phase 5 (SPA pages) | 5.1, 5.2, 5.3, 5.4 | all `[x]` ✅ (5.1 "search with debounce" marked done but debounce is non-functional — see W-05) |
| Phase 1–3 (PR 1, carry-over) | 1.1–2.3 `[x]`; 3.1 `[x]`; 3.2 `[ ]` | unchanged — no regression in PR 1 checkboxes. Task 3.2 remains the PR 1 carry-over WARNING (W-04). |

No PR 2 task was left unchecked. Phase 4–5 are complete.

---

## Slice Boundary Check

`git diff feat/admin-web-panel..feat/admin-web-panel-spa --name-only` → 15 files, **all under `admin-web/`** plus `openspec/changes/admin-web-panel/tasks.md`:

```
admin-web/.gitignore, index.html, package.json, vite.config.ts, tsconfig.json
admin-web/src/{main.tsx, App.tsx, App.css, vite-env.d.ts}
admin-web/src/lib/{api.ts, auth.tsx}
admin-web/src/pages/{Login.tsx, Users.tsx, Courses.tsx}
openspec/changes/admin-web-panel/tasks.md
```

**No `server/`, `shared/`, `composeApp/`, or `iosApp/` files were touched.** PR 2 is purely additive SPA + task-list bookkeeping. The backend contract consumed by the SPA is the already-verified PR 1 surface. ✅ Clean boundary.

---

## Deviation Assessment (PR 2)

### D2-01: npm instead of pnpm — ACCEPTABLE (INFO)
`proposal.md:75` explicitly permits "Node.js / pnpm (or npm)". pnpm is not installed in this environment; npm was used. `package.json` is a standalone project (not a workspace member). npm is standard and fully supported by Vite. **Not a problem.**

### D2-02: No component library (297 lines plain CSS) — ACCEPTABLE (SUGGESTION S-06)
`proposal.md:34` mentions shadcn/ui as a *recommendation* ("React offers the richest admin component ecosystem"), not a requirement. v1 ships plain CSS in `App.css`. Acceptable for a v1 admin tool of this size; revisit if the panel grows.

### D2-03: Role update on-change (no Save button) — ACCEPTABLE, satisfies spec (SUGGESTION S-07)
The role `<select>` saves immediately on change (`Users.tsx:172-186`) rather than via a Save button. The spec's role-update scenarios (U4/U5/U6) govern the API contract (`PUT` is made, role persisted, 400 on invalid, 404 on missing) — all satisfied; no Save button is mandated. Minor UX note: an accidental selection commits immediately (S-07). No spec violation.

### D2-04: No `.env.example` — ACCEPTABLE (SUGGESTION S-04)
`VITE_API_BASE_URL` defaults to `http://localhost:8080` (`api.ts:1`) and is typed in `vite-env.d.ts:3-5`. The default matches the documented dev backend (`./gradlew :server:run`), so v1 functions without it. A `.env.example` would improve onboarding/deployment clarity to non-local hosts (S-04), but is not required for v1.

---

## Issues

### CRITICAL
None. Build passes, all contracts align, every in-scope SPA spec scenario is implemented and traceable, no PR 2 task is unchecked, and the slice boundary is clean.

### WARNING
- **W-05 (non-functional search debounce — task 5.1 not fully met)**: `Users.tsx:63-75` `handleSearchChange` returns a cleanup `() => clearTimeout(timeoutId)`, but the `onChange` caller at `Users.tsx:139` (`onChange={(e) => handleSearchChange(e.target.value)}`) discards the return value, so the previous timeout is never cleared. Each keystroke schedules a new 400 ms timeout and **all of them fire**, issuing one delayed fetch per keystroke instead of a single debounced fetch. Search is functional (results are eventually correct), but the documented "search input with debounce" (task 5.1) is not actually satisfied, and intermediate results flicker. **Fix**: drive the timeout from a `useEffect` keyed on `search` with a proper cleanup return, or store the timeout id in a `useRef` and clear it before scheduling a new one.
- **W-06 (nav links cause full page reloads — React Router SPA decision not honored)**: `App.tsx:22-27` `NavBar` uses `<a href="/users">` / `<a href="/courses">` instead of React Router's `<Link to="/users">`. Each nav click triggers a **full browser reload** (re-fetching the 213 KB bundle, re-initializing React), bypassing the client-side routing that the React Router v6 design decision (`design.md:14`) was chosen to provide. It is also fragile for production: it requires SPA history-API fallback on the static host, and Ktor static serving is an unresolved open question (`design.md:91`). No spec scenario breaks (the routes are reachable and render correctly), but this is a visible SPA defect and a design-coherence deviation. **Fix**: replace `<a href>` with `<Link to>` from `react-router-dom` (already a dependency).

### SUGGESTION
- **S-03 (navigate-during-render in Login)**: `Login.tsx:12-15` calls `navigate('/users')` during the render phase — a React anti-pattern that logs a "cannot update a component while rendering" warning. It is also **redundant**: `App.tsx:46-51` already redirects an authenticated user on the `/login` route via `<Navigate to="/users" replace />`. Remove the redundant block (or move the navigation into a `useEffect`).
- **S-04 (add `.env.example`)**: see D2-04. Document `VITE_API_BASE_URL` for onboarding/deployment clarity.
- **S-05 (commit the manual QA checklist)**: The SPA's only verification path is manual QA (`design.md:82`). A 10-step checklist exists (Engram apply-progress) and covers all in-scope SPA scenarios, but it is **not committed to the repo**. Commit it (e.g., `admin-web/README.md` or a `qa-checklist.md` in the change folder) so the verification procedure is reproducible from the repository rather than only from memory.
- **S-06 (component library)**: see D2-02. Plain CSS is fine for v1; revisit if the panel grows.
- **S-07 (role update on-change UX)**: see D2-03. Consider a confirm step if accidental role changes become common.

### Carry-over from PR 1 (not introduced by PR 2; still pending for `sdd-archive`)
- **W-01** (`createdAt` spec divergence) and **W-04** (unchecked task 3.2 `AdminServiceTest.kt`) remain from the PR 1 report. They must be resolved before archiving the whole change but are **not PR 2's responsibility** and do not affect the PR 2 verdict.

---

## Final Verdict (PR 2)
**PASS WITH WARNINGS**

- `npm run build` → 0 errors, 83 modules, exit 0 (BUILD SUCCESSFUL).
- Contract alignment → 4/4 contracts match field-for-field; no runtime-breaking mismatch.
- All in-scope SPA spec scenarios implemented and traceable (U7, U8, UL1, UL2, UL3, CL1, AG1–AG3); manual QA is the sanctioned evidence path and is documented.
- Phase 4–5 tasks complete; Phase 1–3 checkboxes unchanged.
- Slice boundary clean — no backend/shared files touched.
- 2 WARNINGs (W-05 debounce, W-06 nav reloads) do not break any spec scenario and do not block PR 2 merge, but W-06 is a visible SPA defect worth fixing before or as a fast-follow; W-05 should be fixed for the documented debounce feature to actually work.

`next_recommended`: **push-and-pr** — orchestrator pushes `feat/admin-web-panel-spa` and opens PR 2 (base `feat/admin-web-panel`). Recommend addressing W-06 (and ideally W-05) either in this PR or an immediate fast-follow; neither blocks merge.