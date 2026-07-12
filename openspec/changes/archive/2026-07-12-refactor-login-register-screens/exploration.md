## Exploration: Refactor Login and Register Screens

### Current State

The app currently has a minimal auth UI:

- **LoginScreen** (`composeApp/.../ui/LoginScreen.kt`): single-screen email+password form wrapped in `AuthScreenScaffold`. Uses `MTextField` and `MButton` primitives. English copy. No password visibility toggle, no social login, no forgot-password link.
- **RegisterScreen** (`composeApp/.../ui/RegisterScreen.kt`): single-screen form with name, email, and password stacked vertically. Same primitives. No step flow, no progress indicator, no password strength meter, no terms checkbox.
- **ViewModels**: `LoginViewModel` and `RegisterViewModel` hold simple `StateFlow` state with basic validation (blank check only) and repository calls. No field-level validation, no step management.
- **Theme**: Only a light color scheme (`AppLightColorScheme`) exists. No dark mode. Typography uses system sans-serif; the design specifies **Sora**. Shape tokens exist but differ from design radii.
- **App.kt**: `AuthGate` toggles between `LOGIN` and `REGISTER` via `AuthGateRouter`. No step-based register navigation.

### Affected Areas

| File | Why affected |
|------|-------------|
| `composeApp/.../ui/LoginScreen.kt` | Complete layout rewrite to match hifi design: brand hero, field icons, forgot-password link, social buttons, footer link |
| `composeApp/.../ui/RegisterScreen.kt` | Convert from single form to 3-step wizard with back navigation, step header, progress bar |
| `composeApp/.../ui/LoginViewModel.kt` | Add `passwordVisible` toggle; possibly add email-format validation |
| `composeApp/.../ui/RegisterViewModel.kt` | Add `registerStep` (1..3), `passwordVisible`, `acceptedTerms`, `passwordStrength`, per-step validation logic |
| `composeApp/.../ui/AuthScreenScaffold.kt` | Design uses a different structural layout (top-aligned, not centered card). Likely replace or heavily modify |
| `composeApp/.../ui/primitives/MTextField.kt` | Design needs focus glow (`0 6px 18px -10px rgba(coral,0.42)`), 15px radius, custom border widths, leading/trailing icons |
| `composeApp/.../ui/primitives/MButton.kt` | Needs disabled opacity (~0.5), social-button style (surface + outline), possibly shadow support for primary CTA |
| `composeApp/.../ui/theme/ColorTokens.kt` | Need dark color scheme (`#16181D` bg, `#1F232B` surface, etc.) and additional tokens (`app-bg2`, `surface2`, `line`) |
| `composeApp/.../ui/theme/AppTheme.kt` | Need to support dynamic light/dark selection |
| `composeApp/.../ui/theme/TypeTokens.kt` | Sora font family; specific sizes (27px/800 title, 25px/800 step question) not in current scale |
| `composeApp/.../App.kt` | Register navigation may need step-level awareness; transition animations could be added here |
| `composeApp/src/commonTest/.../LoginViewModelTest.kt` | Update for any new VM state/behavior |
| `composeApp/src/commonTest/.../RegisterViewModelTest.kt` | Significant updates for step flow, terms, strength, per-step validation |
| `composeApp/src/commonMain/composeResources/` | Likely new drawable resources: Google logo, Apple logo, possibly Sora font files |

### Approaches

#### 1. Component-First Refactor
Extract reusable auth components before touching screens.

- **Pros**: Clean boundaries; new primitives (AuthField, SocialButton, StepIndicator, SegmentedProgress) can be reused elsewhere; easier to test in isolation; reviewable in slices.
- **Cons**: More upfront files; requires deciding on component API before screen work.
- **Effort**: Medium

#### 2. Screen-by-Screen Rewrite
Rewrite `LoginScreen` fully, then rewrite `RegisterScreen` and its ViewModel, then backfill theme/primitive gaps.

- **Pros**: Directly targets the design handoff; each screen is independently reviewable; clear definition of done per screen.
- **Cons**: May duplicate temporary glue code; theme gaps discovered late could force re-touching earlier screens.
- **Effort**: Medium

#### 3. Theme + Primitives First, Then Screens
Add dark mode, new color tokens, typography, and enhanced `MTextField`/`MButton` first. Then rebuild screens on the new foundation.

- **Pros**: Screens are built once on the correct foundation; avoids rework; dark mode is a standalone deliverable.
- **Cons**: Largest initial diff; screens remain broken/old until the end; higher risk if design details change during implementation.
- **Effort**: High

### Recommendation

**Approach 2 (Screen-by-Screen Rewrite) with a thin primitives pass first.**

Reasoning: The design is a complete visual overhaul, not a minor tweak. Rewriting each screen independently lets us validate against the HTML prototype quickly. A thin pass on `MTextField` and `MButton` (focus glow, disabled opacity, social style) provides just enough foundation without boiling the ocean. Dark mode can follow as a second slice after login/register screens are solid in light mode.

Suggested order:
1. Enhance `MTextField` + `MButton` (minimal, just what's needed for auth)
2. Rewrite `LoginScreen` + `LoginViewModel` (simpler, establishes patterns)
3. Rewrite `RegisterScreen` + `RegisterViewModel` (3-step wizard, more complex)
4. Add dark color scheme + `AppTheme` support (cross-cutting but contained)
5. Add remaining assets (fonts, logos)
6. Update tests

### Risks

- **Review budget (400 lines)**: This is a large visual refactor. A single PR will almost certainly exceed the budget. Plan for **chained PRs** or a feature branch with child PRs per screen/theme slice.
- **Social login (Google/Apple)**: The design shows social buttons, but the current backend/client may not have OAuth wired. Verify `AuthApi.kt` / `AuthRepository.kt` before implementing UI triggers.
- **Font (Sora)**: KMP font packaging is non-trivial across Android/iOS/Desktop. The design says "Sora como fuente descargable/empaquetada. Fallback sans-serif." Starting with system sans-serif and adding Sora later is a safe fallback.
- **Spanish copy**: The handoff uses Spanish copy ("Hola de nuevo", "Creá tu cuenta"). The current screens are English. Align with product on localization strategy before switching language.
- **Dark mode**: No dark scheme exists today. Adding it touches `AppTheme`, all color usages, and may reveal unreadable contrast in non-auth screens. Scope dark mode to auth first, or accept a broader audit.
- **3-step register navigation**: `AuthGateRouter` currently only knows `LOGIN`/`REGISTER`. Step transitions can live entirely inside `RegisterScreen`/`RegisterViewModel` without changing the router, but back-from-step-1-to-login must be handled cleanly.
- **Password strength algorithm**: The design shows a 3-segment meter. Need a simple heuristic (length + variety) or delegate to a library. Keep it client-side and deterministic.

### Ready for Proposal

**Yes**, but with one blocker to resolve first:

The orchestrator should ask the user to confirm:
1. **Language**: Should the auth screens switch to Spanish (design handoff) or stay in English (current codebase)?
2. **Social login**: Are Google/Apple OAuth flows already implemented in `AuthRepository` / backend, or should the social buttons be UI-only placeholders for now?
3. **Dark mode scope**: Auth-only dark mode first, or full-app dark mode as part of this change?

Once clarified, proceed to `sdd-propose`.
