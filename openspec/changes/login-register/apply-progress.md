## Implementation Progress

**Change**: login-register
**Mode**: Standard

### Completed Tasks

#### Phase 1: Shared Contract + Server (slice 1 — DONE)
- [x] 1.1 Add `RegisterRequest(name,email,password)`, `LoginRequest`, `AuthResponse` to `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt`.
- [x] 1.2 Remove auth DTOs from `server/src/main/kotlin/com/example/proyectofinal/models/UserDto.kt`; keep only `UpdateUserRequest` and server-only DTOs.
- [x] 1.3 Update `server/src/main/kotlin/com/example/proyectofinal/routes/authRoutes.kt` to use shared DTOs and always assign `STUDENT` for public registration.
- [x] 1.4 Update `server/src/main/kotlin/com/example/proyectofinal/service/AuthService.kt` so public user creation persists `UserRole.STUDENT` instead of reading a role from the request.
- [x] 1.5 Fix server tests impacted by STUDENT-only public registration, including `ServerIntegrationTest.register returns token` and helper payloads.
- [x] 1.6 Verify `./gradlew :server:test` passes.

#### Phase 2: App Auth Infrastructure (slice 2 — DONE, confirmed by orchestrator)
- [x] 2.1 Create `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/AuthApi.kt` — Ktor POST `/auth/login` and `/auth/register`; surfaces non-2xx `bodyAsText()` as raw `AuthApiException`.
- [x] 2.2 Create `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/AuthRepository.kt` — `AuthSession(token, user, isAuthenticated)` + `StateFlow<AuthSession>` interface.
- [x] 2.3 Create `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorAuthRepository.kt` — implements via `AuthApi` + `TokenStore`; the only boundary that mutates `TokenStore`.
- [x] 2.4 Update `composeApp/.../di/AppModule.kt` — bind `AuthApi` and `AuthRepository`.
- [x] 2.6 Write `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorAuthRepositoryTest.kt` — MockEngine assertions for login/register success, raw error text, and logout.

#### Phase 3: Auth Screens + ViewModels (slice 3 — DONE in this batch)
- [x] 2.5 Update `composeApp/.../App.kt` — `AuthGate` observes `AuthSession`, conditionally renders Login/Register (text-link switching) or `CourseScreen` with a Logout `TextButton` that calls `AuthRepository.logout()`. No navigation library.
- [x] 3.1 Create `composeApp/.../ui/LoginScreen.kt` — email/password `OutlinedTextField`s, loading `CircularProgressIndicator`, raw error text, register text link.
- [x] 3.2 Create `composeApp/.../ui/RegisterScreen.kt` — name/email/password fields (NO role picker), loading indicator, raw error text, login text link.
- [x] 3.3 Create `composeApp/.../ui/LoginViewModel.kt` — `MutableStateFlow<LoginUiState>` (fields/isLoading/errorMessage) + `login()` action that calls `AuthRepository`.
- [x] 3.4 Create `composeApp/.../ui/RegisterViewModel.kt` — `MutableStateFlow<RegisterUiState>` (no role) + `register()` action that calls `AuthRepository`.
- [x] 3.5 Write ViewModel commonTest — loading/error states, empty field validation, using a gated fake `AuthRepository`.
- [x] 3.6 Verify `./gradlew :composeApp:jvmTest` passes (`:server:test` not re-run; server untouched in this slice).
- [x] Bind `LoginViewModel` and `RegisterViewModel` in `composeApp/.../di/AppModule.kt` via `viewModelOf`.

