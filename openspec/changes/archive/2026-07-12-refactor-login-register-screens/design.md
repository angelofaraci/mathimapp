# Design: Refactor Login and Register Screens

## Technical Approach

Implement the `frontend-auth` delta entirely in `composeApp/commonMain`. Extend the existing shared primitives with opt-in auth presentation, then rebuild the two Compose screens around their current `StateFlow` ViewModels. The existing `AuthRepository`/`AuthApi` path, session mutation, and `AuthGateRouter` stay unchanged; only the register wizard owns intra-registration steps.

## Architecture Decisions

| Option | Tradeoff | Decision |
|---|---|---|
| Put wizard steps in `AuthGateRouter` | Makes global routing aware of form internals | Keep `step: Int` (1..3) in `RegisterUiState`; router remains Login/Register-only. |
| Add auth-specific controls | More files and APIs | Extend `MTextField` and `MButton` with opt-in auth styling so existing search fields retain their appearance. |
| Change auth/backend contracts | Unnecessary integration risk | Preserve `AuthRepository.register(name, email, password)` and the student-only server assignment exactly. |
| Implement OAuth or recovery flow | Requires unavailable backend/routes | Render social buttons without callbacks; show a Spanish recovery placeholder from the Login screen. |
| Add dark theme or bundled font | Cross-app visual scope | Use the existing light `AppTheme` and typography; no theme or localization-system changes. |

## Data Flow

```text
LoginScreen -> LoginViewModel -> AuthRepository.login -> AuthApi -> /auth/login
RegisterScreen -> RegisterViewModel (steps 1..3) -> AuthRepository.register -> /auth/register
                                             |                         |
                                             +-- field/step errors      +-- TokenStore + session
                                                                            |
AuthRepository.session ------------------------------------------------> App.kt auth gate
```

Registration partitions the unchanged payload as step 1: `name`; step 2: `email` and `password`; step 3: terms acceptance and submit. Back from steps 2/3 decrements the step without clearing fields. Back from step 1 calls the existing Login router callback after the ViewModel resets its state.

## File Changes

| File | Action | Description |
|---|---|---|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MTextField.kt` | Modify | Add opt-in 15.dp auth shape, focus border/glow, and preserve leading/trailing slots. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MButton.kt` | Modify | Apply ~0.5 disabled opacity and add a surface/outline social style. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthScreenScaffold.kt` | Modify | Replace the centered-card composition with the scrollable, top-aligned auth layout while retaining the logo resource. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginScreen.kt` | Modify | Render Spanish hero/form copy, icons, visibility control, recovery placeholder, inert social buttons, and register footer. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginViewModel.kt` | Modify | Add visibility and email-validity state; block invalid login before repository invocation. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterScreen.kt` | Modify | Render step indicator, step-specific fields, back behavior, password meter, and terms control. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterViewModel.kt` | Modify | Model step transitions, field errors, visibility, deterministic strength, terms gating, and state reset. |
| `composeApp/src/commonMain/composeResources/drawable/google_logo.xml` | Create | Google social-button vector asset. |
| `composeApp/src/commonMain/composeResources/drawable/apple_logo.xml` | Create | Apple social-button vector asset. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/LoginViewModelTest.kt` | Modify | Cover email validation, visibility, invalid-call prevention, and existing success/failure semantics. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/RegisterViewModelTest.kt` | Modify | Cover step validation/navigation, preserved fields, strength, terms gate, reset, and unchanged request payload. |

## Interfaces / Contracts

```kotlin
data class LoginUiState(
    val email: String = "", val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null, val errorMessage: String? = null,
    val isLoading: Boolean = false
)

data class RegisterUiState(
    val step: Int = 1, val name: String = "", val email: String = "", val password: String = "",
    val isPasswordVisible: Boolean = false, val acceptedTerms: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.EMPTY,
    val fieldErrors: Map<RegisterField, String> = emptyMap(),
    val errorMessage: String? = null, val isLoading: Boolean = false
)
```

`PasswordStrength` has three display levels derived deterministically from length plus character-class variety. ViewModels expose `continueStep()`, `goBack()`, visibility/terms setters, and retain `login()`/`register()` for the final repository call. User-facing validation and auth copy are Spanish literals local to this feature.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Login validity/visibility; wizard transitions, preservation, strength, terms, and payload | Extend existing `commonTest` fake-repository ViewModel tests. |
| Integration | Existing token/session and post-auth routing behavior | Retain current repository/routing tests; verify registration still submits only name/email/password. |
| Visual/build | Primitives, inert social controls, Spanish layout on supported targets | Manual UI check; run `./gradlew :composeApp:jvmTest` and `./gradlew :composeApp:assembleDebug`. |

## Migration / Rollout

No migration required. Deliver as chained review slices: primitives/assets, login, then register/tests. No API, database, shared-model, or feature-flag changes are needed.

## Open Questions

- [ ] Confirm the exact Spanish recovery-placeholder copy; no recovery route currently exists.
