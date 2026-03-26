# AGENTS.md

Local guidance for work inside `shared/`.

## Scope

This module exists to hold cross-module contracts shared by:

- `composeApp`
- `server`

Its job is narrow by design.

## What Belongs Here

Good candidates for `shared`:

- `@Serializable` models used on both sides
- shared enums
- shared response/request shapes when both client and server truly consume them

## What Does Not Belong Here

Do not place these in `shared`:

- Compose UI
- SQLDelight schema or drivers
- Exposed tables
- route handlers
- Ktor client/server setup
- repository implementations
- platform-specific utilities

## Design Rule

Keep `shared`:

- minimal
- platform-agnostic
- framework-light
- focused on contracts

If a type is only used by the backend, keep it in `server`.

If a type is only used by the app, keep it in `composeApp`.

## Change Rule

When editing `shared`:

1. confirm the type is truly cross-module
2. keep naming and serialization stable
3. check impact on both `composeApp` and `server`
4. verify both modules after the change

Useful commands:

```powershell
.\gradlew.bat :server:test
.\gradlew.bat :composeApp:jvmTest
.\gradlew.bat build
```

## Avoid

- turning `shared` into a misc utilities module
- moving implementation code here for convenience
- adding dependencies that pull in app or server frameworks
