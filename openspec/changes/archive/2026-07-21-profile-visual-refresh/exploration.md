# Exploration: profile-visual-refresh

## Current State

The existing `ProfileScreen` is a gamified learner dashboard: it shows a header with initials avatar + display name + school-year chip, a level/XP progress card, stat cards for streak and completed lessons, an achievements grid, and a logout button. This does **not** match the prepared design.

The prepared design (`docs/ui/screens/profile/design_handoff_perfil/`) defines a **hub-style profile** with:
1. **Identity section** (centered): avatar with edit badge, name, email, role chip, optional streak chip.
2. **Navigation cards** (vertical list): Cuenta, Preferencias, Ayuda y soporte, Acerca de — each with an icon box, title, subtitle, and chevron.
3. **Footer**: logout button + version caption.
4. **Sub-screens**: Cuenta (name/email/password rows + delete-account button), Preferencias (toggles for notifications, sounds, dark mode, language selector). Ayuda/Acerca de follow the same card+row pattern but have no dedicated mockups.

The app currently has **no dark color scheme** and uses the system sans-serif font instead of the design-mandated **Sora** font. The `AppTheme` hardcodes `AppLightColorScheme`.

## Affected Areas

| Path | Why it is affected |
|---|---|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileScreen.kt` | Full visual rewrite to hub + sub-screens layout. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileViewModel.kt` | Needs to expose `email` and `role` (or pass `User`) for the identity section; current `ProfileUiState` omits both. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/ColorTokens.kt` | Design requires dark-mode tokens; currently only light scheme exists. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/TypeTokens.kt` | Design specifies Sora type scale; app uses system sans-serif. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/AppTheme.kt` | Hardcodes light scheme; adding dark scheme support would be touched. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/` | Likely need new primitives: navigation card, toggle row, list row with chevron/label+value, avatar with badge. Existing `MCard` and `MButton` can be reused with adjusted styling. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/ProfileViewModelTest.kt` | If `ProfileUiState` fields change, tests must be updated. |
| `docs/ui/screens/profile/design_handoff_perfil/README.md` + `.dc.html` | Source of truth for the redesign; should be referenced in downstream design/specs. |

## Approaches

### 1. Pure visual restyle of the hub only
Rewrite `ProfileScreen` to the hub layout (identity + nav cards + logout) but do **not** implement any sub-screen composables. Navigation cards are present visually but non-interactive (no-op clicks with TODOs).

- **Pros**: Smallest blast radius, no local navigation logic, no new ViewModel state beyond email/role.
- **Cons**: Does not match the design file fully (missing Cuenta/Preferencias UIs), leaves large visual gap.
- **Effort**: Low–Medium

### 2. Visual restyle + local sub-screen stubs (recommended)
Rewrite `ProfileScreen` as a hub, and add a simple local enum/state machine inside the screen to switch between **Hub**, **Cuenta**, **Preferencias**, **Ayuda**, and **Acerca de** composables. All interactive elements (toggles, delete account, change password) are visually present but stubbed with hardcoded values and TODO comments. Minimal ViewModel changes to expose `email` and `role`.

- **Pros**: Matches the prepared design structure; gives clear placeholders for future behavior; stays within `composeApp` and single file (or small new files in same package).
- **Cons**: Introduces a new intra-tab navigation pattern not used elsewhere; requires new primitive-like composables.
- **Effort**: Medium

### 3. Global theme + font + dark mode first
Before touching `ProfileScreen`, add Sora font loading, define `AppDarkColorScheme`, and update `AppTheme` to support dark mode. Then implement Approach 2.

- **Pros**: Achieves highest design fidelity; benefits entire app.
- **Cons**: Massively expands scope beyond a single-screen visual refresh; font embedding in KMP is non-trivial and must work across Android/iOS/JVM; dark scheme touches every screen implicitly.
- **Effort**: High

## Recommendation

**Approach 2** — implement the hub and all sub-screen UIs visually, using a local state enum for intra-profile navigation, and stub every functional interaction with TODOs. This respects the explicit instruction that "functional behavior is out of scope" while delivering the visual redesign that was actually prepared.

**Boundaries to enforce**:
- Do **not** add Sora font or dark mode support in this change; document them as TODOs / follow-up proposals.
- Do **not** change backend contracts or `shared` models.
- Do **not** modify `MainRouter` or global navigation; keep sub-screen switching local to `ProfileScreen`.
- Minimal ViewModel change: add `email` and `role` to `ProfileUiState` so the identity section can render real data already available in `AuthSession.user`.

## Risks

- **Dark mode / Sora font gap**: The design file treats these as first-class requirements, but the app lacks both infrastructure. Accepting light-only + system font means visual deviation from the handoff.
- **Intra-tab navigation is novel**: No other tab currently has nested screens. A simple enum switch is fine, but engineers must not later try to push these onto the global `MainRouter` (which is tab-level only).
- **Test impact**: `ProfileViewModelTest` asserts on existing `ProfileUiState` fields. Adding fields is safe; removing/renaming old fields (level, XP, achievements) would break tests. Since this is visual-only, keep old fields in state but stop rendering them, or decide explicitly to remove them and update tests.
- **Scope creep**: The design includes destructive actions (delete account) and preference toggles. It is easy to accidentally start wiring real behavior. Strict TODO discipline is required.

## Ready for Proposal

**Yes.** The design reference is clear, the current implementation is well-understood, and the scope boundary (visual-only, local to `ProfileScreen` + minimal ViewModel) is identifiable. The orchestrator can tell the user:

> "The exploration confirms the prepared design is ready to implement. The current profile screen is a dashboard that needs to become a hub with sub-screens. The safest path is Approach 2: build the visual hub and sub-screens locally inside `ProfileScreen`, stub all interactions with TODOs, and defer dark mode + custom font to a follow-up proposal. This stays within `composeApp/commonMain` and avoids touching global navigation or backend contracts."

## TODOs for Future Behavior (identified during exploration)

1. **Dark mode support**: Add `AppDarkColorScheme` and wire into `AppTheme`; design tokens are fully specified in the handoff.
2. **Sora font**: Load Sora via Compose Multiplatform font resources and update `TypeTokens`.
3. **Avatar image picker**: The edit badge on the avatar is visual-only; actual image selection requires platform-specific file picker integration.
4. **Cuenta sub-screen functionality**: Edit name/email, change password, and delete account require backend endpoints and confirmation dialogs.
5. **Preferencias sub-screen functionality**: Toggles for notifications, sounds, and dark mode need persistent settings store (SQLDelight or DataStore). Language selector needs supported-locale list and in-app locale switching.
6. **Ayuda y soporte / Acerca de**: No mockups exist; when functionality is needed, design and content must be provided.
7. **App version caption**: Needs a way to read the app version string at runtime (BuildConfig or expect/actual).
