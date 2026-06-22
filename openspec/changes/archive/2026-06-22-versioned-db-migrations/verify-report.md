## Verification Report

**Change**: versioned-db-migrations
**Version**: N/A (initial capability `server-db-migrations`)
**Mode**: Standard
**Strict TDD**: Inactive (no testing-capabilities/config cache or `strict_tdd` flag found; standard verify applies)

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 12 |
| Tasks complete | 12 |
| Tasks incomplete | 0 |

All tasks across Phase 1 (Foundation), Phase 2 (Core Implementation), Phase 3 (Testing), and Phase 4 (Cleanup/Documentation) are checked in `tasks.md` and corroborated by `apply-progress.md`. No unchecked implementation tasks remain.

### Build & Tests Execution
**Build**: ✅ Passed
```text
$ ./gradlew :server:test --rerun-tasks
> Task :server:compileKotlin
> Task :server:compileTestKotlin
> Task :server:test
BUILD SUCCESSFUL in 1m 55s
8 actionable tasks: 8 executed
```

**Tests**: ✅ 30 passed / 0 failed / 0 errors / 0 skipped
```text
server/build/test-results/test/*.xml aggregate:
com.example.proyectofinal.AuthServiceTest          tests=2 failures=0 errors=0 skipped=0
com.example.proyectofinal.CourseServiceTest        tests=2 failures=0 errors=0 skipped=0
com.example.proyectofinal.LessonExerciseServiceTest tests=7 failures=0 errors=0 skipped=0
com.example.proyectofinal.ServerIntegrationTest    tests=17 failures=0 errors=0 skipped=0
com.example.proyectofinal.UserServiceTest          tests=2 failures=0 errors=0 skipped=0
TOTAL tests=30 failures=0 errors=0 skipped=0
```

Migration-specific runtime evidence:
- `LessonExerciseServiceTest > database init backfills missing course school year column and is idempotent` — exercises a pre-created legacy schema (no `school_year`), two consecutive `DatabaseFactory.init()` calls, and asserts the column exists with default `0`. Covers idempotent repeated startup.
- `LessonExerciseServiceTest > database init fails when a pending migration cannot be applied` — baselines an incompatible DB at V1 and asserts `DatabaseFactory.init()` throws, with the error chain referencing `V2__ensure_courses_school_year.sql` / `courses` / `school_year`. Covers failure-blocking startup.

**Coverage**: ➖ Not available (no coverage plugin configured for `server`; not required by this slice).

### Spec Compliance Matrix
| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| Startup runs versioned migrations | Fresh startup migrates successfully | `ServerIntegrationTest` / `ServiceLayerTest` classes via `setupTestDatabase()` / `initServiceTestDatabase()` → `DatabaseFactory.init()` on fresh H2 (all 30 tests depend on this succeeding) | ✅ COMPLIANT |
| Startup runs versioned migrations | Migration failure blocks service startup | `LessonExerciseServiceTest > database init fails when a pending migration cannot be applied` | ✅ COMPLIANT |
| Baseline matches current schema | Fresh database contains the expected schema | `ServerIntegrationTest` + `CourseServiceTest` exercise all 8 tables; `courses.school_year` asserted via `getOfficialCourses(year)` and `schoolYear` field equality | ✅ COMPLIANT |
| Baseline matches current schema | Baseline omission is observable as a schema mismatch | `LessonExerciseServiceTest > database init fails when a pending migration cannot be applied` (no-silent-success intent covered; no dedicated deliberate-omission assertion test) | ⚠️ PARTIAL |
| Existing `courses.school_year` databases remain compatible | Prior workaround database starts cleanly | `LessonExerciseServiceTest > database init backfills missing course school year column and is idempotent` | ✅ COMPLIANT |
| Existing `courses.school_year` databases remain compatible | Repeated startup stays stable | Same test calls `DatabaseFactory.init()` twice and re-asserts `school_year` presence + default `0` | ✅ COMPLIANT |
| No runtime schema creation workaround | Initialization is migration-driven only | Static: `Database.kt` runs `Flyway.configure().dataSource(...).locations("classpath:db/migration").baselineOnMigrate(true).baselineVersion("1").load().migrate()` before `Database.connect()`; no `SchemaUtils` / inline `ALTER` in main source. Runtime: 30 tests pass through Flyway. | ✅ COMPLIANT |
| No runtime schema creation workaround | Missing schema is not silently repaired outside migrations | `LessonExerciseServiceTest > database init fails when a pending migration cannot be applied` (init aborts rather than silently creating `courses`) | ✅ COMPLIANT |

**Compliance summary**: 7/8 scenarios COMPLIANT, 1/8 PARTIAL, 0 UNTESTED, 0 FAILING.

