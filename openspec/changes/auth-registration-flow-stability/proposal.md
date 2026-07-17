# Proposal: Auth Registration Flow Stability

## Intent

Fix three critical defects blocking users from completing authentication and onboarding: (1) login gets stuck after configuration change because Koin destroys and recreates the global DI context, causing `AuthGate` and `LoginViewModel` to observe different `AuthRepository` instances; (2) the onboarding province step pushes the Continue button off-screen due to an unconstrained `LazyColumn`; (3) rotation resets the auth gate from register back to login and restores an abandoned registration step because `AuthGateRouter` is held in `remember` and does not survive configuration changes.

## Scope

### In Scope
- Hoist Koin startup to Android `MainActivity` and iOS `MainViewController`; remove `KoinApplication` from `App.kt`
- Convert `AuthGateRouter` to `AuthGateViewModel` (survives config changes)
- Convert `MainRouter` to `MainRouterViewModel` (same class of bug, incidental)
- Fix `OnboardingScreen` layout so step content uses `Modifier.weight(1f)` and the Continue button remains visible
- Register new ViewModels in `AppModule`

### Out of Scope
- Persistent token storage (`InMemoryTokenStore` still loses tokens on process death)
- RegisterScreen "Back to Login" UX debt
- Backend contract or API changes

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `frontend-auth`: Add spec-level requirement that auth gate router target and auth session consistency MUST survive configuration changes without divergence between `AuthGate` and child ViewModels.
- `onboarding-flow`: Strengthen state-survival requirement to explicitly cover configuration changes; add requirement that all step action buttons MUST remain reachable/visible.

## Approach

**Approach A from exploration** — idiomatic Koin lifecycle + ViewModel state retention.

1. Move `startKoin { modules(appModule) }` from inside `App()` to `MainActivity.onCreate` (before `setContent`) and `MainViewController` (before `ComposeUIViewController`).
2. In `App.kt`, remove `KoinApplication` wrapper and inject `AuthGateViewModel` via `koinViewModel()`.
3. Convert `AuthGateRouter` class to `AuthGateViewModel : ViewModel()` exposing the same `target` StateFlow.
4. Apply the same `remember → ViewModel` conversion to `MainRouter` in `AuthenticatedHomeScaffold`.
5. In `OnboardingContent`, wrap the `when(state.currentStep)` block in a container with `Modifier.weight(1f)` so the scrollable list occupies remaining space and the bottom buttons stay on-screen.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/src/androidMain/.../MainActivity.kt` | Modified | Explicit `startKoin` before `setContent`; remove implicit Koin startup via `App()` |
| `composeApp/src/iosMain/.../MainViewController.kt` | Modified | Explicit `startKoin` before `ComposeUIViewController` |
| `composeApp/src/commonMain/.../App.kt` | Modified | Remove `KoinApplication`; use `koinViewModel<AuthGateViewModel>()` |
| `composeApp/src/commonMain/.../AuthGateRouter.kt` | Modified | Convert class to `AuthGateViewModel : ViewModel()` |
| `composeApp/src/commonMain/.../AppModule.kt` | Modified | Add `viewModelOf(::AuthGateViewModel)` and `viewModelOf(::MainRouterViewModel)` |
| `composeApp/src/commonMain/.../OnboardingScreen.kt` | Modified | Add `Modifier.weight(1f)` to step content container |
| `composeApp/src/commonMain/.../AuthenticatedHomeScaffold.kt` | Modified | Replace `remember { MainRouter() }` with `koinViewModel<MainRouterViewModel>()` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Cross-platform entry point regression | Low | Compile Android and iOS targets; smoke-test rotation on both |
| MainRouter conversion breaks bottom nav | Low | Apply identical ViewModel pattern; verify tab retention |
| Koin context lifecycle mismatch | Low | Follow Koin 4.1.0 documented pattern (`startKoin` outside Compose) |

## Rollback Plan

1. Revert all files in one commit.
2. If a hotfix is needed before full revert, temporarily restore `KoinApplication` in `App.kt` and the old `remember { AuthGateRouter() }` while keeping the onboarding layout fix.

## Dependencies

- Koin 4.1.0 (already in project)
- Existing `kotlin.test` suite (`:composeApp:jvmTest`)

## Success Criteria

- [ ] Login succeeds and reaches `HomeDashboardScreen` after device rotation on Android and iOS
- [ ] Register screen survives rotation without resetting to login or restoring an abandoned step
- [ ] Onboarding province step shows a scrollable province list with the Continue button always visible
- [ ] `HomeDashboardScreen` bottom-navigation tab survives rotation
- [ ] `./gradlew :composeApp:jvmTest` passes
- [ ] `./gradlew :composeApp:assembleDebug` passes
