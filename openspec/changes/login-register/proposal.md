# Proposal: Login/Register Frontend Integration

## Intent

The app currently renders `CourseScreen` unconditionally with no authentication UI or session management. We need to integrate the existing backend auth endpoints into the frontend so users can register, log in, log out, and access courses as authenticated users.

## Scope

### In Scope
- Move `RegisterRequest`, `LoginRequest`, `AuthResponse` from `server` to `shared`
- Add `AuthApi` (Ktor client) and `AuthRepository` in `composeApp`
- Build `LoginScreen` and `RegisterScreen` with `MutableStateFlow` ViewModels
- Conditionally route between auth and main content in `App.kt`
- Add logout action that clears the in-memory token and returns to login
- Keep `TokenStore` in-memory only

### Out of Scope
- Token persistence across app restarts
- Role selection UI (public registration defaults to `STUDENT`)
- Teacher promotion / profile settings
- Compose Navigation library migration
- Structured error contract alignment

## Capabilities

### New Capabilities
- `frontend-auth`: Login/register UI, API client, repository, in-memory session, and conditional app routing
- `auth-logout`: Clear session and return to auth flow

### Modified Capabilities
- `client-server-contract`: Extend shared contract with auth request/response DTOs

## Approach

Minimal slice. Move auth DTOs to `shared` so both client and server use the same contract. Add a thin Ktor `AuthApi` and an in-memory `AuthRepository`. Build two screens with basic `MutableStateFlow` ViewModels. Use simple conditional rendering in `App.kt` based on token presence. Logout resets the token and flips the conditional state. Display raw server error strings for now.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/.../models/Models.kt` | Modified | Append auth DTOs |
| `server/.../models/UserDto.kt` | Modified | Remove DTOs; import from shared |
| `server/.../routes/authRoutes.kt` | Modified | Import auth DTOs from shared |
| `composeApp/.../data/AuthApi.kt` | New | Ktor client auth endpoints |
| `composeApp/.../domain/AuthRepository.kt` | New | Auth contract interface |
| `composeApp/.../data/KtorAuthRepository.kt` | New | Repository implementation |
| `composeApp/.../ui/LoginScreen.kt` | New | Login UI |
| `composeApp/.../ui/RegisterScreen.kt` | New | Register UI |
| `composeApp/.../ui/LoginViewModel.kt` | New | Login state machine |
| `composeApp/.../ui/RegisterViewModel.kt` | New | Register state machine |
| `composeApp/.../di/AppModule.kt` | Modified | Add auth bindings |
| `composeApp/.../App.kt` | Modified | Conditional auth/courses routing |
| `composeApp/.../di/TokenStore.kt` | Modified | Keep in-memory for this slice |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Token lost on process death | Certain | Acceptable for slice 1; persistence is the next slice |
| Raw server error strings in UI | High | Display as-is; align structured errors in a follow-up |
| `App.kt` conditional scaling | Med | Only auth + courses; migrate to navigation library later |

## Rollback Plan

Revert the commits. Restore server DTOs to their original file. Remove auth UI and repository files. Restore `App.kt` to unconditional `CourseScreen`.

## Dependencies

- Backend `POST /auth/register` and `POST /auth/login` are already deployed and functional.

## Success Criteria

- [ ] A new user can register as `STUDENT` and land on `CourseScreen`
- [ ] An existing user can log in and land on `CourseScreen`
- [ ] Logout clears the session and returns to the login screen
- [ ] Server module still compiles and tests pass
- [ ] App builds for Android and JVM targets

## Proposal Question Round

Product decisions were pre-provided by the orchestrator. The following assumptions remain:

1. **Error display**: Raw server error strings will be shown to the user (e.g., "Email already registered"). No client-side error mapping layer in this slice.
2. **Navigation between Login and Register**: A simple clickable text link ("Don't have an account? Register") will toggle between the two screens inside the auth conditional branch.

If any of these assumptions are incorrect, please flag them before the spec phase begins.
