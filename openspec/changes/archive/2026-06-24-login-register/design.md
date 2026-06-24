# Design: Login/Register Frontend Integration

## Technical Approach

Implement the minimal auth slice across the existing ownership boundaries: `shared` owns serializable auth DTO contracts, `server` consumes those DTOs and keeps public registration student-only, and `composeApp` owns Ktor auth calls, in-memory session state, form ViewModels, screens, and the `App.kt` auth gate. No navigation library or token persistence is introduced.

## Architecture Decisions

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Move auth DTOs to `shared` | Keeps client/server serialization aligned but requires removing duplicate server declarations | Use `shared/src/commonMain/.../models/Models.kt` for `RegisterRequest`, `LoginRequest`, `AuthResponse` |
| Keep role in public register payload | Smallest move but allows public teacher creation through the API | `RegisterRequest` has `name`, `email`, `password`; server assigns `UserRole.STUDENT` |
| Repository-owned session state | Slightly more structure than direct TokenStore writes in screens | `AuthRepository` is the only boundary that mutates `TokenStore` and exposes `StateFlow<AuthSession>` |
| Conditional rendering in `App.kt` | Does not scale like navigation, but fits this slice | Use a small auth gate and simple text-link switching between login/register |

## Data Flow

```text
Login/RegisterScreen -> Login/RegisterViewModel -> AuthRepository -> AuthApi -> /auth/*
                                           |             |
                                           |             -> TokenStore.accessToken
                                           -> AuthSession StateFlow -> App.kt -> CourseScreen

CourseScreen logout -> AuthRepository.logout() -> TokenStore cleared -> App.kt shows LoginScreen
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modify | Add `RegisterRequest`, `LoginRequest`, `AuthResponse`; omit public role input. |
| `server/src/main/kotlin/com/example/proyectofinal/models/UserDto.kt` | Modify | Remove moved auth DTOs; keep server-only `UpdateUserRequest`. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/authRoutes.kt` | Modify | Use shared auth DTOs via existing package imports; generate token/user with `STUDENT` for registration. |
| `server/src/main/kotlin/com/example/proyectofinal/service/AuthService.kt` | Modify | Stop reading `request.role`; persist public users as `UserRole.STUDENT`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/AuthApi.kt` | Create | POST `/auth/login` and `/auth/register`; return `AuthResponse`; surface non-2xx `bodyAsText()` as raw error. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/AuthRepository.kt` | Create | Define auth/session boundary. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorAuthRepository.kt` | Create | Mutate `TokenStore`, expose session, implement login/register/logout. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginViewModel.kt` | Create | Login form state machine. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterViewModel.kt` | Create | Register form state machine without role selection. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginScreen.kt` | Create | Login UI with register text link. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterScreen.kt` | Create | Register UI with login text link. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modify | Bind `AuthApi`, `AuthRepository`, auth ViewModels. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modify | Observe auth session, switch auth/main content, pass logout callback to courses. |

## Interfaces / Contracts

`AuthSession`: `token: String?`, `user: User?`, `isAuthenticated = token != null`. `AuthRepository`: `val session: StateFlow<AuthSession>`, `suspend fun login(email, password): Result<User>`, `suspend fun register(name, email, password): Result<User>`, `fun logout()`.

`LoginUiState` and `RegisterUiState` stay screen-local: fields, `isLoading`, `errorMessage`. They do not expose or store tokens.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Shared/server integration | Register DTO shape and student-only public registration | Update `ServerIntegrationTest`; run `./gradlew :server:test` |
| App unit | `AuthApi` paths/body/error text, repository token/session mutation, ViewModel loading/error states | Add common tests with `MockEngine` and fake repositories; run `./gradlew :composeApp:jvmTest` |
| Build | Android/KMP wiring and Koin bindings | Run `./gradlew :composeApp:assembleDebug` |
| Combined verification | Auth contract remains aligned | Run `./gradlew :server:test :composeApp:jvmTest` |

## Migration / Rollout

No data migration required. Roll out as one reviewable slice; if it exceeds the 400-line budget, split into contract/server first and composeApp auth UI second.

## Risks and Follow-up Work

- Token is lost on process death; persistent secure storage is a follow-up.
- Raw server strings are acceptable now; structured error DTOs should follow.
- `App.kt` conditional routing is intentionally small; add navigation before more top-level flows.
- Existing tests that expect teacher public registration must be updated to match student-only registration.

## Open Questions

None.
