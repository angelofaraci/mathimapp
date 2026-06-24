## Exploration: Login/Register Frontend Integration

### Current State

The backend already has working auth endpoints (`POST /auth/register`, `POST /auth/login`) that return `AuthResponse(token, user)`. `AuthService` uses bcrypt, and `Security` generates 24-hour JWTs. The `composeApp` has bearer-token HTTP client injection via `TokenStore` and `NetworkClient`, but there is **no auth UI, no Auth API client, no auth repository, and no session startup logic**. The app currently always shows `CourseScreen` unconditionally.

The `shared` module contains `User` and `UserRole`, but the auth request/response DTOs (`RegisterRequest`, `LoginRequest`, `AuthResponse`) live only in `server`. This creates a contract gap: the app cannot deserialize auth responses without either duplicating the classes or moving them to `shared`.

### Affected Areas

- `server/src/main/kotlin/com/example/proyectofinal/models/UserDto.kt` — `RegisterRequest`, `LoginRequest`, `AuthResponse` must move to `shared`.
- `server/src/main/kotlin/com/example/proyectofinal/routes/authRoutes.kt` — imports will change to `shared` models.
- `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` — append auth DTOs.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/TokenStore.kt` — stays in-memory for slice 1; persistence is a follow-up.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` — add `AuthApi` and `AuthRepository` bindings.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` — add conditional routing between auth and main content.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/` — new `AuthApi.kt` and `KtorAuthRepository.kt`.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/` — new `AuthRepository.kt`.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/` — new `LoginViewModel`, `RegisterViewModel`, and screens.

### Approaches

1. **Minimal Slice (recommended)** — Move auth DTOs to `shared`, add `AuthApi` + `AuthRepository`, build Login and Register screens with basic `MutableStateFlow` ViewModels, and conditionally render them in `App.kt` (no navigation library). Keep `InMemoryTokenStore`.
   - Pros: Fits inside the 400-line budget; delivers end-to-end working flow immediately; no new dependencies.
   - Cons: Token lost on app restart; `App.kt` conditional rendering does not scale beyond 3–4 screens.
   - Effort: Low

2. **Full Session + Navigation** — Add Compose Navigation dependency, implement persistent `TokenStore` (DataStore on Android, NSUserDefaults/Keychain on iOS), add a splash/session-restore screen, logout, and profile.
   - Pros: Production-ready session handling; scalable screen architecture.
   - Cons: Exceeds 400-line budget; requires platform-specific `expect/actual` work; larger review surface.
   - Effort: High

### Recommendation

**Approach 1 (Minimal Slice)** for the first delivery. It closes the cross-module contract gap and gives users a working login/register flow with minimal risk. Token persistence and navigation should be planned as a second, independent slice once this one is merged.

### Risks

- **Token loss on process death**: `InMemoryTokenStore` means users must log in again after the app is killed. Acceptable for slice 1, but should be the very next improvement.
- **Unstructured backend errors**: The backend returns plain-string bodies (`"Email already registered"`, `"Invalid email or password"`). The frontend will have to display raw server text or map status codes heuristically. Consider aligning error contracts later.
- **No navigation library**: Conditional rendering in `App.kt` is fine for two screens, but will become technical debt if more top-level screens are added before a navigation solution is introduced.
- **Role selection UX**: The backend allows `STUDENT` and `TEACHER` registration, blocking `ADMIN`. The UI must either default to `STUDENT` or expose a role picker. A product decision is needed.

### Product / Business Questions to Answer Before Proposal

1. **Allowed roles for public registration**: Backend blocks `ADMIN`. Should the UI show a role picker (Student / Teacher) or default to `STUDENT`?
2. **Default role**: If no picker, default to `STUDENT`.
3. **Session persistence expectations**: Is in-memory token acceptable for MVP, or must persistence ship in the first slice?
4. **Logout**: Should slice 1 include a logout button (clear token + return to login), or is restart-only acceptable?
5. **Error messages**: Should the app display raw server strings, or do we want a small client-side error mapping layer?
6. **Post-login landing**: Go straight to the existing `CourseScreen`, or is a dashboard/home planned?
7. **Offline behavior**: Not critical for auth itself, but worth noting that login/register require network.

### Ready for Proposal

**Yes — with one blocker**: the product/business questions above (especially role selection and session persistence expectations) should be answered so the proposal can define exact scope. If the user wants the minimal slice as described, we can proceed immediately.
