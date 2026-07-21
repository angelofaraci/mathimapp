# Proposal: Profile Visual Refresh

## Intent

Replace the gamified `ProfileScreen` dashboard with a hub-style profile that matches the prepared design handoff (`docs/ui/screens/profile/design_handoff_perfil/`). The current screen shows level/XP/achievements; the design defines an identity section plus navigation cards to Cuenta, Preferencias, Ayuda, and Acerca de.

## Scope

### In Scope
- Hub layout: avatar with edit badge, name, email, role chip, streak chip, navigation cards, logout, version caption.
- Local intra-profile navigation enum to switch between Hub, Cuenta, Preferencias, Ayuda, and Acerca de.
- Stubbed sub-screen UIs with hardcoded values and explicit TODO comments.
- Minimal `ProfileUiState` additions (`email`, `role`) without removing existing gamification fields.
- New reusable primitives in `ui/primitives/` (navigation card, toggle row, list row with chevron).

### Out of Scope
- Dark mode, custom Sora font, avatar picker, real account actions, preference persistence, app version resolution, backend changes, global navigation changes.

## Capabilities

### New Capabilities
- `profile-hub-navigation`: Local enum-driven navigation within `ProfileScreen` and stubbed composables for Cuenta, Preferencias, Ayuda y soporte, and Acerca de.

### Modified Capabilities
- `profile-screen`: Replace gamified dashboard layout requirement with hub + sub-screens layout; preserve bottom nav shell and logout behavior.

## Approach

Implement Approach 2 from exploration: rewrite `ProfileScreen` as a hub, add a local `ProfileSubScreen` enum and `AnimatedContent` switcher, build stub composables for each sub-screen, and expose `email`/`role` from `AuthSession.user` into `ProfileUiState`. Keep all functional interactions as no-op TODOs.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `composeApp/.../ui/ProfileScreen.kt` | Modified | Full visual rewrite to hub + sub-screens. |
| `composeApp/.../ui/ProfileViewModel.kt` | Modified | Add `email` and `role` to `ProfileUiState`. |
| `composeApp/.../ui/primitives/` | New | Navigation card, toggle row, list row primitives. |
| `composeApp/.../ui/ProfileViewModelTest.kt` | Modified | Update assertions for new state fields. |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Scope creep into real behavior | Med | Strict TODO discipline on every stubbed interaction. |
| Visual gap without dark mode/Sora | High | Document as follow-up proposals; accept system font + light scheme. |
| Intra-tab nav pattern is novel | Low | Keep enum switch local to `ProfileScreen`; do not push to global router. |
| Test breakage if old fields removed | Low | Keep existing `ProfileUiState` fields; only add new ones. |

## Rollback Plan

Revert the implementing commit. Because the old dashboard code is fully replaced, maintain a backup branch from `main` before applying so the original `ProfileScreen` composable can be restored instantly if needed.

## Dependencies

None.

## Success Criteria

- [ ] Hub identity section and all four navigation cards render per design handoff.
- [ ] Each sub-screen (Cuenta, Preferencias, Ayuda, Acerca de) renders stubbed UI with TODOs.
- [ ] `ProfileViewModelTest` passes with updated state fields.
- [ ] No functional regressions in logout or bottom nav behavior.
