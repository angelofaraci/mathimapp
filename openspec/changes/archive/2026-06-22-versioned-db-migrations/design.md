# Design: Versioned Database Migrations

## Technical Approach

Introduce Flyway in the `server` module only. `DatabaseFactory.init()` will load the configured JDBC driver, run Flyway against the same URL/user/password currently passed to Exposed, then connect Exposed. This preserves the existing `Application.module()` order: database initialization happens before plugins, routes, services, and seed data perform DB-backed work.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Migration tool | Flyway programmatic API in server startup | Liquibase; hand-rolled Exposed migration runner | Flyway is SQL-first, small, standard for PostgreSQL/JVM, and avoids reinventing checksums/history/locking. |
| Startup order | Run `Flyway.configure().dataSource(url, user, password).locations("classpath:db/migration").baselineOnMigrate(true).baselineVersion("1").load().migrate()` before `Database.connect(...)` | Run after Exposed connects; Gradle-only migration task | Running first ensures routes and `SeedData.seedOfficialCourses()` see the migrated schema. Programmatic startup matches current deployment simplicity. |
| Baseline compatibility | V1 creates the current full schema; V2 is a guarded `courses.school_year` bridge | Keep inline `ALTER`; only use `baselineOnMigrate` | Existing non-empty DBs without Flyway history are baselined at V1, so V1 is skipped. A V2 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` preserves the prior inline workaround for DBs that have not yet received `school_year`; fresh DBs already have it from V1 and V2 no-ops. |
| Scope boundary | Server Flyway only | Include SQLDelight `.sqm`; rewrite `LEARNER` rows and remove parser compatibility | SQLDelight local data is derived and uses a separate SQLite toolchain. Role-value cleanup is a data/domain migration and should follow once the server migration foundation exists. |

## Data Flow

```text
Application.module(initDatabase=true)
  -> DatabaseFactory.init()
     -> read DB_URL/DB_DRIVER/DB_USER/DB_PASSWORD
     -> Class.forName(driver)
     -> Flyway.migrate(url, user, password)
     -> Exposed Database.connect(url, driver, user, password)
  -> install Ktor plugins/routes
  -> SeedData.seedOfficialCourses()
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `gradle/libs.versions.toml` | Modify | Add `flyway` version plus `flyway-core` and `flyway-database-postgresql` aliases. PostgreSQL support requires the database module in modern Flyway. |
| `server/build.gradle.kts` | Modify | Add `implementation(libs.flyway.core)` and `implementation(libs.flyway.database.postgresql)`; keep `libs.h2` as `testImplementation`. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Database.kt` | Modify | Remove `SchemaUtils.create(...)` and inline `ALTER`; add private migration helper before Exposed connection. |
| `server/src/main/resources/db/migration/V1__add_courses_school_year.sql` | Delete/Rename | Replace the manual reference-only script because Flyway cannot have two V1 migrations. |
| `server/src/main/resources/db/migration/V1__baseline_current_schema.sql` | Create | Full current schema from `Tables.kt`, including `courses.school_year`. Use H2/PostgreSQL-compatible SQL. |
| `server/src/main/resources/db/migration/V2__ensure_courses_school_year.sql` | Create | Guarded compatibility migration for already-created DBs missing the prior inline workaround. |
| `server/src/test/kotlin/com/example/proyectofinal/*.kt` | Modify | Keep H2 URLs in PostgreSQL mode; update/add tests for fresh schema creation and legacy `school_year` backfill. |
| `README.md` or `openspec/backlog.md` | Optional | Document that server schema changes require Flyway scripts; defer CI enforcement. |

## Interfaces / Contracts

No API or shared model contract changes. Internal helper shape:

```kotlin
private fun migrate(url: String, driver: String, user: String, password: String)
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit/Integration | Fresh H2 database initializes all tables through Flyway | Existing `DatabaseFactory.init(...)` test helpers should pass with unique H2 URLs. |
| Integration | Existing H2 database without `courses.school_year` migrates safely and idempotently | Adapt current backfill test to assert two `init()` calls and value default `0`. |
| Production compatibility | PostgreSQL migration syntax | Keep SQL to shared syntax: `VARCHAR`, `INTEGER`, `BOOLEAN`, `TEXT`, `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`. |

## Migration / Rollout

Fresh databases run V1 then V2. Existing non-empty databases without `flyway_schema_history` are baselined at V1 and then run V2. Operators should back up persistent PostgreSQL data before deploy and confirm the app user can create `flyway_schema_history` and alter tables. Rollback is PR revert; data tables remain, while `flyway_schema_history` is harmless. Do not run Flyway `clean` in any environment.

## Risks and Alternatives

- Baselining the wrong database is the main operational risk because `baselineOnMigrate` removes a safety check; mitigate with explicit DB env review and backups.
- SQL drift between `Tables.kt` and migrations remains possible; document the rule now and add CI validation later.
- H2 may differ from PostgreSQL on constraints; keep SQL conservative and verify with `:server:test`.
- Liquibase was rejected as heavier than needed; a custom runner was rejected as unreviewable infrastructure debt.

## Open Questions

None. SQLDelight migrations and role-value cleanup are explicitly deferred.
