# Exploration: role-naming-cleanup

## Current State

The codebase currently uses `UserRole.LEARNER` as the student role everywhere:

- **`shared`** — `UserRole` enum is `ADMIN, TEACHER, LEARNER`.
- **`server`** — Exposed `Users.role` defaults to `"LEARNER"`; `UserRole.valueOf(...)` maps DB strings to the enum in `ServiceMappers.kt`, `AuthService.kt`, and `Security.kt`; role checks in routes and services use `UserRole.LEARNER`.
- **`composeApp`** — SQLDelight `UserEntity.role` defaults to `'LEARNER'`; `EnumColumnAdapter` handles DB serialization; `KtorUserRepository` falls back to `UserRole.LEARNER` on failure.
- **Specs** — Functional specs (exercise-completion, lesson-progress-derivation, progress-sync) use "student" in natural-language scenarios, while `backend-auth-security` uses `LEARNER` as the concrete role value.
- **Tests** — ~26 matches in server tests and ~10 in client tests hardcode `UserRole.LEARNER`.
- **UI copy** — No Compose resources currently display the role name, but future screens (onboarding, profiles, teacher dashboards) will.

## Affected Areas

| File / Module | Why it is affected |
|---------------|-------------------|
| `shared/src/commonMain/kotlin/.../models/Models.kt` | Enum definition and default value |
| `server/src/main/kotlin/.../database/Tables.kt` | Exposed column default `"LEARNER"` |
| `server/src/main/kotlin/.../models/UserDto.kt` | `RegisterRequest` default role |
| `server/src/main/kotlin/.../service/ServiceMappers.kt` | `UserRole.valueOf(this[Users.role])` |
| `server/src/main/kotlin/.../service/AuthService.kt` | `UserRole.valueOf(role)` |
| `server/src/main/kotlin/.../plugins/Security.kt` | `UserRole.valueOf(role)` from JWT claim |
| `server/src/main/kotlin/.../routes/authRoutes.kt` | Rejects `ADMIN`, allows `LEARNER`/`TEACHER` |
| `server/src/main/kotlin/.../routes/userRoutes.kt` | `role != UserRole.LEARNER` guards |
| `server/src/main/kotlin/.../routes/lessonRoutes.kt` | Role-scoped answer hiding |
| `server/src/main/kotlin/.../service/LessonService.kt` | `hideAnswers = role == UserRole.LEARNER` |
| `server/src/main/kotlin/.../service/UserService.kt` | `role != UserRole.LEARNER` guard |
| `server/src/main/kotlin/.../service/ContentReadAccess.kt` | `UserRole.LEARNER` read-access gate |
| `server/src/test/.../ServerIntegrationTest.kt` | Tokens, inserts, assertions |
| `server/src/test/.../ServiceLayerTest.kt` | Fixtures and assertions |
| `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq` | `DEFAULT 'LEARNER'` |
| `composeApp/src/commonMain/kotlin/.../di/AppModule.kt` | `EnumColumnAdapter` for `UserEntity.role` |
| `composeApp/src/commonMain/kotlin/.../data/KtorUserRepository.kt` | Default/fallback role |
| `composeApp/src/commonTest/.../KtorUserRepositoryTest.kt` | Expectations |
| `openspec/specs/backend-auth-security/spec.md` | Concrete role value requirements |
| `openspec/specs/exercise-completion/spec.md` | Natural-language "student" terminology |
| `openspec/specs/lesson-progress-derivation/spec.md` | Natural-language "student" terminology |
| `openspec/specs/progress-sync/spec.md` | Natural-language "student" terminology |

## Approaches

### 1. Keep `LEARNER`, align specs to "learner"
- **Description** — Leave the enum unchanged; rewrite spec natural language from "student" to "learner".
- **Pros** — Zero runtime risk; zero DB migration need; ~20-line spec change; fastest delivery.
- **Cons** — Perpetuates a mismatch with product language (roadmap/backlog already says "student"); `TEACHER`/`LEARNER` is less conventional than `TEACHER`/`STUDENT`; future onboarding and classroom features will feel linguistically awkward.
- **Effort** — Low

### 2. Rename to `STUDENT` with backward-compatible DB parser
- **Description** — Rename the enum constant to `STUDENT`, replace every `UserRole.valueOf` with a safe parser that maps both `"LEARNER"` and `"STUDENT"` to `UserRole.STUDENT`, and replace SQLDelight's `EnumColumnAdapter` with a custom `ColumnAdapter` that does the same.
- **Pros** — Permanently aligns code, specs, and product language; `TEACHER`/`STUDENT` is intuitive; avoids a much larger refactor after `teacher-course-ownership` and `classroom-join-codes` land.
- **Cons** — Touches ~40+ files across three modules; custom adapters/parsers add a small amount of temporary debt; tests must be updated comprehensively.
- **Effort** — Medium

### 3. Rename to `STUDENT` with full DB migration
- **Description** — Same as Approach 2, but also update every existing DB row from `"LEARNER"` to `"STUDENT"` and drop the legacy parser.
- **Pros** — Cleanest long-term state; no legacy-mapping debt.
- **Cons** — The project currently has no versioned migration strategy (`SchemaUtils.create` is used); the next slice is `versioned-db-migrations`, so doing this now would require ad-hoc migration scripts or accepting app data loss on reinstall. Risk of crashing installed apps that have local SQLDelight rows with `"LEARNER"`.
- **Effort** — High

## Recommendation

**Adopt Approach 2: rename to `STUDENT` with a backward-compatible parser.**

Rationale:
- The product language has already drifted to "student" in specs, backlog, and roadmap descriptions.
- Delaying this until after `teacher-course-ownership` (which references "student progress") would multiply the refactor surface.
- A safe parser (`parseRole("LEARNER") -> STUDENT`, `parseRole("STUDENT") -> STUDENT`) in both `server` and `composeApp` mitigates DB migration risk without blocking on the `versioned-db-migrations` slice.
- After versioned migrations are introduced, the legacy parser can be deprecated and removed in a follow-up slice.

## Risks

1. **Runtime crash on existing rows** — Both Exposed (`UserRole.valueOf`) and SQLDelight (`EnumColumnAdapter`) will crash if they encounter `"LEARNER"` after the enum is renamed. A safe parser/adapter is mandatory.
2. **JWT token compatibility** — Already-issued tokens with claim `"role": "LEARNER"` will fail `Security.kt` parsing unless the parser is backward-compatible.
3. **Wide review surface** — ~40+ files will change. Although most changes are mechanical, the review budget is at risk of being consumed by noise. Grouping commits as `shared` -> `server` -> `composeApp` -> `specs` helps.
4. **Missed string literals** — Any hardcoded `"LEARNER"` in seed scripts, test data, or comments that is not caught becomes a latent bug.
5. **SQLDelight schema cache** — Developers must regenerate SQLDelight code after the schema default changes; CI/local builds may fail until `./gradlew generateSqlDelightInterface` runs.

## Ready for Proposal

**Yes.**

The orchestrator should confirm with the user that `STUDENT` is the preferred product term, then proceed to `sdd-propose`. The proposal must explicitly include:
- A `UserRole.safeValueOf(String)` helper (or equivalent) in `shared` or `server`.
- A custom SQLDelight `ColumnAdapter<UserRole, String>` in `composeApp`.
- A commit/PR strategy that groups changes by module to keep review noise low.