### Correctness (Static Evidence)
| Requirement | Status | Notes |
|------------|--------|-------|
| Flyway dependency wiring | ✅ Implemented | `gradle/libs.versions.toml` adds `flyway = "10.22.0"` plus `flyway-core` and `flyway-database-postgresql` aliases; `server/build.gradle.kts` wires `implementation(libs.flyway.core)` + `implementation(libs.flyway.database.postgresql)` and keeps `testImplementation(libs.h2)` with an explicit H2-support comment. |
| Migration-driven startup | ✅ Implemented | `Database.kt` replaces `SchemaUtils.create(...)` and the inline `ALTER TABLE` with a private `migrate(...)` helper invoked before `Database.connect(...)`. No `SchemaUtils` references remain in `server/src` (grep: 0 matches). The only `ALTER TABLE` in `server/src/main` is the legitimate `V2` migration script. |
| V1 baseline matches `Tables.kt` | ✅ Implemented | `V1__baseline_current_schema.sql` defines all 8 current tables (`users`, `courses`, `lessons`, `exercises`, `user_progress`, `completed_lessons`, `completed_exercises`, `enrolled_courses`) with matching columns, PKs, unique index, FKs with `ON DELETE CASCADE`, and `courses.school_year INTEGER NOT NULL DEFAULT 0`. `creator_id` intentionally stays a plain non-FK value to match `Tables.kt` (documented inline). |
| V2 guarded compatibility bridge | ✅ Implemented | `V2__ensure_courses_school_year.sql` uses `ALTER TABLE courses ADD COLUMN IF NOT EXISTS school_year INTEGER NOT NULL DEFAULT 0`, preserving the prior inline workaround for legacy non-baselined DBs. |
| Obsolete V1 stub removed | ✅ Implemented | `V1__add_courses_school_year.sql` deleted; `db/migration/` contains only `V1__baseline_current_schema.sql` and `V2__ensure_courses_school_year.sql` (no Flyway version conflict). |
| Drift/checksum rule documented | ✅ Implemented | `openspec/backlog.md` records that every future server schema change requires a matching Flyway migration script; CI drift/checksum validation is deferred. |

### Coherence (Design)
| Decision | Followed? | Notes |
|----------|-----------|-------|
| Migration tool: Flyway programmatic API in server startup | ✅ Yes | `Database.kt` uses `Flyway.configure()...load().migrate()`; no Liquibase or custom runner introduced. |
| Startup order: Flyway before `Database.connect(...)` | ✅ Yes | `init(url, driver, user, password)` calls `migrate(...)` then `Database.connect(...)`, preserving `Application.module()` ordering so routes/seed see migrated schema. |
| Baseline compatibility: V1 full schema + V2 guarded bridge, `baselineOnMigrate(true)` + `baselineVersion("1")` | ✅ Yes | Exact configuration present in `migrate(...)`; V1 captures full schema, V2 is `ADD COLUMN IF NOT EXISTS`. |
| Scope boundary: server Flyway only (no SQLDelight, no LEARNER→STUDENT rewrite) | ✅ Yes | No `composeApp`/`shared`/SQLDelight changes; `LEARNER` parser compatibility untouched (still exercised by `ServerIntegrationTest > legacy learner role values still authenticate and hydrate as student`). |
| File changes match design table | ✅ Yes | All listed files modified/created/deleted as specified; `README.md` was optional and the drift rule landed in `openspec/backlog.md` instead (an allowed alternative named in the design). |
| Testing strategy (H2 fresh init, legacy backfill idempotency, conservative shared SQL) | ✅ Yes | SQL uses only `VARCHAR`/`INTEGER`/`BOOLEAN`/`TEXT`/`CREATE TABLE IF NOT EXISTS`/`ALTER TABLE ... ADD COLUMN IF NOT EXISTS`; tests cover both fresh and legacy paths plus a failure path. |

### Issues Found
**CRITICAL**: None
**WARNING**: None
**SUGGESTION**:
- The "Baseline omission is observable as a schema mismatch" scenario is only indirectly covered. The failure-path test proves missing schema aborts startup noisily (satisfying the no-silent-repair intent), but no test deliberately introduces a baseline omission and asserts a specific absent column. This is a defensive/tautological scenario and not a blocker; a future hardening test could create a reduced baseline and assert the resulting schema gap, but it is low priority.

### Verdict
PASS — All 12 tasks complete; `:server:test` passes with 30/30 tests green; 7/8 spec scenarios COMPLIANT and the remaining 1 PARTIAL has its intent covered by the failure-path test; design decisions are followed with no deviations; no CRITICAL or WARNING issues.
