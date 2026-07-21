## Exploration: auth-registration-flow-stability

### Current State

The auth entry flow is orchestrated by `AuthGate` in `App.kt`. `AuthGate` observes two key StateFlows:
1. `AuthRepository.session` — drives authentication state.
2. `AuthGateRouter.target` — toggles between `LOGIN` and `REGISTER` when anonymous.

`AuthGate` is wrapped inside `KoinApplication` at the root of `App()`. `LoginViewModel` and `RegisterViewModel` are standard `ViewModel` instances retrieved via `koinViewModel()`.

`OnboardingScreen` (shown after successful auth when the learner profile is incomplete) presents a 4-step wizard: Province → School Year → Category → Confirmation.

### Affected Areas

- **`composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt`**
  - `KoinApplication` is instantiated inside the `App()` composable. Koin Compose 4.1.0’s `CompositionKoinApplicationLoader` calls `stopKoin()` when the composition is abandoned (e.g., Android configuration change / Activity recreation). This destroys the global Koin context and recreates all `single` instances on recomposition.
  - `AuthGateRouter` is held in `remember { AuthGateRouter() }`, which does **not** survive configuration changes. It resets to `LOGIN` on rotation.

- **`composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthGateRouter.kt`**
  - A plain class with a `MutableStateFlow`. It has no lifecycle resilience.

- **`composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/OnboardingScreen.kt`**
  - `OnboardingContent` uses a `Column` with `fillMaxSize`. Inside it, `ProvinceStep` renders a `LazyColumn` with 23+ items. Because the `LazyColumn` is not constrained with `weight(1f)`, it expands to its full content height and pushes the `Continue` button below the visible viewport. The outer `Column` has no scrolling, so the user cannot reach the button.

- **`composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt`**
  - `MainRouter` is also held in `remember { MainRouter() }`, causing the bottom-navigation tab to reset on configuration change (incidental finding, same class of bug).

- **Platform entry points**
  - `composeApp/src/androidMain/kotlin/com/example/proyectofinal/MainActivity.kt` — currently starts Koin implicitly via `App()`; must start Koin explicitly before `setContent`.
  - `composeApp/src/iosMain/kotlin/com/example/proyectofinal/MainViewController.kt` — must start Koin before `ComposeUIViewController`.

### Root Cause Analysis

#### Defect 1 — Login cannot progress past its screen
When the composition is abandoned (configuration change, process death + recreation, etc.), `CompositionKoinApplicationLoader` stops the global Koin context. On recomposition, a new Koin context is created with fresh `single` instances. `AuthGate` injects the **new** `AuthRepository` (unauthenticated session). However, `LoginViewModel` survives in the `ViewModelStore` and retains the **old** `AuthRepository`. The login network call succeeds on the old instance, but `AuthGate` never observes the authentication because it is bound to the new instance. The user remains on `LoginScreen` indefinitely with no error.

#### Defect 2 — Selecting a province leaves no way to confirm/continue
`OnboardingScreen` layout bug. `LazyColumn` inside an unconstrained `Column` pushes the `Continue` button off-screen.

#### Defect 3 — Rotation returns registration to login and restores abandoned step
Two combined issues:
1. `AuthGateRouter` is lost on configuration change (`remember`), resetting the target to `LOGIN`.
2. `RegisterViewModel` survives in `ViewModelStore`, preserving its `step`. When the user re-opens registration, the old step is restored, creating a dirty state.

### Approaches

