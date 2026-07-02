# Proposal: Home Dashboard (Inicio Tab)

## Intent

Replace the legacy `CourseScreen` on the HOME tab with an orientation dashboard that greets the user, surfaces light progress, and prompts action.

## Scope

### In Scope
- Create `HomeDashboardScreen` + `HomeDashboardViewModel` in `composeApp`.
- Wire HOME tab in `AuthenticatedHomeScaffold` to the new screen.
- Render greeting, status chip, empty-state "continue learning" card, and catalog CTA.
- Remove or repurpose legacy `CourseScreen` from `App.kt`.

### Out of Scope
- Pixel-perfect visual fidelity to reference image.
- Real date-based streak tracking.
- "Resume lesson" / in-progress entity (renders empty state intentionally).
- New backend endpoints or contract changes.

## Capabilities

### New Capabilities
- `home-dashboard`: Dashboard screen with greeting, progress summary, empty-state card, and navigation CTA.

### Modified Capabilities
- `profile-screen`: Update "Inicio Tab Hosts CourseScreen" requirement to host `HomeDashboardScreen`.
- `frontend-auth`: Update post-auth routing references from `CourseScreen` to the new authenticated landing view.

## Approach

Follow Approach 1 from exploration: create a dedicated screen/VM pair reusing existing repositories (`UserRepository`, `LearnerProfileRepository`, `CourseRepository`). The ViewModel computes greeting, level, and synthetic streak from local progress. The "Continuar aprendiendo" section intentionally renders an empty-state card because no in-progress tracking exists yet. CTAs navigate to the ACTIVITIES tab via `MainRouter.showActivities()`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/ui/AuthenticatedHomeScaffold.kt` | Modified | HOME tab target changed to `HomeDashboardScreen` |
| `composeApp/App.kt` | Modified | Legacy `CourseScreen` removed or repurposed |
| `composeApp/ui/HomeDashboardScreen.kt` | New | Dashboard UI with empty-state card |
| `composeApp/ui/HomeDashboardViewModel.kt` | New | Dashboard state and data aggregation |
| `composeApp/di/AppModule.kt` | Modified | Koin registration for new ViewModel |
| `composeApp/composeResources/drawable/` | New | Empty-state illustration placeholder |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Streak chip misleading users | Low | Label it generically ("Actividad") or show count without "días" |
| Missing illustration asset blocks UI | Low | Use icon-based placeholder initially |
| `CourseScreen` removal breaks previews/tests | Med | Audit previews/tests in `App.kt` before deleting |

## Rollback Plan

Revert `AuthenticatedHomeScaffold` HOME tab back to `CourseScreen`, restore `CourseScreen` in `App.kt` if deleted, and remove the two new dashboard files and Koin registration.

## Dependencies

- `profile-screen`, `course-catalog-discovery`, and `progress-sync` data sources (already implemented).

## Success Criteria

- [ ] HOME tab renders dashboard with greeting and empty-state card.
- [ ] ACTIVITIES tab still renders course catalog unchanged.
- [ ] `composeApp:jvmTest` passes.
- [ ] No new backend contracts or routes required.
