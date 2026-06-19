# SKILLS.md

Suggested skills for working effectively in this monorepo. These are not a language or build feature. They are reusable playbooks for humans or agents.

## How To Use This File

Each skill defines:

- when to use it
- which areas of the repo it may touch
- recommended steps
- minimum checks before closing the task

## Skill: `kmp-ui-feature`

Use when:

- there is a new screen
- navigation, UI state, or presentation changes
- shared logic used by Android/iOS/Desktop is being updated

Write scope:

- `composeApp/src/commonMain/**`
- optionally `composeApp/src/androidMain/**`, `composeApp/src/iosMain/**`, `composeApp/src/jvmMain/**`

Checklist:

1. locate the affected screen, state holder, and models
2. keep as much as possible in `commonMain`
3. move code to a platform-specific source set only when it truly depends on that platform
4. review impact on shared resources and serialization
5. run module compilation or tests

Avoid:

- placing Android-specific code in `commonMain`
- duplicating UI by platform without a clear reason

## Skill: `sqlDelight-change`

Use when:

- local persistence changes
- the SQLDelight schema or queries change
- a feature needs cache or offline storage

Write scope:

- `composeApp/src/commonMain/sqldelight/**`
- Kotlin data access files inside `composeApp`

Checklist:

1. update the `.sq` files and review generated names/contracts
2. adjust consuming repositories or data sources
3. review test impact
4. validate module compilation

Avoid:

- mixing local schema concerns with backend schema concerns
- renaming tables or queries without reviewing all usages

## Skill: `ktor-backend-feature`

Use when:

- there are new endpoints or route changes
- authentication, JWT, or middleware changes
- server-side business logic is being added or updated

Write scope:

- `server/src/main/kotlin/**`
- `server/src/main/resources/**`

Checklist:

1. locate the affected route, service, and data access code
2. keep serialization aligned with the client
3. review HTTP errors, auth behavior, and validations
4. run `:server:test` or at least compile/start the server

Avoid:

- exposing persistence entities directly as external contracts
- duplicating DTOs if a valid shared contract already exists

## Skill: `api-contract-sync`

Use when:

- a request/response used by both app and backend changes
- drift appears between client and server
- a feature requires changes on both sides

Write scope:

- `server/**`
- `shared/**`
- `composeApp/**`

Checklist:

1. define the target contract first
2. move or update shared models in `shared` when they belong on both sides
3. update the backend
4. update the Ktor client and app models
5. review JSON parsing, nullability, and defaults
6. verify both modules

Avoid:

- closing the change after validating only one side
- doing large architectural refactors when the task is only about sync

## Skill: `ios-entrypoint-adjustment`

Use when:

- the change is specific to SwiftUI/Xcode
- iOS assets or configuration must be adjusted
- a new native capability must be wired in

Write scope:

- `iosApp/**`

Checklist:

1. confirm the change cannot be solved inside `composeApp`
2. limit the change to the native entrypoint or configuration
3. review touched target names, plist values, and assets

Avoid:

- duplicating domain logic in Swift

## Skill: `monorepo-safe-refactor`

Use when:

- there is a cross-cutting refactor
- shared code should be extracted
- structural debt appears between `composeApp`, `shared`, and `server`

Write scope:

- potentially multiple modules, but in stages

Checklist:

1. identify the real ownership of each piece
2. separate mechanical changes from functional changes
3. keep `shared` focused on contracts, not implementation details
4. keep the refactor in reviewable slices
5. validate module by module

Avoid:

- moving contracts, UI, and backend in one opaque diff
- turning `shared` into a mixed app/server utility module

## Skill: `bugfix-minimal-surface`

Use when:

- there is a scoped bug
- the fix should touch as few files as possible

Checklist:

1. reproduce or identify the affected layer
2. fix it in the module closest to the issue
3. add or adjust a test if that area already has coverage
4. verify the impacted surface first, then widen verification only if needed

Avoid:

- turning a focused bugfix into a general refactor

## Recommendation For This Repo

If you want to keep only one document, `AGENTS.md` gives you the most value first because it clarifies ownership and coordination in the monorepo.

If you want a second layer of maturity, `SKILLS.md` complements it well by standardizing how repeated tasks should be approached without reinventing the process every time.
