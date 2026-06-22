# Proposal: Rename LEARNER to STUDENT with Backward Compatibility

## Intent

Product language and roadmap already use "student". Keeping `UserRole.LEARNER` in code while upcoming slices (`teacher-course-ownership`, `classroom-join-codes`) add role-dependent logic will multiply the refactor surface. Renaming now aligns code, specs, and product language before that debt compounds.

## Scope

### In Scope
- Rename `UserRole.LEARNER` → `UserRole.STUDENT` in `shared`, `server`, and `composeApp`.
- Add safe parser/adapter in `server` and `composeApp` that maps both `"LEARNER"` and `"STUDENT"` to `UserRole.STUDENT`.
- Update all role checks, route guards, service logic, and tests.
- Update `backend-auth-security` spec concrete values from `LEARNER` to `STUDENT`.
- Update functional spec natural language from "learner" to "student" for consistency.

### Out of Scope
- Full DB migration rewriting existing rows (deferred until `versioned-db-migrations`).
- Dropping legacy parser/adapter (deferred until migration slice).
- Accepting legacy `LEARNER` JSON request bodies with a custom serializer.
- UI/UX redesign or new screens.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `backend-auth-security`: Update concrete role value requirements and scenarios from `LEARNER` to `STUDENT`.

## Approach

Replace the enum constant and every `UserRole.valueOf` call with a backward-compatible parser. On the server, a `safeValueOf` maps `"LEARNER"` and `"STUDENT"` to `UserRole.STUDENT`. In the app, a custom SQLDelight `ColumnAdapter` does the same. JWT claims with `"LEARNER"` continue to work. After versioned migrations arrive, the legacy mapping can be removed.

**Review strategy**: Group commits by module (`shared` → `server` → `composeApp` → `specs`) to keep review noise low. If the task forecast exceeds the 400-line budget, split into a chain: (1) `shared` + `server`, (2) `composeApp` + `specs`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/src/commonMain/.../Models.kt` | Modified | Enum constant rename |
| `server/src/main/kotlin/.../database/Tables.kt` | Modified | Column default string |
| `server/src/main/kotlin/.../service/*.kt` | Modified | Parser + role checks |
| `server/src/main/kotlin/.../routes/*.kt` | Modified | Role guards |
| `server/src/main/kotlin/.../plugins/Security.kt` | Modified | JWT claim parser |
| `server/src/test/...` | Modified | Fixtures and assertions |
| `composeApp/.../sqldelight/AppDatabase.sq` | Modified | Schema default |
| `composeApp/.../di/AppModule.kt` | Modified | Custom column adapter |
| `composeApp/.../data/KtorUserRepository.kt` | Modified | Fallback role |
| `composeApp/src/commonTest/...` | Modified | Expectations |
| `openspec/specs/backend-auth-security/spec.md` | Modified | Concrete role values |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Runtime crash on legacy DB rows | High | Mandatory safe parser/adapter |
| Missed string literals | Med | String search audit before commit |
| SQLDelight build failure | Med | Regenerate interfaces and verify locally |
| Wide review surface | Med | Group commits by module |

## Rollback Plan

Rollback is safe as a plain revert only if no server rows, local cache rows, or JWT claims using `STUDENT` have been emitted yet. Because the old enum lacks `STUDENT`, a pure revert after deployment would crash when older code reads those values.

Concrete recovery path if `STUDENT` has already been emitted:

1. Deploy a compatibility hotfix (or rollback target) that accepts both `LEARNER` and `STUDENT` again.
2. If reverting to legacy-only code is still required, run a migration-aware rollback that rewrites persisted `STUDENT` values back to `LEARNER`, clears or migrates local SQLDelight caches, and invalidates/rotates JWTs carrying `STUDENT` claims.
3. After compatibility is restored or data is migrated, complete the revert.

## Dependencies

- `versioned-db-migrations` (future) to eventually drop legacy mapping.

## Success Criteria

- [ ] `UserRole.STUDENT` exists and `LEARNER` does not in source.
- [ ] Server parser accepts both `"LEARNER"` and `"STUDENT"`.
- [ ] App SQLDelight adapter accepts both `"LEARNER"` and `"STUDENT"`.
- [ ] All tests pass.
- [ ] `backend-auth-security` spec updated.
- [ ] `generateSqlDelightInterface` succeeds.
