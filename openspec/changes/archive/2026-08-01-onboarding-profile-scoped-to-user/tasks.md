# Tasks: Scope Learner Profile to the Authenticated User

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~520 (PR1 ~290, PR2 ~230) |
| 400-line budget risk | High (as one PR) / Low per split PR |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: schema + migration repair + repository → PR 2: AuthGate + ViewModel wiring |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

11 files across schema, table-rebuild migration, domain interface, repository impl, `App.kt`, 3 ViewModels, and 5 test files. PR2 depends on PR1's new repository signatures, so PR1 must merge first; `stacked-to-main` fits since each PR is independently mergeable and there is no need to hold a long-lived tracker branch for only two slices.

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | `userId`-scoped schema, table-rebuild repair, and repository | PR 1 | `./gradlew :composeApp:jvmTest --tests "*SqlDelightLearnerProfileRepositoryTest*" --tests "*LocalDatabaseSchemaFixesTest*"` | N/A — pure data-layer; proven by JVM SQLite driver tests, no on-device step for this slice | Revert `AppDatabase.sq` + `LocalDatabaseSchemaFixes.kt` + `LearnerProfileRepository.kt` + `SqlDelightLearnerProfileRepository.kt` + their two test files together (schema and repair function must revert as one unit per design's Migration/Rollout note) |
| 2 | `AuthGate`/ViewModel wiring on top of the scoped repository | PR 2 | `./gradlew :composeApp:jvmTest --tests "*OnboardingViewModelTest*" --tests "*ProfileViewModelTest*" --tests "*HomeDashboardViewModelTest*" --tests "*ComposeAppCommonTest*"` | Manual: two different accounts on one device/emulator, confirm per-account onboarding gating | Revert `App.kt`, `OnboardingViewModel.kt`, `CourseViewModel.kt`, `ProfileViewModel.kt`, `HomeDashboardViewModel.kt`, and their test updates; independent of PR1's schema (only depends on its merged signatures) |

## Phase 1: Schema, Migration Repair, Repository (PR 1)

- [x] 1.1 RED — Extend `SqlDelightLearnerProfileRepositoryTest`: two `userId`s hold independent profiles/`onboardingComplete`; writing A never flips B; `upsertProfile` replaces within one user only (spec: learner-profile "Different users on the same device get independent rows")
- [x] 1.2 RED — Same file: blank `userId` returns `null` from `getProfile`, `false` from `isOnboardingComplete`, no-op on `upsertProfile`
- [x] 1.3 GREEN — Modify `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq`: re-key `LearnerProfileEntity` on `userId TEXT NOT NULL PRIMARY KEY`, drop `profileId`/`CHECK`, scope `selectProfile`/`upsertProfile`/`clearProfile` by `userId`
- [x] 1.4 RED — Add `LocalDatabaseSchemaFixesTest` case: legacy `profileId ... CHECK (profileId = 1)` table with one populated row survives `applyPendingLocalSchemaFixes()` — row persists under `userId = ''`, fields intact, `isOnboardingComplete("")` is `false`, a subsequent two-user `upsertProfile` succeeds (proves `CHECK` is gone)
- [x] 1.5 GREEN — Add `ensureLearnerProfileEntityShape()` to `di/LocalDatabaseSchemaFixes.kt`: `PRAGMA table_info` guard → `PRAGMA foreign_keys=OFF` → `BEGIN` → `CREATE ..._new` → `INSERT ... SELECT '' AS userId, ...` → `DROP` → `RENAME` → `COMMIT`, `ROLLBACK` on throw, `foreign_keys=ON` in `finally`; register after `ensureExerciseEntityShape()` in `applyPendingLocalSchemaFixes()`
- [x] 1.6 GREEN — Update `domain/LearnerProfileRepository.kt`: add `userId: String` as first parameter on `getProfile`, `isOnboardingComplete`, `upsertProfile`
- [x] 1.7 GREEN — Update `data/SqlDelightLearnerProfileRepository.kt`: thread `userId` through to the scoped queries; blank-`userId` short circuit per 1.2
- [x] 1.8 REFACTOR — Ran `./gradlew :composeApp:jvmTest --tests "*SqlDelightLearnerProfileRepositoryTest*" --tests "*LocalDatabaseSchemaFixesTest*"`; compile fails as EXPECTED, solely due to the 5 Phase 2 caller files (`App.kt`, `CourseViewModel.kt`, `OnboardingViewModel.kt`, `ProfileViewModel.kt`, `HomeDashboardViewModel.kt`) not yet updated to the new signatures — zero errors originate from any Phase 1 file; no scaffolding duplication introduced. Full green run deferred to Phase 2 PR per this run's explicit scope boundary.

## Phase 2: AuthGate + ViewModel Wiring (PR 2)

- [x] 2.1 RED — In `OnboardingViewModelTest`, add an `AuthRepository` fake (shape of `ProfileFakeAuthRepository`) and give `FakeLearnerProfileRepository` a `Map<String, LearnerProfile>` store; assert `completeOnboarding()` upserts under the session user id, and a null-user session sets `errorMessage` without writing (`isSaving = false`)
- [x] 2.2 GREEN — Update `ui/OnboardingViewModel.kt`: new `authRepository: AuthRepository` first constructor param; `completeOnboarding()` resolves `authRepository.session.value.user?.id`, null/blank sets error and returns without writing
- [x] 2.3 GREEN — Update `App.kt`'s `AuthGate`: add `session.user?.id` as a `produceState` vararg key alongside `isAuthenticated`/`token`/`onboardingRefreshKey`; blank/null id treated as not onboarded (spec: onboarding-flow "Missing session user id fails toward showing onboarding")
- [x] 2.4 GREEN — Update `ui/CourseViewModel.kt`: new `authRepository` param; scope its `getProfile` call to the session user id
- [x] 2.5 GREEN — Update `ui/ProfileViewModel.kt` and `ui/home/HomeDashboardViewModel.kt` call sites: pass the existing session user id into `getProfile`
- [x] 2.6 GREEN — Update `ProfileViewModelTest` and `HomeDashboardViewModelTest` fakes/assertions for the new `getProfile(userId)` signature
- [x] 2.7 GREEN — Update `ComposeAppCommonTest`'s Koin graph check: update `FakeLearnerProfileRepository` signature so `OnboardingViewModel`/`CourseViewModel` still construct through `viewModelOf`
- [x] 2.8 Verify — Run full `./gradlew :composeApp:jvmTest`
- [x] 2.9 Manual verification — Confirmed by user on device: two accounts on the same device now get independent onboarding state.

## Notes

- `di/AppModule.kt` is intentionally unchanged (design decision: `viewModelOf` resolves the added `AuthRepository` param reflectively; verified by 2.7, not a wiring edit).
- The `learner-profile` delta spec's "Existing installs are migrated" scenario has been corrected to describe the table rebuild (not additive `ALTER TABLE`), matching the proposal/design.
- Threat Matrix: N/A per design (no routing/shell/subprocess/VCS boundary) — no RED-test rows required beyond the ones above.
