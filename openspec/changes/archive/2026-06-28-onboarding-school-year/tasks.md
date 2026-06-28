# Tasks: Onboarding School Year

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~675 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-always |
| Chain strategy | feature-branch-chain (recommended) |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Data Foundation — domain types, province catalog, SQLDelight persistence, repo tests | PR 1 | Base = feature/tracker branch; self-contained data layer |
| 2 | Auth Gate + DI — ONBOARDING state, gate routing, Koin wiring, routing tests | PR 2 | Base = PR 1 branch; connects gate to profile check |
| 3 | Onboarding UI + Course — ViewModel, screen, CourseViewModel year integration, tests | PR 3 | Base = PR 2 branch; full onboarding UX |

## Phase 1: Foundation / Data Layer

- [x] 1.1 Create `domain/StudentTrack.kt` with enum: `PRIMARY`, `SECONDARY`, `TECHNICAL_SECONDARY`, `SELF_DIRECTED`
- [x] 1.2 Create `domain/LearnerProfileRepository.kt` interface (`getProfile`, `isOnboardingComplete`, `upsertProfile`)
- [x] 1.3 Create `data/ProvinceSchoolCatalog.kt` — 24 provinces, year-band resolution (6+6, 7+5, technical extra year)
- [x] 1.4 Add `LearnerProfileEntity` table + queries (`selectProfile`, `upsertProfile`, `clearProfile`) to `AppDatabase.sq`
- [x] 1.5 Create `data/SqlDelightLearnerProfileRepository.kt` implementing `LearnerProfileRepository`
- [x] 1.6 Write `SqlDelightLearnerProfileRepositoryTest.kt` — single-row upsert, read-back, null-on-missing

## Phase 2: Auth Gate + DI Wiring

- [x] 2.1 Add `AuthView.ONBOARDING` to `AuthGateRouter.kt`; update `resolveAuthView()` to accept `onboardingComplete`
- [x] 2.2 Modify `App.kt` `AuthGate()` to inject `LearnerProfileRepository` and route auth+incomplete to onboarding
- [x] 2.3 Register `LearnerProfileRepository`, `SqlDelightLearnerProfileRepository`, `StudentTrack` adapter in `AppModule.kt`
- [x] 2.4 Extend `AuthGateRoutingTest.kt` — authenticated/incomplete→ONBOARDING, complete→COURSE

## Phase 3: Onboarding UI + Course Integration

- [x] 3.1 Create `OnboardingViewModel.kt` — step state machine (province→year→category→save), province-based validation
- [x] 3.2 Create `OnboardingScreen.kt` — multi-step Compose UI: province picker, year selector, category selector, confirmation
- [x] 3.3 Modify `CourseViewModel.kt` — read `LearnerProfileRepository.schoolYear`, call `getOfficialCourses(schoolYear)`
- [x] 3.4 Write `OnboardingViewModelTest.kt` — step progression, 4-track availability, province boundaries, completion

## Phase 4: Verification

- [x] 4.1 Build and verify SQLDelight generation: `./gradlew :composeApp:jvmTest`
- [x] 4.2 Verify full compile across JVM and Android targets
