# Proposal: Versioned Database Migrations

## Intent

Replace the server's runtime `SchemaUtils.create(...)` and inline `ALTER TABLE` workaround with Flyway-based versioned migrations. This closes the persistent-deployment gap where schema changes today risk data loss or manual operator steps.

## Scope

### In Scope
- Add Flyway dependency to `server` and version catalog.
- Generate a baseline V1 migration representing the current PostgreSQL schema (including `courses.school_year`).
- Replace `SchemaUtils.create(...)` and inline `ALTER TABLE` in `DatabaseFactory.init()` with programmatic Flyway migration on startup.
- Verify `:server:test` still passes (H2 compatibility).

### Out of Scope
- SQLDelight app-side migrations (local data is derived; deferred slice).
- Legacy `LEARNER` → `STUDENT` data rewrite (gated on migration system; deferred slice).
- Removal of `UserRole.parse("LEARNER")` compatibility (deferred with data rewrite).
- New schema changes (e.g., `province`, `Topic` split) not yet in `Tables.kt`.

## Capabilities

### New Capabilities
- `server-db-migrations`: Flyway-powered versioned migration lifecycle for the server PostgreSQL schema, including baseline generation and startup invocation.

### Modified Capabilities
- None (this is pure infrastructure; no spec-level behavior changes).

## Approach

Use Flyway programmatic API in `DatabaseFactory.init()` before Exposed connects. Store SQL scripts in `server/src/main/resources/db/migration/`. Baseline V1 captures the current schema exactly; existing deployments use `baselineOnMigrate` or `IF NOT EXISTS` guards to avoid conflicts with the prior inline `ALTER TABLE`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `server/build.gradle.kts` | Modified | Add Flyway dependency. |
| `gradle/libs.versions.toml` | Modified | Add Flyway version catalog entry. |
| `server/src/main/kotlin/.../database/Database.kt` | Modified | Replace `SchemaUtils.create` + inline ALTER with Flyway migrate call. |
| `server/src/main/resources/db/migration/` | New | V1 baseline script + future migration home. |
| `server/src/test/...` | Modified | Ensure H2 in-memory still initializes correctly via Flyway. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| H2/PostgreSQL dialect mismatch in baseline SQL | Low | Use standard ANSI SQL; avoid PG-specific types; validate via `:server:test`. |
| Schema drift between `Tables.kt` and SQL scripts | Med | Document rule: every schema change needs a Flyway script; add CI check later. |
| Existing deployments with inline `school_year` ALTER | Low | Baseline script uses `IF NOT EXISTS` or Flyway `baselineOnMigrate` strategy. |

## Rollback Plan

1. Revert the PR: remove Flyway dependency and restore `SchemaUtils.create(...)` + inline `ALTER TABLE`.
2. Flyway history table (`flyway_schema_history`) remains harmless; no data is migrated or dropped in this slice.
3. Re-deploy; existing tables are untouched because `SchemaUtils.create` is idempotent.

## Dependencies

- None external beyond Maven Central (Flyway).

## Success Criteria

- [ ] `server` starts and runs Flyway migrate successfully against both H2 (tests) and PostgreSQL (prod-like).
- [ ] `:server:test` passes with no changes to test logic.
- [ ] No `SchemaUtils.create(...)` or inline `ALTER TABLE` remains in `DatabaseFactory.init()`.
- [ ] A V1 baseline migration script exists and accurately represents the current schema.
