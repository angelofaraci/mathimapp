## Implementation Progress

**Change**: onboarding-school-year
**Mode**: Standard

### Completed Tasks

#### Phase 1: Foundation / Data Layer (slice 1 — DONE)
- [x] 1.1 Create `domain/StudentTrack.kt` with enum: `PRIMARY`, `SECONDARY`, `TECHNICAL_SECONDARY`, `SELF_DIRECTED`
- [x] 1.2 Create `domain/LearnerProfileRepository.kt` interface (`getProfile`, `isOnboardingComplete`, `upsertProfile`)
- [x] 1.3 Create `data/ProvinceSchoolCatalog.kt` — 24 provinces, year-band resolution (6+6, 7+5, technical extra year)
- [x] 1.4 Add `LearnerProfileEntity` table + queries (`selectProfile`, `upsertProfile`, `clearProfile`) to `AppDatabase.sq`
- [x] 1.5 Create `data/SqlDelightLearnerProfileRepository.kt` implementing `LearnerProfileRepository`
- [x] 1.6 Write `SqlDelightLearnerProfileRepositoryTest.kt` — single-row upsert, read-back, null-on-missing

#### Phase 2: Auth Gate + DI Wiring (slice 2 — DONE)
- [x] 2.1 Add `AuthView.ONBOARDING` to `AuthGateRouter.kt`; update `resolveAuthView()` to accept `onboardingComplete`
- [x] 2.2 Modify `App.kt` `AuthGate()` to inject `LearnerProfileRepository` and route auth+incomplete to onboarding
- [x] 2.3 Register `LearnerProfileRepository`, `SqlDelightLearnerProfileRepository`, `StudentTrack` adapter in `AppModule.kt`
- [x] 2.4 Extend `AuthGateRoutingTest.kt` — authenticated/incomplete→ONBOARDING, complete→COURSE

#### Phase 3: Onboarding UI + Course Integration (slice 3 — DONE)
- [x] 3.1 Create `OnboardingViewModel.kt` — step state machine (province→year→category→save), province-based validation
- [x] 3.2 Create `OnboardingScreen.kt` — multi-step Compose UI: province picker, year selector, category selector, confirmation
- [x] 3.3 Modify `CourseViewModel.kt` — read `LearnerProfileRepository.schoolYear`, call `getOfficialCourses(schoolYear)`
- [x] 3.4 Write `OnboardingViewModelTest.kt` — step progression, 4-track availability, province boundaries, completion

#### Phase 4: Verification (slice-local verification completed)
- [x] 4.1 Build and verify SQLDelight generation: `./gradlew :composeApp:jvmTest`
- [x] 4.2 Verify full compile across JVM and Android targets

### Files Changed
| File | Action | What Was Done |
|------|--------|---------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/StudentTrack.kt` | Created | Added the onboarding track enum with stable display/storage labels and tolerant parsing for persisted values. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/LearnerProfileRepository.kt` | Created | Added the local learner-profile contract plus the `LearnerProfile` data model. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/ProvinceSchoolCatalog.kt` | Created | Added the 24-province school-structure catalog, year options, and year/track validation helpers for primary, secondary, technical secondary, and self-directed flows. |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modified | Added the single-row `LearnerProfileEntity` schema plus `selectProfile`, `upsertProfile`, and `clearProfile` queries. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/SqlDelightLearnerProfileRepository.kt` | Created | Added the SQLDelight-backed repository that persists one learner profile and maps track text back into the domain enum. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/SqlDelightLearnerProfileRepositoryTest.kt` | Created | Added repository tests for missing profile, completed profile persistence, self-directed round-trip, row replacement, and incomplete-profile completion checks. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthGateRouter.kt` | Modified | Added the `ONBOARDING` auth view and made the routing decision depend on learner-profile completion for authenticated sessions. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modified | Replaced the bounded onboarding placeholder with the real onboarding screen and added a refresh trigger so auth-gate completion is re-evaluated immediately after profile save. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modified | Registered `OnboardingViewModel` in Koin while keeping learner-profile repository wiring available to both onboarding and course loading. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/AuthGateRoutingTest.kt` | Modified | Extended the pure routing tests to cover authenticated-complete → `COURSE` and authenticated-incomplete → `ONBOARDING`. |
| `openspec/changes/onboarding-school-year/tasks.md` | Modified | Marked Phase 1 slice tasks 1.1 through 1.6 complete, then marked Phase 2 slice tasks 2.1 through 2.4 complete. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/OnboardingViewModel.kt` | Created | Added the onboarding step state machine, province/year validation, four-category availability model, and learner-profile persistence on completion. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/OnboardingScreen.kt` | Created | Added the real multi-step onboarding UI for province, school year, category selection, and confirmation. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/CourseViewModel.kt` | Modified | Made official course loading read the stored learner profile and request courses with the selected `schoolYear`. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/OnboardingViewModelTest.kt` | Created | Added onboarding ViewModel coverage for step progression, four-category rendering rules, province boundaries, and completion persistence. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ComposeAppCommonTest.kt` | Modified | Updated the course ViewModel tests so they provide a learner-profile dependency and verify the persisted `schoolYear` reaches `getOfficialCourses(schoolYear)`. |
| `openspec/changes/onboarding-school-year/tasks.md` | Modified | Marked Phase 3 slice tasks 3.1 through 3.4 complete. |
| `openspec/changes/onboarding-school-year/apply-progress.md` | Modified | Merged slice 3 onboarding UI + course integration progress into the cumulative apply artifact without losing slices 1-2 history. |

