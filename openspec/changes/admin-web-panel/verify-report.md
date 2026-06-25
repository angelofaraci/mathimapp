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