#### Phase 4: Corrective — Auth Gate Routing Testability (corrective re-run, DONE)
Triggered by `sdd-verify` failure: two `frontend-auth` "Auth Entry Flow" scenarios ("Default state is login", "Text links switch forms") had no runtime covering test — the routing decision lived in inline Composable state (`mutableStateOf(AuthScreenMode.Login)` in `App.kt`) and the project has no Compose UI test harness in commonTest.
- [x] 4.1 Extract the auth-gate routing decision into a pure, framework-agnostic construct `AuthGateRouter` (with `AuthScreenTarget` / `AuthView` enums and `resolveAuthView` pure function) in `composeApp/.../ui/AuthGateRouter.kt`. Defaults to `LOGIN`; exposes `switchToLogin()` / `switchToRegister()` / `toggle()`.
- [x] 4.2 Refactor `App.kt` `AuthGate` to route through `AuthGateRouter` + `resolveAuthView` (text-link clicks call `router::switchToRegister` / `router::switchToLogin`); removed the old `AuthScreenMode` enum. Behavior-preserving for the auth gate.
- [x] 4.3 Add `composeApp/src/commonTest/.../ui/AuthGateRoutingTest.kt` — plain kotlin.test + `StateFlow.value` assertions (no Compose UI test deps, no `createComposeRule`/`uiTest`). Covers: default target = LOGIN; default view = LOGIN when anonymous ("Default state is login"); switch LOGIN->REGISTER and back ("Text links switch forms"); `toggle()` both directions; authenticated session hides the auth area (gate side of "Default state is login").
- [x] 4.4 Verify `./gradlew :composeApp:jvmTest` passes (52 tests, 0 failures, 0 errors, 0 skipped, 13 suites; new `AuthGateRoutingTest` = 6 tests green).

### Files Changed (this Phase 3 slice)
| File | Action | What Was Done |
|------|--------|---------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginViewModel.kt` | Created | Login form state machine; `MutableStateFlow<LoginUiState>`; empty-field validation; raw error message surfacing; session mutation left to `AuthRepository`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterViewModel.kt` | Created | Register form state machine with name/email/password (no role); same validation/error pattern. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginScreen.kt` | Created | Compose Login UI: email/password fields, loading indicator, raw error text, register text link. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterScreen.kt` | Created | Compose Register UI: name/email/password fields (no role picker), loading indicator, error text, login text link. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/LoginViewModelTest.kt` | Created | Tests: empty-field validation, loading→success, raw error surfacing; includes gated `FakeAuthRepository`. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/RegisterViewModelTest.kt` | Created | Tests: empty-field validation, register call shape (name/email/password only, student session), raw error surfacing. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modified | Added `AuthGate` observing `AuthSession`; conditional Login/Register/CourseScreen rendering with text-link switching; Logout button wired to `AuthRepository.logout()`. Preserved previews. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modified | Added `viewModelOf(::LoginViewModel)` and `viewModelOf(::RegisterViewModel)` bindings (minimal edit). |
| `openspec/changes/login-register/tasks.md` | Modified | Merged cumulative progress (Phase 1 + 2 + 3). |
| `openspec/changes/login-register/apply-progress.md` | Modified | Merged cumulative progress (Phase 1 + 2 + 3). |

### Files Changed (this Phase 4 corrective slice)
| File | Action | What Was Done |
|------|--------|---------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthGateRouter.kt` | Created | Pure, framework-agnostic auth-gate routing construct: `AuthScreenTarget` enum, `AuthView` enum, `AuthGateRouter` (holds `MutableStateFlow<AuthScreenTarget>` defaulting to `LOGIN`; `switchToLogin()` / `switchToRegister()` / `toggle()`), and `resolveAuthView(session, target)` pure function (authenticated -> COURSE, else target). No Compose dependency. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modified | Replaced inline `mutableStateOf(AuthScreenMode.Login)` with `remember { AuthGateRouter() }`; `AuthGate` now branches on `resolveAuthView(session, target)` and wires text links to `router::switchToRegister` / `router::switchToLogin`. Removed the private `AuthScreenMode` enum. Added imports for `AuthGateRouter`, `AuthView`, `resolveAuthView`. Behavior-preserving. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/AuthGateRoutingTest.kt` | Created | Plain kotlin.test + `StateFlow.value` assertions (no Compose UI test deps): default target = LOGIN; default view = LOGIN when anonymous; switch LOGIN<->REGISTER; `toggle()` both directions; authenticated session hides auth area. 6 tests. |
| `openspec/changes/login-register/tasks.md` | Modified | Added "Phase 4: Corrective — Auth Gate Routing Testability" (4.1-4.4), all marked `[x]`. |
| `openspec/changes/login-register/apply-progress.md` | Modified | Merged Phase 4 corrective batch into cumulative progress. |

