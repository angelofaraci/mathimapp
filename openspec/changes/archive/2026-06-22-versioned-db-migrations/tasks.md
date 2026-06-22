# Tasks: Versioned Database Migrations

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~120 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Flyway deps + migration SQL + Database.kt rewrite + test fixes + docs | PR 1 | Single PR — fits within 400-line budget. |

## Phase 1: Foundation

- [x] 1.1 Add Flyway version `10.22.0` and `flyway-core` + `flyway-database-postgresql` library aliases to `gradle/libs.versions.toml`
- [x] 1.2 Add `implementation(libs.flyway.core)` and `implementation(libs.flyway.database.postgresql)` to `server/build.gradle.kts`; keep `libs.h2` as `testImplementation`
- [x] 1.3 **Resolve Flyway H2**: keep Flyway `10.22.0`, verify no published `flyway-database-h2` artifact exists for this line, and document H2 coverage explicitly instead of leaving support implicit

## Phase 2: Core Implementation

- [x] 2.1 Create `V1__baseline_current_schema.sql` — full current schema (all tables + columns from `Tables.kt`) using H2/PostgreSQL-compatible SQL
- [x] 2.2 Create `V2__ensure_courses_school_year.sql` — guarded `ALTER TABLE courses ADD COLUMN IF NOT EXISTS school_year INTEGER NOT NULL DEFAULT 0` for existing non-baselined databases
- [x] 2.3 Delete `V1__add_courses_school_year.sql` (old manual reference script — conflicts with Flyway V1)
- [x] 2.4 Replace `SchemaUtils.create(...)` + inline `ALTER TABLE` in `Database.kt` with `Flyway.configure(...).load().migrate()` before Exposed `Database.connect()`

## Phase 3: Testing

- [x] 3.1 Update `ServiceLayerTest.kt` — rewrite `database init backfills missing course school year column` test to pre-create schema via raw SQL and verify two `DatabaseFactory.init()` calls run idempotently through Flyway
- [x] 3.2 Verify `ServerIntegrationTest.kt` — all tests call `setupTestDatabase()` → `DatabaseFactory.init()`; confirm no route-test logic changes are required beyond the explicit Flyway/H2 support decision
- [x] 3.3 Run `:server:test` — assert all tests pass with zero regressions
- [x] 3.4 Add a focused server test showing a migration failure aborts startup initialization before routes can rely on schema readiness

## Phase 4: Cleanup / Documentation

- [x] 4.1 Document checksum/drift rule in `openspec/backlog.md`: every future server schema change needs a corresponding Flyway migration script; CI validation deferred
- [x] 4.2 Remove stale comments referencing old `SchemaUtils.create` or inline `ALTER TABLE` approach in any server source file