### Verification
| Command | Result |
|---------|--------|
| `./gradlew :composeApp:jvmTest` | Passed (`BUILD SUCCESSFUL`). SQLDelight code generation, compile, and the new learner-profile repository tests are green for the composeApp JVM target. |
| `./gradlew :composeApp:jvmTest` | Passed again (`BUILD SUCCESSFUL`) after the auth-gate/DI slice. The new `ONBOARDING` routing logic compiled cleanly and the extended `AuthGateRoutingTest` coverage is green on the composeApp JVM target. |
| `./gradlew :composeApp:jvmTest` | Passed again (`BUILD SUCCESSFUL`) after the onboarding UI + course integration slice. The new onboarding ViewModel tests passed and `CourseViewModel` now compiles/runs with learner-profile-backed school-year filtering on the composeApp JVM target. |
| `./gradlew :composeApp:assembleDebug` | Passed (`BUILD SUCCESSFUL`) during verification. Android compile/build evidence is green for the composeApp target. |

### Deviations from Design
- `studentTrack` continues to be stored as plain text and parsed in `SqlDelightLearnerProfileRepository`. This slice still avoids a schema/type migration because the change only needs year-aware availability and local persistence, not a database representation refactor.

### Issues Found
- SQLDelight insert queries return `QueryResult<Long>`; the repository implementation had to normalize the insert call back to `Unit` to satisfy the repository contract. This remained stable after the auth-gate slice and is still covered by `:composeApp:jvmTest`.
- The new `resolveAuthView()` signature initially broke two existing test call sites that still passed only `(session, target)`. Updating those call sites to include `onboardingComplete` fixed the compile failure and the full JVM test suite passed afterward.
- The top-level auth-gate completion check is driven by `produceState`, so persisting the profile alone does not automatically re-run the check in the same session. `App.kt` now bumps a local refresh key when onboarding completes so the gate re-resolves to `COURSE` immediately.

### Remaining Tasks
- None. All planned tasks are complete.

### Workload / PR Boundary
- Mode: stacked PR slice
- Current work unit: PR 3 / Slice 3 — Onboarding UI + Course Integration
- Boundary: starts from the auth-gate/DI slice with learner-profile persistence and `ONBOARDING` routing already in place; ends with the real onboarding ViewModel/screen, immediate post-save navigation back into the auth gate, school-year-aware course loading, and slice-local tests. It intentionally excludes the pending broader target verification in task 4.2.
- Boundary: starts from the auth-gate/DI slice with learner-profile persistence and `ONBOARDING` routing already in place; ends with the real onboarding ViewModel/screen, immediate post-save navigation back into the auth gate, school-year-aware course loading, slice-local tests, and completed JVM + Android target verification.
- Chain strategy: `stacked-to-main` — this slice should target the Phase 2 branch (or `main` after the previous slices merge).
- Estimated review budget impact: Moderate slice covering the new onboarding UI/state machine plus the minimal auth-gate, DI, and course-view-model support needed to keep the UX autonomous and testable.

### Status
16/16 total tasks complete for the planned change scope (Phases 1-3 done; verification complete for JVM tests and Android compile). Ready for `sdd-archive`.
