# Tasks: Refactor Login and Register Screens

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 600-800 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (primitives) → PR 2 (login) → PR 3 (register + tests) |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Auth primitives + drawable assets | PR 1 | Base: main; no upstream deps |
| 2 | Login screen + ViewModel + scaffold | PR 2 | Base: main after PR 1 merges |
| 3 | Register wizard + all ViewModel tests | PR 3 | Base: main after PR 2 merges |

## Phase 1: Foundation (Primitives + Assets)

- [x] 1.1 Modify `MTextField.kt` — add focus glow, 15.dp corner radius, leading/trailing icon slots
- [x] 1.2 Modify `MButton.kt` — add ~0.5 disabled opacity and surface/outline social-button style
- [x] 1.3 Create `google_logo.xml` and `apple_logo.xml` vector drawables in `composeResources/drawable/`

## Phase 2: Login Screen

- [x] 2.1 Rewrite `AuthScreenScaffold.kt` — top-aligned scrollable layout replacing centered card
- [x] 2.2 Update `LoginViewModel.kt` — add `isPasswordVisible`, `emailError`, email-format validation; block login on invalid
- [x] 2.3 Rewrite `LoginScreen.kt` — Spanish copy, brand hero, field icons, visibility toggle, recovery placeholder, inert social buttons, register footer link

## Phase 3: Register Wizard

- [x] 3.1 Update `RegisterViewModel.kt` — step state 1..3, per-step validation, password visibility, `passwordStrength`, `acceptedTerms`, state reset on back-to-login
- [x] 3.2 Rewrite `RegisterScreen.kt` — 3-step wizard with step indicator, back nav from steps 2/3 preserving data, back from step 1 to login, password meter, terms checkbox

## Phase 4: Testing

- [x] 4.1 Update `LoginViewModelTest.kt` — cover email validation, visibility toggle, invalid-call rejection, existing success/failure semantics
- [x] 4.2 Update `RegisterViewModelTest.kt` — cover step validation/navigation, field preservation on back, password strength, terms gate, state reset, unchanged register payload
- [x] 4.3 Run `./gradlew :composeApp:jvmTest` and verify all tests pass
