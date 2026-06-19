# AGENTS.md

Local guidance for work inside `server/`.

## Scope

This module owns the JVM backend:

- Ktor routing
- JWT auth
- Exposed database access
- seed/startup behavior
- backend integration tests

## What Belongs Here

Keep these concerns in `server`:

- route handlers
- auth and security setup
- database schema and persistence logic
- server-only request/response models
- backend-only tests and test fixtures

Do not move these into `shared`:

- Exposed tables
- route logic
- auth logic
- Ktor server configuration
- backend test helpers

## Shared Boundary

Use `shared` for models that are genuinely consumed by both app and backend.

Keep in `server`:

- request payloads that are only meaningful on the server
- internal persistence representations
- infrastructure-oriented helpers

If a type is reused by both `composeApp` and `server`, prefer moving it to `shared`.

## Database Rule

Exposed queries must run inside a transaction context.

Use the local helper:

- `com.example.proyectofinal.database.dbQuery`

This applies to:

- `SELECT`
- `INSERT`
- `UPDATE`
- `DELETE`

Do not assume reads are safe outside a transaction when using Exposed DSL.

## Testing Rule

Prefer backend integration tests over narrow mocks when behavior spans:

- routing
- auth
- serialization
- persistence

Current backend tests use:

- Ktor `testApplication`
- in-memory H2
- configurable `module(initDatabase, seedData)`

Useful command:

```powershell
.\gradlew.bat :server:test
```

## When Editing Routes

For route changes:

1. validate auth requirements
2. validate status codes
3. validate serialization shape
4. validate persistence side effects
5. add or update a backend test when behavior changes

## Avoid

- querying Exposed outside `dbQuery`
- putting cross-module contracts back into `composeApp`
- moving app concerns into backend models
- leaving route behavior untested after changing persistence or auth
