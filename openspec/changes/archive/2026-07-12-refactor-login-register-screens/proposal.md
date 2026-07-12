# Proposal: Refactor Login and Register Screens

## Intent

Refactor the auth screens to match the design handoff: Spanish copy, a polished login layout, and a 3-step registration wizard. All existing auth behavior—token storage, post-auth routing, error surfacing, student role assignment, and the registration data contract—must remain unchanged.

## Scope

### In Scope
- Rewrite `LoginScreen` with Spanish copy, brand hero, field icons, forgot-password link, visual-only social buttons, and footer link.
- Rewrite `RegisterScreen` as a 3-step wizard with step indicators, back navigation, and per-step validation.
- Enhance `MTextField` (focus glow, 15px radius, leading/trailing icons) and `MButton` (disabled opacity, social-button style).
- Update `LoginViewModel` (password visibility toggle, email-format validation) and `RegisterViewModel` (step state 1..3, password visibility, accepted terms, password strength, per-step validation).
- Update `AuthScreenScaffold` to match the top-aligned design layout.
- Add drawable resources (Google logo, Apple logo) to `composeResources`.
- Update `LoginViewModelTest` and `RegisterViewModelTest` for new state/behavior.

### Out of Scope
- Dark mode (explicitly deferred).
- Social authentication wiring (Google/Apple buttons are visual-only placeholders).
- Localization of non-auth screens.
- Backend, API contract, or shared model changes.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `frontend-auth`: Screen UX and validation behavior are changing. The register form becomes a 3-step wizard with step-level validation, password visibility toggle, password strength indicator, and explicit terms acceptance. Login gains email-format validation and password visibility. Post-auth routing, token handling, error surfacing, and role assignment requirements remain unchanged.

## Approach

Screen-by-screen rewrite with a thin primitives pass first.

1. Enhance `MTextField` and `MButton` just enough for the auth designs.
2. Rewrite `LoginScreen` + `LoginViewModel` (simpler; establishes patterns).
3. Rewrite `RegisterScreen` + `RegisterViewModel` (3-step wizard; more complex).
4. Update `AuthScreenScaffold` layout.
5. Add drawable assets.
6. Update tests.

Because the default 400-line review budget will likely be exceeded, plan chained PRs per deliverable slice.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/kotlin/.../ui/LoginScreen.kt` | Modified | Full layout rewrite to match hifi design |
| `composeApp/src/commonMain/kotlin/.../ui/RegisterScreen.kt` | Modified | Convert single form to 3-step wizard |
| `composeApp/src/commonMain/kotlin/.../ui/LoginViewModel.kt` | Modified | Add password visibility, email validation |
| `composeApp/src/commonMain/kotlin/.../ui/RegisterViewModel.kt` | Modified | Add step management, terms, strength, per-step validation |
| `composeApp/src/commonMain/kotlin/.../ui/AuthScreenScaffold.kt` | Modified | Top-aligned layout instead of centered card |
| `composeApp/src/commonMain/kotlin/.../ui/primitives/MTextField.kt` | Modified | Focus glow, radius, leading/trailing icons |
| `composeApp/src/commonMain/kotlin/.../ui/primitives/MButton.kt` | Modified | Disabled opacity, social-button style |
| `composeApp/src/commonMain/composeResources/` | New | Google/Apple logo drawables |
| `composeApp/src/commonTest/.../LoginViewModelTest.kt` | Modified | Update for new VM state |
| `composeApp/src/commonTest/.../RegisterViewModelTest.kt` | Modified | Update for step flow and new state |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Exceeds 400-line review budget | High | Use chained PRs per screen/primitive slice |
| Users expect social buttons to work | Med | Keep buttons disabled or label as upcoming; no wiring |
| Password strength heuristic feels wrong | Low | Simple client-side rule (length + variety); defer library |
| Step-1 back navigation ambiguity | Low | Back from step 1 returns to login; document in spec |

## Rollback Plan

Revert to the previous commit. The change is confined to UI and ViewModels inside `composeApp`; no database migrations, API contracts, or shared models are touched.

## Dependencies

None.

## Success Criteria

- [ ] LoginScreen matches hifi design with Spanish copy and all new interactive elements.
- [ ] RegisterScreen implements a 3-step wizard with step indicators, back navigation, and per-step validation.
- [ ] All existing registration data fields are collected and submitted to the backend unchanged.
- [ ] All existing auth behavior is preserved: token storage, post-auth routing, error surfacing, student role.
- [ ] Google/Apple buttons are visible but non-functional (no OAuth wiring).
- [ ] `./gradlew :composeApp:jvmTest` passes.
- [ ] `./gradlew :composeApp:assembleDebug` succeeds.
