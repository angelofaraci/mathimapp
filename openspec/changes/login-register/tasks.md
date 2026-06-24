# Tasks: Login/Register Frontend Integration

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~565 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: contract+server → PR 2: app infra → PR 3: screens |
| Delivery strategy | auto-forecast |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes (resolved)
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Shared auth DTOs + server STUDENT-only | PR 1 | Server tests included |
| 2 | AuthApi, AuthRepository, DI, App gate | PR 2 | Infra tests included |
| 3 | Login/Register screens + ViewModels | PR 3 | ViewModel tests included |

## Phase 1: Shared Contract + Server

- [x] 1.1 Add `RegisterRequest(name,email,password)`, `LoginRequest`, `AuthResponse` to `shared/.../models/Models.kt`
- [x] 1.2 Remove auth DTOs from `server/.../models/UserDto.kt`; keep only `UpdateUserRequest`
- [x] 1.3 Update `server/.../routes/authRoutes.kt` — import shared DTOs; always assign STUDENT
- [x] 1.4 Update `server/.../service/AuthService.kt` — hardcode `UserRole.STUDENT` in `createUser`
- [x] 1.5 Fix `ServerIntegrationTest`: `register returns token` (TEACHER→STUDENT); rm `role` from helper
- [x] 1.6 Verify `./gradlew :server:test` passes

## Phase 2: App Auth Infrastructure

- [x] 2.1 Create `composeApp/.../data/AuthApi.kt` — Ktor POST `/auth/login` and `/auth/register`
- [x] 2.2 Create `composeApp/.../domain/AuthRepository.kt` — interface with `session: StateFlow<AuthSession>`
- [x] 2.3 Create `composeApp/.../data/KtorAuthRepository.kt` — implements via AuthApi + TokenStore
- [x] 2.4 Update `composeApp/.../di/AppModule.kt` — bind AuthApi, AuthRepository, ViewModels
- [x] 2.5 Update `composeApp/.../App.kt` — observe AuthSession; conditional render auth or CourseScreen
- [x] 2.6 Write `KtorAuthRepositoryTest` in commonTest: MockEngine + token store assertions

## Phase 3: Auth Screens + ViewModels

- [x] 3.1 Create `composeApp/.../ui/LoginScreen.kt` — email/password + register link + loading/error
- [x] 3.2 Create `composeApp/.../ui/RegisterScreen.kt` — name/email/password + login link + loading/error
- [x] 3.3 Create `composeApp/.../ui/LoginViewModel.kt` — `MutableStateFlow<LoginUiState>` + login action
- [x] 3.4 Create `composeApp/.../ui/RegisterViewModel.kt` — `MutableStateFlow<RegisterUiState>` + register action
- [x] 3.5 Write ViewModel commonTest: loading/error states, empty field validation
- [x] 3.6 Verify `./gradlew :composeApp:jvmTest :server:test` passes

## Phase 4: Corrective — Auth Gate Routing Testability

Triggered by `sdd-verify` failure: two `frontend-auth` "Auth Entry Flow" scenarios ("Default state is login", "Text links switch forms") had no runtime covering test because the routing decision lived in inline Composable state with no Compose UI test harness.

- [x] 4.1 Extract the auth-gate routing decision into a pure, framework-agnostic construct (`AuthGateRouter` + `AuthScreenTarget`/`AuthView` enums + `resolveAuthView` pure function) in `composeApp/.../ui/AuthGateRouter.kt`. Defaults to LOGIN; exposes switch/toggle actions.
- [x] 4.2 Refactor `App.kt` `AuthGate` to route through `AuthGateRouter` + `resolveAuthView` instead of inline `mutableStateOf`; remove the old `AuthScreenMode` enum.
- [x] 4.3 Add `composeApp/src/commonTest/.../ui/AuthGateRoutingTest.kt` — plain kotlin.test + StateFlow assertions covering default=LOGIN, switch LOGIN<->REGISTER, and authenticated session hides auth area. No Compose UI test dependencies.
- [x] 4.4 Verify `./gradlew :composeApp:jvmTest` passes.
