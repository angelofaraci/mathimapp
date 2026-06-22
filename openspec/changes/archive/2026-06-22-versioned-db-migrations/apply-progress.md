# Apply Progress: Versioned Database Migrations

**Change**: `versioned-db-migrations`
**Mode**: Standard

## Completed Tasks

- [x] 1.1 Add Flyway version and library aliases to `gradle/libs.versions.toml`
- [x] 1.2 Add Flyway dependencies to `server/build.gradle.kts`
- [x] 1.3 Resolve Flyway/H2 compatibility explicitly
- [x] 2.1 Create `V1__baseline_current_schema.sql`
- [x] 2.2 Create `V2__ensure_courses_school_year.sql`
- [x] 2.3 Delete `V1__add_courses_school_year.sql`
- [x] 2.4 Replace runtime schema creation in `Database.kt` with Flyway migration startup
- [x] 3.1 Rewrite the legacy `school_year` backfill test to validate Flyway idempotency
- [x] 3.2 Verify `ServerIntegrationTest.kt` already initializes databases through `DatabaseFactory.init()`
- [x] 3.4 Add a focused migration-failure startup test
- [x] 3.3 Run `./gradlew :server:test`
- [x] 4.1 Document the migration drift/checksum rule in `openspec/backlog.md`
- [x] 4.2 Remove stale references to the old runtime schema workaround

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `gradle/libs.versions.toml` | Modified | Added Flyway version catalog entries and library aliases. |
| `server/build.gradle.kts` | Modified | Added Flyway runtime dependencies and documented why H2 test support remains explicit through `flyway-core` + `libs.h2` instead of a non-existent H2 Flyway module. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Database.kt` | Modified | Replaced Exposed schema creation with Flyway migration execution before `Database.connect()`. |
| `server/src/main/resources/db/migration/V1__baseline_current_schema.sql` | Created | Added the full baseline schema matching current Exposed tables and clarified that `courses.creator_id` intentionally stays non-FK. |
| `server/src/main/resources/db/migration/V2__ensure_courses_school_year.sql` | Created | Added the guarded compatibility migration for legacy databases missing `courses.school_year`. |
| `server/src/main/resources/db/migration/V1__add_courses_school_year.sql` | Deleted | Removed the obsolete manual V1 migration stub that conflicts with Flyway versioning. |
| `server/src/test/kotlin/com/example/proyectofinal/ServiceLayerTest.kt` | Modified | Reworked the legacy-schema migration test, aligned legacy index ordering with the baseline, and added a focused failure-path migration test. |
| `openspec/backlog.md` | Modified | Documented the rule that future server schema changes need matching Flyway scripts. |
| `openspec/changes/versioned-db-migrations/tasks.md` | Modified | Marked all implementation tasks complete. |

## Verification

- Command: `./gradlew :server:test`
- Result: Passed

## Deviations from Design

- Kept Flyway `10.22.0` plus `flyway-database-postgresql`, and documented the H2 test path explicitly after verifying Maven Central has no `org.flywaydb:flyway-database-h2` artifact for this Flyway line.

## Issues Found

- The first baseline draft incorrectly added a foreign key from `courses.creator_id` to `users.id`. Integration tests proved the current schema intentionally treats `creatorId` as a plain value, so the foreign key was removed to match `Tables.kt` and preserve existing behavior.

## Remaining Tasks

- None.

## Workload / PR Boundary

- Mode: single PR
- Current work unit: Unit 1
- Boundary: Flyway dependency setup, migration SQL, startup wiring, tests, and backlog documentation for the server migration slice only
- Estimated review budget impact: Stayed within the planned low-risk single-PR scope

## Status

12/12 tasks complete. Ready for verify.
