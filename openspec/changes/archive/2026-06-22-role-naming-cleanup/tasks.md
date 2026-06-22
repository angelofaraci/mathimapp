# Tasks: Rename LEARNER to STUDENT with Backward Compatibility

## Review Workload Forecast

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: single-pr
400-line budget risk: Low

| Field | Value |
|-------|-------|
| Estimated changed lines | ~120–130 (additions + deletions) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | auto-forecast |

## Phase 1: Shared Contract & Parser

- [x] 1.1 Rename `UserRole.LEARNER` → `UserRole.STUDENT` in `shared/src/commonMain/.../models/Models.kt`; change `User(role)` default to `UserRole.STUDENT`
- [x] 1.2 Add `UserRole.parse(value: String): UserRole?` companion method mapping `"LEARNER"` and `"STUDENT"` → `STUDENT`, `"TEACHER"` → `TEACHER`, `"ADMIN"` → `ADMIN`, else `null`
- [x] 1.3 Write parser tests: verify `parse("LEARNER")` and `parse("STUDENT")` both return `UserRole.STUDENT`, unknown values return `null`

## Phase 2: Server Persistence & Mappers

- [x] 2.1 `server/.../database/Tables.kt` — Change `Users.role` default `"LEARNER"` → `"STUDENT"`
- [x] 2.2 `server/.../models/UserDto.kt` — Change `RegisterRequest(role)` default `UserRole.LEARNER` → `UserRole.STUDENT`
- [x] 2.3 `server/.../service/ServiceMappers.kt` — Replace `UserRole.valueOf(this[Users.role])` with `UserRole.parse(...) ?: error(...)` (fail-loud on unknown persistence value)
- [x] 2.4 `server/.../service/AuthService.kt` — Replace `UserRole.valueOf(role)` in `AuthUserRecord.toUser()` with `UserRole.parse(...) ?: error(...)` to fail-loud on unknown stored values; `createUser` already uses `request.role.name` (emits `"STUDENT"`)

## Phase 3: Server Role Guards & Routes

- [x] 3.1 `server/.../plugins/Security.kt` — Replace `runCatching { UserRole.valueOf(role) }.getOrNull()` with `UserRole.parse(role)` (nullable; preserves current null-return behavior for invalid JWT claims)
- [x] 3.2 `server/.../routes/userRoutes.kt` — Rename `UserRole.LEARNER` → `UserRole.STUDENT` (lines 63, 74); update error messages: `"Only learners"` → `"Only students"`, `"deprecated for learners"` → `"deprecated for students"`
- [x] 3.3 `server/.../service/UserService.kt` — Rename `UserRole.LEARNER` → `UserRole.STUDENT` (line 64)
- [x] 3.4 `server/.../service/LessonService.kt` — Rename `UserRole.LEARNER` → `UserRole.STUDENT` (lines 98, 173)
- [x] 3.5 `server/.../service/ContentReadAccess.kt` — Rename `UserRole.LEARNER` → `UserRole.STUDENT` (line 20)

## Phase 4: Compose App Local Persistence

- [x] 4.1 `composeApp/.../sqldelight/AppDatabase.sq` — Change `UserEntity.role` default `'LEARNER'` → `'STUDENT'`
- [x] 4.2 `composeApp/.../di/AppModule.kt` — Replace `EnumColumnAdapter()` for `UserEntity.Adapter(roleAdapter)` with custom `ColumnAdapter<UserRole, String>` that decodes via `UserRole.parse(...) ?: error(...)` and encodes via `role.name`
- [x] 4.3 `composeApp/.../data/KtorUserRepository.kt` — Change fallback `UserRole.LEARNER` → `UserRole.STUDENT` (line 34)

## Phase 5: Test Updates

- [x] 5.1 `server/.../ServerIntegrationTest.kt` — Replace all `UserRole.LEARNER` → `UserRole.STUDENT` (6 references); update string `"LEARNER"` → `"STUDENT"` in seed data; add test verifying legacy `"LEARNER"` stored role loads as student
- [x] 5.2 `server/.../ServiceLayerTest.kt` — Replace all `UserRole.LEARNER` → `UserRole.STUDENT` (15 references)
- [x] 5.3 `composeApp/.../KtorUserRepositoryTest.kt` — Replace all `UserRole.LEARNER` → `UserRole.STUDENT` (7 references); update test name `"returns LEARNER as default"` → `"returns STUDENT as default"`
- [x] 5.4 Verify: `./gradlew :server:test` passes
- [x] 5.5 Verify: `./gradlew :composeApp:jvmTest` passes
- [x] 5.6 Verify: `./gradlew :composeApp:generateSqlDelightInterface` succeeds

## Phase 6: Spec Archive & Documentation

- [x] 6.1 Update `openspec/specs/backend-auth-security/spec.md` — Replace `LEARNER`/`learner` → `STUDENT`/`student` throughout (Requirements: Registration Role Limits, Protected Course Access, Learner Responses Hide Correct Answers); preserve backward-compatibility note in a `Compatibility` section
- [x] 6.2 Archive delta spec from `openspec/changes/role-naming-cleanup/specs/backend-auth-security/spec.md` into main spec; remove delta spec file
