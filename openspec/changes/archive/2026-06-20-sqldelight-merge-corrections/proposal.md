# Proposal: SQLDelight Merge Security & Contract Corrections

## Intent

Fix 7 push blockers from the `sqldelight-test` branch merge: missing backend authorization, client-controlled role escalation, hardcoded secrets/credentials, client/server progress contract mismatch, missing Authorization headers, and orphaned relational records.

## Scope

### In Scope
- Enforce JWT `userId` claim verification and RBAC on all protected routes.
- Restrict public registration to LEARNER and TEACHER only in current code; reject/ignore ADMIN role from client.
- Keep course reads protected (logged-in user required); do not open unauthenticated public course access.
- Move JWT secret to environment variable; keep automatic admin creation in seed data, but read admin credentials from environment/config and remove console logging of reusable secrets.
- Move `CompleteLessonRequest` to `shared`, align app `UserApi` to send it.
- Add in-memory (memory-only) token holder and `Authorization` header injection in `NetworkClient`.
- Convert Exposed relation columns to `reference(...)` with `onDelete` behavior: cascade course -> lessons/exercises/enrollments/progress; cascade lesson -> exercises/progress; cascade deleted student -> progress/enrollments.
- Hide correct exercise answers from normal learner/content endpoints.
- Update integration tests to assert 403 behavior.

### Out of Scope
- New login UI or auth flows beyond token storage/header injection.
- Teacher-scoped visibility into student progress for owned courses (deferred to future slice).
- Teacher user deletion handling and cascading deletion of teacher-owned courses (deferred).
- Production key rotation mechanism.

## Capabilities

### New Capabilities
- `backend-auth-security`: JWT route authorization, role validation, secure secrets, and hardened seed data.
- `client-server-contract`: Aligned progress DTO and client `Authorization` header injection.
- `database-integrity`: Foreign key constraints and cascade behavior in Exposed schema.

### Modified Capabilities
- None (no existing specs).

## Approach

Single PR corrective patch (~270 lines) within the 400-line review budget. Add a `Security.kt` helper for principal/role extraction and route authorization checks. Update `NetworkClient` with `defaultRequest` header injection using a minimal in-memory token holder. Move `CompleteLessonRequest` to `shared/src/commonMain`. Convert `varchar` relation columns to `reference(...)` with `onDelete = CASCADE` for course/lesson/student deletions as specified; do NOT cascade teacher deletion to owned courses in this slice. Update `SeedData` to use env/config credentials and `BCrypt` hash for admin, removing console logging of secrets. Filter correct answers from learner-facing exercise endpoints.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `server/.../plugins/Security.kt` | Modified | Env JWT secret, role extraction helper |
| `server/.../routes/*Routes.kt` | Modified | Add userId/role checks; hide correct answers from learner endpoints |
| `server/.../database/Tables.kt` | Modified | FK references with cascade (course, lesson, student deletions) |
| `server/.../seed/SeedData.kt` | Modified | Env/config admin credentials, BCrypt hash, remove secret logging |
| `shared/.../models/Models.kt` | Modified | Add `CompleteLessonRequest` |
| `composeApp/.../NetworkClient.kt` | Modified | In-memory token storage, defaultRequest header |
| `composeApp/.../data/UserApi.kt` | Modified | Send aligned DTO |
| `server/.../ServerIntegrationTest.kt` | Modified | Assert 403 behaviors |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| H2 `reference()` incompatibility in tests | Low | Verify Exposed+H2 combo before merge |
| App test breakage from `UserApi` signature change | Low | `MockEngine` tests bypass `NetworkClient`; no direct `UserApi` tests exist |
| RBAC drift without centralized helper | Med | Add `authorizeOwnerOrAdmin()` helper in `Security.kt` |

## Rollback Plan

Revert the single PR commit. The backend will return to the pre-merge security posture (functional but with known blockers), and the client will revert to the old progress DTO shape.

## Dependencies

- `BCrypt` already available in server module (via `authRoutes.kt`).

## Success Criteria

- [ ] All JWT-protected routes reject requests with mismatched `userId` or insufficient role.
- [ ] Registration accepts only LEARNER/TEACHER from public clients in current code; ADMIN is rejected/ignored.
- [ ] Course actions require logged-in user; no unauthenticated public course reads opened.
- [ ] JWT secret is read from environment; seed admin credentials come from env/config and are not logged.
- [ ] App progress calls return 200 with correct `CompleteLessonRequest` payload and `Authorization` header.
- [ ] Progress visibility: learner sees own progress; admin sees all progress. Teacher-owned-course progress access is deferred.
- [ ] Correct exercise answers are hidden from normal learner/content endpoints.
- [ ] Database cascades match specified deletion behavior; teacher deletion does not cascade to owned courses.
- [ ] Integration tests pass with H2 and FK constraints enabled.
