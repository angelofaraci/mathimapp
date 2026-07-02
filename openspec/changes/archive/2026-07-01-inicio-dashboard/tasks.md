# Tasks: Home Dashboard (Inicio Tab)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~300-350 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

```
Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium
```

## Phase 1: Foundation — ViewModel & State

- [x] 1.1 Create `ui/home/HomeDashboardViewModel.kt` with `HomeDashboardUiState`, inject `AuthRepository`, `UserRepository`, `LearnerProfileRepository`
- [x] 1.2 Implement greeting derivation (`salutation(now)` + displayName fallback), level math (`totalScore / 100`), activity cap (`min(completedLessonIds.size, 7)`), and `openActivities()` callback

## Phase 2: UI Screen

- [x] 2.1 Create `ui/home/HomeDashboardScreen.kt` accepting `router: MainRouter` and `onLogout`; render greeting, progress chip (level + activityCount), static empty-state "Continuar aprendiendo" card with illustration placeholder, and catalog CTA
- [x] 2.2 Wire "Continuar aprendiendo" CTA and catalog CTA to `router.showActivities()`

## Phase 3: Wiring & Legacy Cleanup

- [x] 3.1 Modify `AuthenticatedHomeScaffold.kt`: replace `MainTab.HOME -> CourseScreen(...)` with `HomeDashboardScreen(router, onLogout)`
- [x] 3.2 Modify `AppModule.kt`: add `viewModelOf(::HomeDashboardViewModel)`
- [x] 3.3 Modify `App.kt`: remove legacy `CourseScreen`, `CourseContent`, `CourseList`, `CourseCard`, and associated previews no longer wired to HOME tab
- [x] 3.4 Modify `AuthGateRouter.kt`: update doc comments to reflect dashboard as authenticated landing; keep `AuthView.COURSE` enum unchanged

## Phase 4: Testing

- [x] 4.1 Create `HomeDashboardViewModelTest.kt`: verify greeting fallback (null name, time-based), level math, activity cap at 7, error state propagation
- [x] 4.2 Modify `AuthGateRoutingTest.kt`: update doc comments and test name referencing `CourseScreen` → dashboard landing
