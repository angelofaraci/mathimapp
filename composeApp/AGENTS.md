# AGENTS.md

Local guidance for work inside `composeApp/`.

## Scope

This module owns the app-side implementation:

- Compose UI
- app-side repositories
- Ktor client usage
- SQLDelight local persistence
- KMP source-set organization

## Source Set Rule

Prefer the narrowest correct source set:

- `commonMain` for shared app logic and UI
- `androidMain` for Android-only integrations
- `iosMain` for iOS-specific Kotlin code
- `jvmMain` for JVM-specific app/runtime behavior

Do not move code to platform-specific source sets unless it truly depends on platform APIs.

## Shared Boundary

`shared` is for shared contracts only.

Keep in `composeApp`:

- Compose screens and state
- repositories and app orchestration
- SQLDelight schema and local database code
- Ktor client setup
- app-specific interfaces

Do not put these in `shared`:

- Compose UI
- SQLDelight drivers or schema
- app repository implementations
- Ktor client engine wiring

## Networking Rule

Client API code should not depend on hidden defaults when request behavior matters.

If a request sends JSON, be explicit when appropriate:

- set JSON body intentionally
- keep serialization aligned with `shared`
- avoid brittle coupling between production defaults and tests

## Testing Rule

Shared app tests currently run across:

- `commonTest`
- `androidUnitTest`
- `jvmTest`

If `commonTest` declares an `expect`, every relevant target must provide an `actual`.

Useful commands:

```powershell
.\gradlew.bat :composeApp:jvmTest
.\gradlew.bat :composeApp:androidUnitTest
.\gradlew.bat :composeApp:assembleDebug
```

## SQLDelight Rule

Keep local persistence concerns inside `composeApp`.

Typical write areas:

- `composeApp/src/commonMain/sqldelight/**`
- repository/data source classes

When schema changes:

1. update queries/schema
2. update repository usage
3. verify affected tests

## Avoid

- duplicating shared models already in `shared`
- placing Android-only code in `commonMain`
- moving app orchestration into `shared`
- changing API contracts in the app without checking backend impact
