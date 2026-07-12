# Apply Progress: Refactor Login and Register Screens

## Status

**Mode:** Standard (strict TDD disabled in `openspec/config.yaml`)

**Cumulative completion:** 11/11 tasks complete

## Completed Tasks

- [x] 1.1 Modify `MTextField.kt` — add focus glow, 15.dp corner radius, leading/trailing icon slots
- [x] 1.2 Modify `MButton.kt` — add ~0.5 disabled opacity and surface/outline social-button style
- [x] 1.3 Create `google_logo.xml` and `apple_logo.xml` vector drawables in `composeResources/drawable/`
- [x] 2.1 Rewrite `AuthScreenScaffold.kt` — top-aligned scrollable layout replacing centered card
- [x] 2.2 Update `LoginViewModel.kt` — add `isPasswordVisible`, `emailError`, email-format validation; block login on invalid
- [x] 2.3 Rewrite `LoginScreen.kt` — Spanish copy, brand hero, field icons, visibility toggle, recovery placeholder, inert social buttons, register footer link
- [x] 3.1 Update `RegisterViewModel.kt` — step state 1..3, per-step validation, password visibility, `passwordStrength`, `acceptedTerms`, state reset on back-to-login
- [x] 3.2 Rewrite `RegisterScreen.kt` — 3-step wizard with step indicator, back nav from steps 2/3 preserving data, back from step 1 to login, password meter, terms checkbox
- [x] 4.1 Update `LoginViewModelTest.kt` — cover email validation, visibility toggle, invalid-call rejection, existing success/failure semantics
- [x] 4.2 Update `RegisterViewModelTest.kt` — cover step validation/navigation, field preservation on back, password strength, terms gate, state reset, unchanged register payload
- [x] 4.3 Run `./gradlew :composeApp:jvmTest` and verify all tests pass

The task checkboxes in `tasks.md` were re-read and match this cumulative completion state.

## Files Changed — Slice 1

| File | Action | What Was Done |
| --- | --- | --- |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MTextField.kt` | Modified | Added opt-in auth styling with a 15.dp shape and focus glow while retaining leading/trailing icon slots. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MButton.kt` | Modified | Added disabled opacity and `Social` surface/outline button styling. |
| `composeApp/src/commonMain/composeResources/drawable/google_logo.xml` | Created | Added Google social-button vector drawable. |
| `composeApp/src/commonMain/composeResources/drawable/apple_logo.xml` | Created | Added Apple social-button vector drawable. |

## Files Changed — Slice 2

| File | Action | What Was Done |
| --- | --- | --- |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthScreenScaffold.kt` | Modified | Replaced the centered card with a top-aligned, scrollable light auth layout and compact brand header. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginViewModel.kt` | Modified | Added password visibility state plus non-blank email-format validation without changing required-field or repository behavior. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginScreen.kt` | Modified | Added Spanish login UX, field and visibility icons, recovery placeholder, inert social controls, and the registration footer link. |

## Files Changed — Slice 3

| File | Action | What Was Done |
| --- | --- | --- |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterViewModel.kt` | Modified | Defined wizard state and explicit field/strength enums; added 1..3 transitions, validation, deterministic strength, visibility, terms gating, preserved back navigation, reset, and unchanged registration payload. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterScreen.kt` | Modified | Rebuilt registration as a Spanish three-step wizard with indicator, safe back behavior, password visibility/meter, and terms acceptance. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/LoginViewModelTest.kt` | Modified | Added email-validation/invalid-call and password-visibility coverage while retaining success/failure tests. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/RegisterViewModelTest.kt` | Modified | Added coverage for wizard validation/navigation, state preservation/reset, deterministic strength, terms gating, payload, and repository failure. |

## Wizard State and Validation Contract

- `step` is constrained by transitions to 1..3: step 1 collects `name`; step 2 collects `email` and `password`; step 3 requires accepted terms before it can submit.
- `continueStep()` validates only the active step and never advances on errors. Steps 1 and 2 show field-specific Spanish errors; step 3 is gated by terms acceptance.
- `goBack()` decrements from steps 2/3 without clearing inputs. At step 1 it resets `RegisterUiState` and returns `true` so the screen navigates to Login.
- Password strength is display-only: empty, weak, medium (6+ characters and 2 character classes), or strong (8+ characters and 3 character classes). It does not alter the existing registration payload or block submission.
- `register()` retains the pre-wizard generic required-fields error for incomplete direct submissions, then sends only trimmed `name`, trimmed `email`, and unchanged `password` after terms acceptance.

## Verification Evidence

Slice 1:

| Command | Result |
| --- | --- |
| `./gradlew :composeApp:jvmTest` | Passed |
| `./gradlew :composeApp:assembleDebug` | Passed |

Slice 2:

| Command | Result |
| --- | --- |
| `./gradlew :composeApp:jvmTest` | Passed |

Slice 3:

| Command | Result |
| --- | --- |
| `./gradlew :composeApp:jvmTest` | Passed (2026-07-12) |

## Deviations from Design

None — implementation matches the planned design scope. The review-ledger warnings were treated as informational: explicit `RegisterField` and `PasswordStrength` enums were defined, and the pre-existing generic required-fields error remains intact.

## Issues Found

None. The test task emits existing Kotlin beta warnings for `expect`/`actual` classes in `DatabaseDriverFactory`; they are unrelated to this slice and do not fail the build.

## Remaining Tasks

None.

## Workload / PR Boundary

- **Mode:** Stacked PR slice
- **Current work unit:** Slice 3 — register wizard and ViewModel tests
- **Boundary:** Starts after the merged primitives/login slices and ends with the complete registration presentation, state behavior, and auth ViewModel verification. It targets `main` after prior slices merge.
- **Estimated review budget impact:** Final autonomous slice; tests remain paired with the wizard behavior they verify. The full change remains split because the forecast is 600–800 changed lines.

## Next Recommended

Run `sdd-verify`; all implementation and executor verification tasks are complete.
