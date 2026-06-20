## Exploration: sqldelight-merge-corrections

### Current State
The `origin/sqldelight-test` branch introduced SQLDelight persistence in `composeApp` and expanded the backend with JWT auth, Ktor routes, and seed data. After merging into `main`, fresh reviewers identified 7 push blockers spanning security, data integrity, and client/server contract alignment.

Key findings:
- Routes are wrapped in `authenticate("auth-jwt")` but never compare the JWT `userId` claim against path/body parameters, and never enforce RBAC.
- `RegisterRequest` and `UpdateUserRequest` accept arbitrary `UserRole` from the client, enabling trivial privilege escalation.
- The JWT signing secret is a hardcoded string in `Security.kt`.
- `SeedData` hardcodes admin credentials (`admin123`), prints them to stdout, and inserts a non-bcrypt hash placeholder that prevents actual login.
- `DatabaseFactory` defaults the DB password to `"mathimapp"` when the env var is missing.
- The app sends `UserProgress` to `POST /progress`, but the server deserializes `CompleteLessonRequest`; fields do not align.
- `NetworkClient.kt` and all `*Api` classes send no `Authorization` header; the app has no token storage or auth flow.
- Exposed tables use plain `varchar` columns for all relationships instead of `reference(...)` with FK/cascade behavior, allowing orphaned records.

### Affected Areas
- `server/src/main/kotlin/com/example/proyectofinal/plugins/Security.kt` — hardcoded secret, no role extraction helper.
- `server/src/main/kotlin/com/example/proyectofinal/routes/authRoutes.kt` — client-controlled role on register.
- `server/src/main/kotlin/com/example/proyectofinal/routes/userRoutes.kt` — missing userId/role authorization.
- `server/src/main/kotlin/com/example/proyectofinal/routes/courseRoutes.kt` — missing userId/role authorization.
- `server/src/main/kotlin/com/example/proyectofinal/routes/lessonRoutes.kt` — missing userId/role authorization.
- `server/src/main/kotlin/com/example/proyectofinal/routes/exerciseRoutes.kt` — missing userId/role authorization.
- `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` — no FK constraints or cascade behavior.
- `server/src/main/kotlin/com/example/proyectofinal/database/Database.kt` — default DB password.
- `server/src/main/kotlin/com/example/proyectofinal/seed/SeedData.kt` — hardcoded admin credentials and logging.
- `server/src/main/kotlin/com/example/proyectofinal/models/UserDto.kt` — accepts arbitrary role.
- `server/src/main/kotlin/com/example/proyectofinal/models/ProgressDto.kt` — `CompleteLessonRequest` is server-only; needs to be shared.
- `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` — missing `CompleteLessonRequest`.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/NetworkClient.kt` — no `Authorization` header injection.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/UserApi.kt` — sends wrong DTO to progress endpoint.
- `server/src/test/kotlin/com/example/proyectofinal/ServerIntegrationTest.kt` — tests currently assert that TEACHER registration succeeds and do not assert 403 behavior.

### Approaches
1. **Single PR — Minimal Corrective Patch**
   - Fix all 7 blockers in one PR.
   - Add JWT env secret + principal/role extraction helper.
   - Harden all routes with userId and role checks.
   - Remove hardcoded credentials/logging; fix seed admin bcrypt hash.
   - Move `CompleteLessonRequest` to `shared`; update app `UserApi` to send it.
   - Add minimal in-memory token holder and `defaultRequest` header injection in `NetworkClient`.
   - Convert `varchar` relation columns to `reference(...)` with `onDelete` behavior.
   - Update integration tests.
   - Pros: Atomic, no intermediate broken state, all blockers resolved together.
   - Cons: High review density; mixes security, schema, and client contract.
   - Effort: Medium
   - Estimated changed lines: ~270 (additions + deletions), within the 400-line budget.

2. **Two PRs — Backend Security First, Client Contract Second**
   - PR 1 (`server` + `shared` partial): blockers 1-4 and 7 (auth, roles, JWT secret, credentials, schema FKs).
   - PR 2 (`shared` + `composeApp`): blockers 5-6 (progress contract, auth headers).
   - Pros: Smaller, focused reviews; respects module ownership boundaries.
   - Cons: PR 2 depends on PR 1 merge or rebase; client remains broken (401) until PR 2 lands.
   - Effort: Medium

3. **Three PRs — Security, Schema, Client**
   - PR 1: Auth, roles, JWT secret, credentials.
   - PR 2: Schema FKs.
   - PR 3: Client contract + headers.
   - Pros: Very small diffs.
   - Cons: Coordination overhead; schema PR is tiny and touches same modules as security PR.
   - Effort: Medium-High

### Recommendation
**Approach 1 (Single PR)** is recommended because:
- The total estimated line count (~270) is safely under the 400-line budget.
- The blockers are tightly coupled: backend auth is meaningless without client headers, and the progress contract mismatch spans both sides.
- Splitting would leave the client non-functional against the backend until the second PR lands, which is risky for a corrective merge.
- The changes are localized and do not introduce new features (e.g., no login UI), making a single reviewable unit reasonable.

### Risks
- **Exposed `reference()` on H2**: The integration tests use H2 in-memory. `reference(...)` must be verified to work with the Exposed + H2 combo used in tests.
- **App test breakage**: App repository tests use `MockEngine` with custom `HttpClient` instances, so `NetworkClient` header injection should not affect them. However, `UserApi.saveUserProgress` signature change requires updating any direct tests (currently none in app tests).
- **RBAC consistency**: Without a centralized authorization helper or Ktor `RoleBasedAuthorization` plugin, manual checks in every route could drift. A small `authorizeOwnerOrAdmin()` helper in `Security.kt` mitigates this.
- **Admin seed hash**: Fixing the admin bcrypt hash requires adding the `BCrypt` dependency to the seed scope (already available in `authRoutes.kt` via the same module).

### Ready for Proposal
Yes. Proceed to `sdd-propose` with the single PR scope. The orchestrator should tell the user that a single corrective PR is feasible under the review budget, but the review will be security-dense.