| Approach | Description | Pros | Cons | Effort |
|----------|---------------|------|------|--------|
| **A — Hoist Koin + ViewModel router + Layout fix** | Move Koin startup to platform entry points (Android `Application`, iOS app delegate). Remove `KoinApplication` from `App.kt`. Convert `AuthGateRouter` to `AuthGateViewModel` (extend `ViewModel`, register with `viewModelOf`). Apply `Modifier.weight(1f)` to step content in `OnboardingContent`. | Fixes the real architectural root cause (Koin lifecycle anti-pattern). Idiomatic state retention via ViewModel. Also fixes the incidental `MainRouter` bug if applied consistently. | Touches Android and iOS entry points. Requires adding a new ViewModel to `AppModule`. | Medium |
| **B — rememberSaveable router + Keep Koin in App.kt + Layout fix** | Keep `KoinApplication` in `App.kt`. Use `rememberSaveable` to persist the router target as a `String` / `Int`. Wrap `KoinApplication` in a way that avoids `stopKoin` on abandonment (e.g., isolate context). Fix onboarding layout. | Fewer files touched. | Does **not** solve the Koin singleton destruction issue; Koin 4.1.0 is explicitly designed to be started outside Compose. `KoinContext` is deprecated and not recommended. Fragile. | Medium |
| **C — SavedStateHandle router + Hoist Koin + Layout fix** | Same as A, but use `SavedStateHandle` inside `AuthGateViewModel` to persist target across process death. | Survives both config changes and process death. | Slightly more boilerplate. `SavedStateHandle` availability varies by platform without extra setup. | Medium-High |

### Recommendation

**Approach A** is the correct fix.

- **Koin lifecycle**: Koin 4.1.0 documentation and source explicitly expect `startKoin()` outside of the Compose tree. Keeping it inside `App()` causes `stopKoin()` on every composition abandonment, which is the root cause of the auth session divergence between `AuthGate` and `LoginViewModel`.
- **Router state**: Converting `AuthGateRouter` to a `ViewModel` is the standard, testable pattern for configuration-change resilience in KMP.
- **Onboarding layout**: Adding `Modifier.weight(1f)` to the step container is a one-line fix that keeps the `Continue` button always visible while allowing the list to scroll internally.

### Minimal Cohesive Repair Scope

1. **Platform entry points**
   - `MainActivity.kt` — call `startKoin` before `setContent`; remove implicit reliance on `KoinApplication` in `App()`.
   - `MainViewController.kt` — call `startKoin` before `ComposeUIViewController`.
   - (Add equivalent for any `jvmMain` or `desktopMain` entry point if present.)

2. **`App.kt`**
   - Remove `KoinApplication` wrapper.
   - Replace `remember { AuthGateRouter() }` with `koinViewModel<AuthGateViewModel>()`.

3. **`AuthGateRouter.kt`**
   - Convert to `AuthGateViewModel : ViewModel` with the same `target` StateFlow.

4. **`AppModule.kt`**
   - Register `viewModelOf(::AuthGateViewModel)`.

5. **`OnboardingScreen.kt`**
   - In `OnboardingContent`, wrap the `when(state.currentStep)` block in a `Box(Modifier.weight(1f))` so the step content occupies remaining space and the `Continue` / `Back` buttons remain visible.

6. **`AuthenticatedHomeScaffold.kt`** *(incidental but same class of bug)*
   - Apply the same `remember` → `ViewModel` conversion to `MainRouter` to prevent tab reset on rotation.

### Relevant Tests

- `KtorAuthRepositoryTest` — verifies repository auth behavior in isolation.
- `LoginViewModelTest` — verifies login UI state transitions.
- `RegisterViewModelTest` — verifies wizard step progression.
- `OnboardingViewModelTest` — verifies province/school-year/track logic.
- **Gap**: There are **no integration tests** for `App.kt` / `AuthGate` composition behavior across configuration changes. This is where the defects manifest.

### Risks

- **Cross-platform entry point changes**: The fix must be verified on both Android and iOS. A missing entry point (e.g., desktop) could break the build.
- **`MainRouter` debt**: `AuthenticatedHomeScaffold` has the exact same `remember` bug. If left unfixed, the user will experience tab resets after login, undermining the perceived stability of the auth flow.
- **`InMemoryTokenStore`**: Tokens do not survive process death. This is a pre-existing limitation outside the scope of these three defects, but it means users must re-authenticate after a force-kill. A persistent `TokenStore` should be planned separately.
- **RegisterScreen lacks a "Back to Login" link**: This is UX debt (not a reported defect) but becomes more noticeable once the router survives rotation cleanly.

### Ready for Proposal

Yes. The root causes are identified with high confidence (confirmed via Koin 4.1.0 source code), the repair scope is cohesive, and the next phase should be `sdd-propose` to capture the intent and rollback plan.
