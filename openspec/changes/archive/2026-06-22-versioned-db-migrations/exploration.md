# Exploration: Versioned Database Migrations

## Current State

### Server (PostgreSQL + Exposed)
- `server/src/main/kotlin/.../database/Database.kt` initializes the connection and calls `SchemaUtils.create(...)` for every table inside a transaction.
- `SchemaUtils.create` is idempotent but **not** a migration strategy: it only creates missing tables; it does not apply additive or destructive schema changes to existing tables safely in persistent environments.
- An inline `ALTER TABLE courses ADD COLUMN IF NOT EXISTS school_year` is currently hard-coded in `DatabaseFactory.init()` as a workaround for the missing migration system.
- A manual reference script exists at `server/src/main/resources/db/migration/V1__add_courses_school_year.sql`, but it is **not executed automatically** by Gradle or the server. Comments in the file explicitly state it is for manual/operator-run deployments only.
- Tests use in-memory H2 (`MODE=PostgreSQL`) and call `DatabaseFactory.init()` directly, so they are unaffected by the migration gap today.
- Exposed version is `1.1.1`. There is no `exposed-migrations` or similar module on the classpath.

### App (SQLDelight + SQLite)
- `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq` defines the local schema.
- No `.sqm` migration files exist. SQLDelight is configured with a single database (`AppDatabase`) but no explicit schema version or migration folder.
- Local data is largely derived (course cache, progress, enrollments). In a development context, schema changes are currently handled by reinstalling the app.

### Deferred Debt from `role-naming-cleanup`
- The archived `role-naming-cleanup` slice intentionally deferred:
  1. Full DB migration rewriting existing `LEARNER` rows to `STUDENT`.
  2. Dropping the legacy `UserRole.parse("LEARNER")` compatibility mapping.
- Both items are gated on the existence of a versioned migration system.

### Known Schema Gaps
- `courses.school_year` — already present in `Tables.kt` and SQLDelight, but the persistent PostgreSQL migration path is the inline `ALTER TABLE` hack.
- Future columns (e.g., `province`, `Topic` split) are on the backlog but not yet in schema.

## Affected Areas
- `server/src/main/kotlin/com/example/proyectofinal/database/Database.kt` — remove/replace `SchemaUtils.create` and inline ALTER TABLE.
- `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` — becomes the schema source of truth for baseline migration authoring.
- `server/build.gradle.kts` — add migration tooling dependency.
- `gradle/libs.versions.toml` — add version catalog entry for migration library.
- `server/src/test/...` — verify that tests still pass with new initialization path (H2 compatibility).
- `server/src/main/resources/db/migration/` — becomes the home for versioned SQL scripts (if Flyway/Liquibase chosen).
- `openspec/backlog.md` — can remove or mark the backlog item once a strategy is chosen.

## Approaches

### 1. Flyway (server only)
Replace `SchemaUtils.create(...)` with Flyway programmatic migration on startup. Maintain SQL migration files in `server/src/main/resources/db/migration/`.

- **Pros**
  - De-facto industry standard for JVM/PostgreSQL migrations.
  - Simple SQL-based migrations; easy to review in PRs.
  - Excellent H2 and PostgreSQL support for both production and test environments.
  - Gradle plugin available for validation/ baseline generation.
  - Small dependency footprint (~2MB).
- **Cons**
  - Requires converting the current schema into a baseline migration script.
  - `SchemaUtils.create` currently auto-creates tables from Exposed DSL; moving to SQL means keeping SQL scripts in sync with `Tables.kt`.
  - Does not solve SQLDelight app-side migrations.
- **Effort**: Low–Medium

### 2. Liquibase (server only)
Use Liquibase with YAML/XML or SQL changelogs, invoked programmatically on startup.

- **Pros**
  - Powerful rollback and conditional changeset support.
  - Cross-database abstraction (useful if the project ever switches away from PostgreSQL).
- **Cons**
  - Heavier dependency and configuration overhead than Flyway.
  - YAML/XML changelogs are harder to review than plain SQL.
  - Overkill for a project with a single PostgreSQL deployment target and a simple schema.
- **Effort**: Medium

### 3. Exposed-native DDL / Manual Migration Tracker (server only)
Keep `SchemaUtils.create(...)` but add a hand-rolled `migrations` table and a Kotlin-driven migration runner that executes ordered Kotlin functions.

- **Pros**
  - Zero new dependencies.
  - Keeps migrations in Kotlin, aligned with `Tables.kt`.
