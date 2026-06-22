# server-db-migrations Specification

## Purpose

Provide server-only versioned database migrations for the PostgreSQL schema. This slice excludes SQLDelight migrations and the deferred LEARNER→STUDENT cleanup.

## Requirements

### Requirement: Startup runs versioned migrations

The server MUST apply versioned database migrations before it serves any DB-backed route or performs DB-dependent startup work.

#### Scenario: Fresh startup migrates successfully

- GIVEN a database with no applied migration history
- WHEN the server starts
- THEN the database schema is migrated successfully
- AND DB-backed routes may start serving only after migration succeeds

#### Scenario: Migration failure blocks service startup

- GIVEN a database where a migration cannot be applied
- WHEN the server starts
- THEN startup fails before DB-backed routes are served

### Requirement: Baseline matches current schema

The initial versioned baseline MUST represent the current persistent server schema, including all existing tables and columns.

#### Scenario: Fresh database contains the expected schema

- GIVEN an empty database
- WHEN the server completes startup migrations
- THEN the schema includes all current tables and columns
- AND the `courses.school_year` column exists

#### Scenario: Baseline omission is observable as a schema mismatch

- GIVEN a fresh database with a missing current column or table in the baseline
- WHEN startup migrations run
- THEN at least one current table or column is absent from the resulting schema

### Requirement: Existing `courses.school_year` databases remain compatible

The migration flow MUST accept existing persistent databases that already contain `courses.school_year` and MUST complete startup without treating that column as an error.

#### Scenario: Prior workaround database starts cleanly

- GIVEN a persistent database that already has `courses.school_year`
- WHEN the server starts
- THEN migrations complete successfully
- AND startup does not fail due to a duplicate or existing-column condition

#### Scenario: Repeated startup stays stable

- GIVEN the same persistent database after a successful migration run
- WHEN the server starts again
- THEN startup succeeds again without schema errors

### Requirement: No runtime schema creation workaround

The server MUST NOT rely on runtime schema creation or inline schema-alteration workarounds as its accepted initialization behavior.

#### Scenario: Initialization is migration-driven only

- GIVEN a server startup with database access available
- WHEN initialization runs
- THEN schema readiness is established through versioned migrations
- AND no ad hoc schema creation workaround is required

#### Scenario: Missing schema is not silently repaired outside migrations

- GIVEN a database missing required schema state
- WHEN startup begins
- THEN the server does not silently create or alter tables outside the migration flow
