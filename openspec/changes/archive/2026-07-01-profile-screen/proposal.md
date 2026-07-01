# Proposal: Profile Screen with Bottom Navigation

## Intent

Add a "Mi Perfil" screen and bottom navigation, matching the reference image `docs/ui/screens/perfil-usuario.png`. Keep all changes inside `composeApp`; derive gamification metrics from existing local data to avoid backend work.

## Scope

### In Scope
- `ProfileScreen` composable: avatar placeholder, name, level badge, XP progress bar, stats (streak, completed lessons), achievements grid
- Bottom navigation with four tabs: Inicio, Actividades, Progreso, Perfil
- `ProfileViewModel` deriving level, XP, streak, and achievements from `User`, `UserProgress`, and `LearnerProfile`
- Placeholder screens for Actividades and Progreso tabs
- `AuthGate` scaffold replacement for the authenticated area

### Out of Scope
- Backend gamification models, APIs, or database tables
- Cross-device sync for levels, achievements, or streaks
- Real avatar upload or URL storage
- SQLDelight schema changes

## Capabilities

### New Capabilities
- `profile-screen`: UI, ViewModel, and client-side gamification derivation for the user profile tab

### Modified Capabilities
- None (auth entry flow behavior stays the same; `frontend-auth` spec is unaffected at requirement level)

## Approach

Replace `AuthGate`'s direct `CourseScreen` with a `Scaffold` + `NavigationBar` for the authenticated area. Derive level from `totalScore`, use an activity streak proxy from `completedLessonIds` capped at 7, and evaluate achievements from progress thresholds. Use existing repositories; no new backend contracts.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/App.kt` | Modified | AuthGate renders scaffold with bottom nav for authenticated area |
| `composeApp/ui/ProfileScreen.kt` | New | Profile UI matching reference image |
| `composeApp/ui/ProfileViewModel.kt` | New | Derives gamification from local data |
| `composeApp/ui/MainRouter.kt` | New | Bottom-nav tab routing inside authenticated area |
| `composeApp/di/AppModule.kt` | Modified | Registers ProfileViewModel |
| `composeApp/composeResources/` | New | Vector icons for bottom nav and achievement placeholders |
| `composeApp/ui/PlaceholderScreen.kt` | New | Lightweight screens for Actividades and Progreso tabs |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| User model has no `avatarUrl` | Certain | Placeholder avatar using initials or generic icon |
| `totalScore` does not map cleanly to XP | Med | Document heuristic formula (e.g., `level = totalScore / 100`) |
| Bottom nav is a new pattern in app | Low | Keep `AuthGate` logic intact; only replace the authenticated-area renderer |

## Rollback Plan

Revert `App.kt` to render `CourseScreen` directly in `AuthView.COURSE`. Delete all new UI, ViewModel, and router files. No database or backend revert needed.

## Dependencies

None.

## Success Criteria

- [ ] ProfileScreen matches reference image layout (avatar card, stats, achievements)
- [ ] Bottom nav switches between four tabs without crashes
- [ ] Gamification metrics derive believable values from existing `UserProgress`
- [ ] `composeApp:jvmTest` passes after change
- [ ] No changes required in `shared` or `server` modules

## Assumptions

- Avatar is a placeholder because `User` has no `avatarUrl`; initials or a generic vector icon is acceptable.
- Gamification is client-derived heuristic only; it will not sync across devices and can be promoted to a backend change later if required.
- The "Inicio" tab will host the existing `CourseScreen` content.
- "Actividades" and "Progreso" tabs receive minimal placeholder UI to avoid dead navigation.
