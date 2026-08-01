# Proposal: Scope Learner Profile to the Authenticated User

## Intent

`LearnerProfileEntity` is a single global row (`profileId CHECK (profileId = 1)`), unlike every other local table, which is keyed by `userId`. Whichever account last completed onboarding on a device leaves `onboardingComplete = true` in that shared row, so any other account — including a brand-new registration — that logs in on the same device has its onboarding wizard silently skipped by `AuthGate`. New users land in a course list filtered by a stranger's province and school year. Shared/family/classroom devices are the normal case for this app, so this is a correctness bug, not an edge case.

## Scope

### In Scope
- Re-key `LearnerProfileEntity` on `userId TEXT NOT NULL PRIMARY KEY` (the `profileId INTEGER ... CHECK (profileId = 1)` single-row constraint is removed entirely); update `.sq` queries
- Table-rebuild repair in `di/LocalDatabaseSchemaFixes.kt`, following the existing `ensureLessonEntityShape()` precedent (`PRAGMA table_info` guard → rebuild → copy rows). An additive `ALTER TABLE ADD COLUMN` was considered but does not work here: SQLite cannot drop a `PRIMARY KEY`/`CHECK` via `ALTER`, so it would leave the old constraint in place and the bug would resurface as an insert-time crash for the second user on an upgraded device.
- Make the **entire** `LearnerProfileRepository` interface `userId`-scoped (see Decision below)
- Inject `AuthRepository` into `OnboardingViewModel`; pass the session user id from `AuthGate`
- Update Koin wiring and affected tests

### Out of Scope
- **`TokenStore` session persistence.** `InMemoryTokenStore` is the only implementation, so every cold restart already routes to LOGIN/REGISTER without calling `logout()`. This amplifies how often the bug is seen but is a separate, pre-existing limitation (archived `2026-06-28-onboarding-school-year` exploration, Risk #4). Not fixed here.
- Server-side profile sync (server has no learner-profile concept)
- The three other known onboarding issues tracked in `openspec/backlog.md`: Spanish localization, step order (province/year/track), logout button visible during onboarding
- Wiring the dead `clearProfile` query (superseded by scoping)

## Decision: how far does `userId`-scoping reach?

**All repository methods, not just the gate-relevant ones.** `isOnboardingComplete`, `getProfile`, and `upsertProfile` are the ones that fix the bug; downstream display consumers (`ProfileViewModel`, `HomeDashboardViewModel`, `CourseViewModel`) read post-gate and are technically already safe. We still scope them because a half-scoped interface leaves two contradictory read semantics on one repository and invites the same bug back through the next caller. The cost is near zero: those ViewModels already inject `AuthRepository`. Design must not re-litigate this.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `learner-profile`: replace "at most one active profile row" with "at most one row per `userId`"; profile writes, completion checks, and retrieval MUST be scoped to the authenticated user.
- `onboarding-flow`: the mandatory onboarding gate MUST evaluate completion for the currently authenticated user, so a different account on the same device is never treated as onboarded.

## Approach

Exploration Approach 1. Purely additive local-schema change:

1. `.sq`: add `userId` to the table; rewrite `selectProfile`/`upsertProfile`/`isOnboardingComplete`/`clearProfile` to filter by `userId`.
2. `LocalDatabaseSchemaFixes.kt`: `PRAGMA table_info` check, then `ALTER TABLE LearnerProfileEntity ADD COLUMN userId` for existing installs.
3. Domain interface + `SqlDelightLearnerProfileRepository`: every method takes `userId`.
4. `AuthGate` passes `session.user?.id` (already in scope, currently unused) into the completion check; no session id means no profile, so onboarding runs.
5. `OnboardingViewModel` gains an `AuthRepository` constructor dependency and writes the profile under the current user id.

Pre-existing global rows are migrated with a `NOT NULL DEFAULT ''` sentinel, which matches no real user id and therefore reads as "not onboarded" for everyone. This is deliberate: one forced onboarding replay is correct behavior, since the row's true owner is unknowable.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq` | Modified | `userId` column + scoped queries (lines 65-71, 153-163) |
| `composeApp/src/commonMain/kotlin/.../di/LocalDatabaseSchemaFixes.kt` | Modified | Additive `ALTER TABLE` repair |
| `composeApp/src/commonMain/kotlin/.../domain/LearnerProfileRepository.kt` | Modified | `userId` parameter on all methods |
| `composeApp/src/commonMain/kotlin/.../data/SqlDelightLearnerProfileRepository.kt` | Modified | Scoped implementations |
| `composeApp/src/commonMain/kotlin/.../App.kt` | Modified | `AuthGate` passes `session.user?.id` |
| `composeApp/src/commonMain/kotlin/.../ui/OnboardingViewModel.kt` | Modified | New `AuthRepository` dependency |
| `composeApp/src/commonMain/kotlin/.../ui/{Profile,HomeDashboard,Course}ViewModel.kt` | Modified | Pass current user id to profile reads |
| `composeApp/src/commonMain/kotlin/.../di/AppModule.kt` | Modified | Koin wiring for new dependency |
| `composeApp/src/commonTest,jvmTest/...` | Modified | Repository, `AuthGateViewModel`, `OnboardingViewModel` tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Existing installs replay onboarding once | High (intended) | Named explicitly; correct behavior — the stale row's owner is unknown |
| `ALTER TABLE` repair misfires on a fresh install | Low | `PRAGMA table_info` guard, same as the three existing repairs |
| Missing/blank session id at gate time | Medium | Treat as "not onboarded" — fail toward showing onboarding, never toward skipping it |
| Test fixture churn from the new `AuthRepository` dependency | Medium | Sibling ViewModels already have this shape; reuse their fakes |
| `TokenStore` non-persistence keeps forcing re-login | High (pre-existing) | Out of scope; cross-referenced so it is not rediscovered as a regression of this change |

## Rollback Plan

1. Revert the change in one commit, including the `.sq` file. Because the fix rebuilds the table (dropping `profileId`), a Kotlin-only revert would leave reverted `SELECT`/`INSERT` statements pointing at a column that no longer exists — the `.sq` revert and the repair function must go back together. No data is lost (rows carry over through the same rebuild style, just in the opposite direction).
2. If a partial hotfix is needed, keep the schema/repository scoping and revert only the ViewModel wiring, leaving `AuthGate` on the unscoped read.

## Dependencies

- SQLDelight 2.3.1 (present); no `.sqm` migration harness exists project-wide, hence the `LocalDatabaseSchemaFixes.kt` pattern
- Existing `kotlin.test` suite (`./gradlew :composeApp:jvmTest`)

## Success Criteria

- [ ] Account A completes onboarding; account B logging in on the same device sees the full onboarding wizard
- [ ] Account A logging back in on that device does **not** repeat onboarding
- [ ] A fresh registration on a device with a prior completed profile always runs onboarding
- [ ] Course filtering, dashboard, and profile screens show the authenticated user's own province/school year
- [ ] Existing installs upgrade without a crash or data wipe (one onboarding replay is expected)
- [ ] `./gradlew :composeApp:jvmTest` passes
- [ ] `./gradlew :composeApp:assembleDebug` passes