### Verification
| Command | Result |
|---------|--------|
| `./gradlew :composeApp:jvmTest` (Phase 3 slice) | Passed (`BUILD SUCCESSFUL`; new Login/Register ViewModel tests compiled and executed; pre-existing `KtorAuthRepositoryTest` still passes). |
| `./gradlew :composeApp:jvmTest --rerun-tasks` (Phase 3 slice) | Passed — tests re-executed and green. |
| `./gradlew :server:test` | Not re-run this change (server untouched). Status from slice 1: Passed. |
| `./gradlew :composeApp:jvmTest` (Phase 4 corrective slice) | Passed — `BUILD SUCCESSFUL in 1m 26s`. **52 tests, 0 failures, 0 errors, 0 skipped across 13 suites.** New `com.example.proyectofinal.ui.AuthGateRoutingTest` = 6 tests, all green. JUnit XML in `composeApp/build/test-results/jvmTest/`. |

### Deviations from Design
None — implementation matches design.
- `AuthRepository` is the only `TokenStore` mutator; screens/ViewModels observe `session` and surface raw error strings.
- No role UI; registration uses the shared `RegisterRequest(name, email, password)` contract.
- `App.kt` uses simple state-based conditional rendering with text-link switching, no navigation library (per spec scope). As of the Phase 4 corrective slice, the routing decision is driven by the pure `AuthGateRouter` + `resolveAuthView` construct (the old private `AuthScreenMode` enum was removed) so the "Auth Entry Flow" scenarios are unit-testable in commonTest without a Compose UI test harness.

### Issues Found
- Initial transient compiler warnings about `ExperimentalCoroutinesApi` opt-in for `setMain`/`resetMain`/`UnconfinedTestDispatcher`/`advanceUntilIdle` in the new ViewModel tests. Resolved by adding `@OptIn(ExperimentalCoroutinesApi::class)` to each test class (after all imports). No behavioral change.
- Verify-gate gap (corrective): `sdd-verify` flagged the `frontend-auth` "Default state is login" and "Text links switch forms" scenarios as `UNTESTED` because the routing decision lived in inline Composable state. Resolved in Phase 4 by extracting `AuthGateRouter` / `resolveAuthView` and adding `AuthGateRoutingTest` (6 runtime tests, plain kotlin.test). No Compose UI test dependencies were added.

### Remaining Tasks
- None for this change. All 22 tasks across Phase 1, 2, 3, and the Phase 4 corrective slice are complete.

### Workload / PR Boundary
- Mode: stacked PR slice
- Current work unit: PR 4 / Slice 4 — corrective auth-gate routing testability (follow-up to the PR 3 verify failure)
- Boundary: starts from the PR 3 state (auth screens + ViewModels + `App.kt` auth gate already landed); ends with the pure `AuthGateRouter` construct, the `App.kt` refactor to route through it, the `AuthGateRoutingTest` commonTest, and `:composeApp:jvmTest` green. No `server` or `shared` files touched; no new Gradle dependencies.
- Chain strategy: `stacked-to-main` — this corrective PR targets the PR 3 branch (or `main` once PR 3 merges).
- Estimated review budget impact: ~160 changed lines across 1 new main file (`AuthGateRouter.kt`), 1 new test file (`AuthGateRoutingTest.kt`), and 1 modified file (`App.kt`) plus SDD artifacts. Small, focused review slice.

### Status
22/22 tasks complete (18 original + 4 corrective). Ready for `sdd-verify` re-run (verify phase: `./gradlew :server:test :composeApp:jvmTest` and `./gradlew :composeApp:assembleDebug`). The two previously-`UNTESTED` `frontend-auth` scenarios now have runtime covering tests in `AuthGateRoutingTest`.