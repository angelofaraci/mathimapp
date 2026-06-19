# AGENTS.md

Guide for agents and collaborators working in this monorepo.

## Repository Purpose

This project is a Kotlin Multiplatform monorepo with three main areas:

- `composeApp/`: Compose Multiplatform app. Contains shared UI, HTTP client code, common logic, and the SQLDelight schema.
- `shared/`: cross-module shared models and serialization contracts used by app and backend.
- `server/`: JVM backend using Ktor, Exposed, and PostgreSQL.
- `iosApp/`: native iOS entrypoint in Swift/Xcode.

The root Gradle project mainly orchestrates modules. Most functional decisions should live inside `composeApp`, `shared`, or `server`, depending on ownership.

## General Rule

Before editing, identify whether the change is:

- frontend/shared app only
- backend only
- app/server integration
- iOS bootstrap or native wiring

Do not mix responsibilities unnecessarily. If a change is local to one module, keep it inside that module.

## Area Ownership

### 1. Shared App / Compose Agent

Responsible for:

- `composeApp/src/commonMain/**`
- `composeApp/src/androidMain/**`
- `composeApp/src/iosMain/**`
- `composeApp/src/jvmMain/**`
- `composeApp/src/commonMain/composeResources/**`
- `composeApp/src/commonMain/sqldelight/**`

Should handle:

- screens, navigation, and UI state
- models consumed by the app
- Ktor client code and client-side serialization
- local persistence with SQLDelight
- `commonTest` and `androidUnitTest` when the change affects that layer

Should not:

- move server business logic into the UI layer
- change HTTP routes or contracts without syncing with the backend
- introduce backend dependencies into mobile code beyond what is already exposed through multiplatform

### 2. Backend / API Agent

Responsible for:

- `server/src/main/kotlin/**`
- `server/src/main/resources/**`

Should handle:

- Ktor routes
- authentication and JWT
- data access with Exposed
- server configuration
- seed data and backend startup logic

Should not:

- edit Compose screens unless the change is intentionally cross-module and coordinated

### 3. Shared Contracts Agent

Responsible for:

- `shared/src/commonMain/**`
- `shared/src/androidMain/**`
- `shared/src/jvmMain/**`
- `shared/src/iosMain/**`

Should handle:

- cross-module serializable models
- enums and DTOs shared by app and backend
- contract-level changes that must stay platform-agnostic

Should not:

- contain Compose UI code
- contain SQLDelight database code
- contain Ktor client wiring or server routing logic
- become a general dumping ground for unrelated utilities

### 4. iOS Bootstrap Agent

Responsible for:

- `iosApp/iosApp/**`
- `iosApp/Configuration/**`
- `iosApp/*.xcodeproj/**`

Should handle:

- SwiftUI entrypoint
- Xcode configuration
- iOS-specific assets or native wiring

Should not:

- duplicate logic that already exists in `composeApp`

## Cross-Module Changes

If a feature touches both client and backend:

1. define the data contract first
2. update `shared` if the contract itself changes
3. update backend and serialization
4. update the KMP client
4. validate local persistence if relevant
5. validate Android/iOS impact if the change touches platform behavior

When several modules must be edited, prefer PRs or commits that stay easy to review in slices:

- contract/models
- backend
- client/UI

## Current Technical Risk

Right now both `composeApp` and `server` depend on `project(":shared")`.

That means:

- the old app-to-backend coupling through `composeApp` has been removed
- the main architectural risk is allowing `shared` to accumulate framework-specific code
- any model used by both app and backend should live in `shared`, but implementation details should stay in their owning module

Keep `shared` focused on contracts. Do not move app logic, persistence code, UI code, or server infrastructure there unless the task explicitly calls for a broader architectural change.

## Where To Make Each Change

Use this as a quick mental map:

- new screen or UI state -> `composeApp/src/commonMain`
- Android-specific adjustment -> `composeApp/src/androidMain`
- iOS-specific Kotlin adjustment -> `composeApp/src/iosMain`
- iOS Swift/Xcode adjustment -> `iosApp`
- shared request/response model or enum -> `shared/src/commonMain`
- new endpoint or auth change -> `server`
- local SQLDelight schema change -> `composeApp/src/commonMain/sqldelight`

## Useful Commands

From the repository root:

```powershell
.\gradlew.bat :composeApp:assembleDebug
.\gradlew.bat :composeApp:jvmTest
.\gradlew.bat :composeApp:androidUnitTest
.\gradlew.bat :server:run
.\gradlew.bat :server:test
.\gradlew.bat build
```

If only one module changed, run the smallest relevant verification before falling back to `build`.

## Verification Rule

Each agent should validate based on scope:

- UI/shared: compile `composeApp` and run `commonTest` if logic changed
- backend: run `:server:test` or at least compile/start `:server`
- cross-module: validate backend first, then the app

If something cannot be run, state it explicitly.

Every change should preserve a healthy Gradle project state:

- the project should still sync/configure correctly
- the affected modules should still build correctly

If a change touches Gradle files, dependencies, or module structure, explicitly verify Gradle configuration and at least the smallest relevant build/test task.

## Collaboration Conventions

- prefer small, localized changes
- do not move files around in bulk unless necessary
- do not add new dependencies without a clear reason and scope
- document structural TODOs only when they add real context
- respect unrelated code already modified by someone else
- keep `shared` platform-agnostic and minimal
- when adding or changing Gradle dependencies, use the version catalog in `gradle/libs.versions.toml` instead of hardcoded versions in module build files

## When To Split Work Across Agents

It makes sense to split agents when the work is independent:

- agent A: `server`
- agent B: `composeApp`
- agent C: `shared`
- agent D: `iosApp` only if native wiring is involved

Avoid parallelizing:

- changes in the same package or the same models
- structural refactors of shared contracts

## Definition Of Done

A change is ready when:

- it modifies the correct module
- it does not leave accidental debt between `composeApp`, `shared`, and `server`
- it keeps data contracts aligned on both sides
- it leaves behind at least minimal verification, either executed or explicitly noted as pending
