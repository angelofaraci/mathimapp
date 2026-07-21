# Tasks: Auth Registration Flow Stability

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 300–350 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Koin infrastructure at platform entry points | PR 1 | Foundation; tests not yet runnable |
| 2 | Router→ViewModel conversion + wiring | PR 1 | Depends on unit 1 |
| 3 | Onboarding layout fix | PR 1 | Independent layout concern |
| 4 | Test updates and new test | PR 1 | Verifies all units |

Single PR recommended — all units are tightly coupled to the same Koin+ViewModel infrastructure change, and estimated total is under 400 lines. Monitor changed lines during implementation.

## Phase 1: Koin Infrastructure

- [x] 1.1 Create `KoinInitializer.kt` with guarded `startKoin` accepting a platform `Module` + `appModule`
- [x] 1.2 Modify `MainActivity.kt` — build Android `DatabaseDriverFactory` module, call init before `setContent`
- [x] 1.3 Modify `MainViewController.kt` — build native driver module, call init before `ComposeUIViewController`
- [x] 1.4 Modify `App.kt` — remove `KoinApplication` wrapper; inject `AuthGateViewModel` via `koinViewModel()`
- [x] 1.5 Delete `PlatformModule.kt` and platform `actual` files (android/ios/jvm) — superseded by entry-point modules

## Phase 2: ViewModel Conversion

- [x] 2.1 Convert `AuthGateRouter` class to `AuthGateViewModel : ViewModel()` preserving `target` StateFlow and switch/toggle methods
- [x] 2.2 Convert `MainRouter` class to `MainRouterViewModel : ViewModel()` preserving tab state and helpers
- [x] 2.3 Register both ViewModels in `AppModule.kt` via `viewModelOf`
- [x] 2.4 Update `AuthenticatedHomeScaffold.kt` — replace `remember { MainRouter() }` with `koinViewModel<MainRouterViewModel>()`
- [x] 2.5 Update `HomeDashboardScreen.kt` signature to accept `MainRouterViewModel`

## Phase 3: Onboarding Layout Fix

- [x] 3.1 Modify `OnboardingScreen.kt` — wrap step content in `Modifier.weight(1f)` container so Continue/Back stay on-screen

## Phase 4: Testing

- [x] 4.1 Update `AuthGateRoutingTest.kt` — reference `AuthGateViewModel`, assert target survives reuse
- [x] 4.2 Update `MainRouterTest.kt` — reference `MainRouterViewModel`, assert tab retention
- [x] 4.3 Create `OnboardingScreenTest.kt` — render province list at constrained viewport; assert Continue button visible after selection
- [x] 4.4 Verify `./gradlew :composeApp:jvmTest :composeApp:assembleDebug` passes
