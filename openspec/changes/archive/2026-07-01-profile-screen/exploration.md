# Exploration: Profile Screen

### Current State

The app currently uses a top-level `AuthGate` (`App.kt`) that switches between four views: `LOGIN`, `REGISTER`, `ONBOARDING`, and `COURSE`. Once authenticated and onboarded, the user lands on `CourseScreen` — a simple course list with a logout button. There is **no bottom navigation**, **no profile UI**, and **no gamification data models** beyond a basic `totalScore` in `UserProgress`.

The reference asset `docs/ui/screens/perfil-usuario.png` depicts a full "Mi Perfil" screen containing:
- A profile card with avatar, name, level badge, and XP progress bar.
- Summary stats (streak days, completed lessons).
- An achievements grid (Campeón, Brillante, Racha, Leído).
- A bottom navigation bar with four tabs: Inicio, Actividades, Progreso, Perfil.

None of these UI patterns or data concepts exist in the codebase today.

### Affected Areas

- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` — needs authenticated-area scaffold with bottom nav and tab routing.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileScreen.kt` — new screen matching the reference image layout.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileViewModel.kt` — new ViewModel to load `User`, `UserProgress`, and `LearnerProfile` and derive gamification metrics.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` — register new ViewModel with Koin.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthGateRouter.kt` — may need an authenticated-area router, or a new `MainRouter` for bottom-nav tabs.
- `composeApp/src/commonMain/composeResources/` — may need vector icons for bottom navigation and achievement placeholders.
- `docs/ui/screens/perfil-usuario.png` — reference asset that drives the visual layout.

### Approaches

1. **Minimal Profile Screen (local-only, no bottom nav)**
   - Add a `ProfileScreen` accessible via a button from `CourseScreen`.
   - Use existing `User` + `UserProgress` + `LearnerProfile` data only.
   - Skip achievements, level, streak, and XP UI elements.
   - **Pros**: Very small change, no backend/shared impact, fits current architecture.
   - **Cons**: Does not match the reference image at all; lacks gamification UI.
   - **Effort**: Low

2. **Profile Screen + Bottom Navigation + Backend Gamification**
   - Add bottom nav with four tabs.
   - Introduce `Achievement`, `Streak`, `Level/XpProgress` models in `shared`.
   - Extend backend tables, `UserService`, and `UserRepository` for real gamification data.
   - Add SQLDelight schema updates and Flyway migrations.
   - **Pros**: Fully aligned with the reference image; real cross-device gamification.
   - **Cons**: Touches all modules (`composeApp`, `shared`, `server`); requires DB migrations, backend tests, and client tests; far exceeds the 400-line review budget.
   - **Effort**: High

3. **Profile Screen + Bottom Navigation + Derived Gamification (Recommended)**
   - Add bottom nav in `composeApp` with tabs: Inicio, Actividades, Progreso, Perfil.
   - Implement `ProfileScreen` using existing `User`, `UserProgress`, and `LearnerProfile`.
   - Derive gamification metrics locally (e.g., `level = totalScore / 100`, `streak = computed from consecutive completed lessons`, achievements unlocked from progress thresholds).
   - Placeholder or lightweight screens for the other three tabs.
   - **Pros**: Matches the reference image UI structure; changes stay localized to `composeApp`; no backend work; no SQLDelight migrations; reviewable in a single PR slice.
   - **Cons**: Gamification data is client-side heuristic; not synced across devices; backend is unaware of levels/achievements.
   - **Effort**: Medium

### Recommendation

**Adopt Approach 3.** It delivers the profile screen and bottom navigation structure consistent with the reference image while keeping the change inside `composeApp` per module-ownership rules. Existing `UserProgress.totalScore` and `completedLessonIds` provide enough raw material to derive believable level, XP, and achievement UI. If real cross-device gamification becomes a requirement later, it can be promoted to a dedicated backend change.

### Risks

- The `User` model has no `avatarUrl`; a placeholder avatar (initials or generic icon) is needed.
- `totalScore` may not map cleanly to a linear XP curve; the derivation logic must be documented.
- Bottom navigation is a new pattern in the app; the existing `AuthGate` full-screen switch must be replaced by a `Scaffold` with `NavigationBar` for the authenticated area.
- The "Actividades" and "Progreso" tabs need at least placeholder screens to avoid dead UI.

### Ready for Proposal

Yes. The orchestrator should tell the user that Approach 3 is recommended (client-only profile + bottom nav with derived gamification) to keep the change localized and reviewable. If the user insists on full backend gamification, that should be split into a separate, follow-up change.