- **Cons**
  - Reinventing Flyway/Liquibase. Error-prone (checksums, locking, baseline handling).
  - `SchemaUtils.create` behavior with existing tables is limited; it won't handle complex migrations.
  - Not a well-trodden path in production.
- **Effort**: Medium–High

### 4. SQLDelight Migrations (app only)
Add `.sqm` migration files and bump the SQLDelight schema version to handle local DB upgrades.

- **Pros**
  - Native SQLDelight mechanism; no new dependencies.
  - Required before any app store distribution where local data must survive updates.
- **Cons**
  - Does not solve the server persistent-deployment gap, which is the immediate blocker.
  - Current local data is derived; reinstall is acceptable in MVP.
- **Effort**: Low

### 5. Both Server + App in One Slice
Introduce Flyway for the server and SQLDelight `.sqm` files for the app simultaneously.

- **Pros**
  - Covers all migration surfaces in one change.
- **Cons**
  - Larger review surface; risk of exceeding 400-line budget.
  - App-side migrations are not urgent (data is derived and can be recreated from server sync).
  - Mixes two unrelated toolchains in one PR.
- **Effort**: Medium

## Recommendation

**Adopt Approach 1 (Flyway, server only) as the first slice.**

Reasoning:
1. The primary risk is persistent PostgreSQL data loss/corruption on schema changes. Flyway directly addresses this.
2. The current inline `ALTER TABLE` in `DatabaseFactory.init()` is technical debt that should be replaced with a proper V2 migration.
3. SQLDelight migrations are not urgent — the app's local database is a cache of server data. A follow-up slice can handle `.sqm` files before app store distribution.
4. Flyway is the simplest, most reviewable, and most standard choice for this stack.

### Proposed First Slice Boundary (reviewable under 400 lines)
1. Add Flyway dependency to `server/build.gradle.kts` and version catalog.
2. Generate a baseline V1 migration script representing the current schema (all tables as they exist today, including `school_year` on `courses`).
3. Remove `SchemaUtils.create(...)` and the inline `ALTER TABLE` from `DatabaseFactory.init()`.
4. Invoke Flyway migration programmatically in `DatabaseFactory.init()` before application code runs.
5. Add a `V2__migrate_role_names.sql` **only if** the team decides to resolve the `role-naming-cleanup` deferred debt in this slice; otherwise defer role value migration to a follow-up slice.
6. Verify `:server:test` still passes (H2 compatibility).

**Decision point for the orchestrator/user:** Should this slice also migrate existing `LEARNER` rows to `STUDENT` and drop the legacy parser? Doing so adds ~50–100 lines (one SQL migration + parser removal across modules) but stays within budget. Keeping it out keeps the slice purely infrastructure-focused.

## Risks

- **H2 / PostgreSQL dialect mismatch in Flyway SQL**: The baseline migration must be valid for both PostgreSQL (production) and H2 (tests). Using standard ANSI SQL and avoiding PG-specific types minimizes this risk. Flyway supports H2 natively.
- **Schema drift between `Tables.kt` and SQL scripts**: Developers may modify Exposed DSL and forget to add a Flyway migration. Mitigation: document the rule that schema changes require a migration file, and add a CI or Gradle check later.
- **Baseline generation accuracy**: Converting the existing implicit schema into a V1 script must be exact. Missing constraints, defaults, or indexes could cause subtle bugs. Mitigation: generate the script from a fresh database dump or use `SchemaUtils.createMissingTablesAndColumns` temporarily to diff.
- **Test initialization order**: Tests call `DatabaseFactory.init()`; if Flyway is invoked there, tests must not fail due to migration checksum changes during development. Mitigation: use H2 in-memory with `clean` disabled, or ensure migrations are idempotent.
- **Existing deployments with the inline ALTER TABLE**: If a persistent database already has the `school_year` column from the current runtime hack, the baseline V1 script must account for it (e.g., `IF NOT EXISTS` or a Flyway `baselineOnMigrate` strategy).

## Ready for Proposal

**Yes.**

The orchestrator should tell the user:
- The exploration recommends **Flyway for the server only** as the first slice.
- One decision is pending: **should this slice also resolve the deferred `LEARNER` → `STUDENT` DB migration and drop the legacy parser?** If yes, the slice stays infrastructure-focused and safer to review. If no, a follow-up slice can handle role data cleanup.
- The expected line count is ~100–200 lines for Flyway setup + baseline migration, well under the 400-line budget.
- SQLDelight app-side migrations are explicitly deferred to a later slice because local data is derived and can be recreated.
