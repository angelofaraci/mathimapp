# Design: Home Dashboard (Inicio Tab)

## Technical Approach

Replace the HOME tab’s legacy `CourseScreen` with a dedicated `HomeDashboardScreen`/`HomeDashboardViewModel` pair in `composeApp`. The new ViewModel will follow the existing `ProfileViewModel` and `CourseCatalogViewModel` pattern: load once in `init`, expose immutable `StateFlow`, and aggregate data from `AuthRepository`, `UserRepository`, and `LearnerProfileRepository` only. No backend or shared-contract changes are required.

## Architecture Decisions

| Decision | Alternatives considered | Rationale |
|---|---|---|
| Create `ui/home/HomeDashboardScreen.kt` and `ui/home/HomeDashboardViewModel.kt` instead of extending `CourseScreen` in `App.kt` | Keep adding branches inside `CourseScreen`; place dashboard logic in `AuthenticatedHomeScaffold` | `CourseScreen` is legacy catalog UI embedded in `App.kt`; a new screen keeps HOME isolated, testable, and aligned with current screen/viewmodel structure. |
| Reuse existing progress sources (`authRepository.session`, `userRepository.getUserProgress`, `learnerProfileRepository.getProfile`) | Add new repository or fetch course catalog on HOME | These sources already power profile/catalog flows and contain all required data for greeting, level, and synthetic activity count. |
| Represent “streak” as a generic activity metric capped at 7 | Use “days” semantics or invent date logic | Current data only exposes `completedLessonIds`; the spec explicitly forbids date-based implication. Use neutral labels such as `Activity`/`Completed lessons`. |
| Reuse `MainRouter.showActivities()` for both CTAs | Add nested navigation state for catalog targeting | The ACTIVITIES tab already opens `CourseCatalogScreen`, so tab switching alone satisfies both CTA scenarios without widening router scope. |
| Reuse `achievement_placeholder.svg` as the empty-state illustration initially | Add a new drawable now | Repo already has a drawable placeholder pattern; reusing it avoids blocking the frontend-only slice on asset creation. |

## Data Flow

`AuthGate` → `AuthenticatedHomeScaffold` → `HomeDashboardScreen` → `HomeDashboardViewModel`

`HomeDashboardViewModel` reads:

- `AuthRepository.session.value.user` for display name
- `UserRepository.getUserProgress(user.id)` for `totalScore` and `completedLessonIds`
- `LearnerProfileRepository.getProfile()` for school-year context if needed in subtitle text

Derived state:

- `greeting = salutation(now) + optionalName`
- `level = totalScore / 100`
- `activityCount = min(completedLessonIds.size, 7)`
- `continueLearningCard = EmptyState`

CTA flow:

`Button click` → `MainRouter.showActivities()` → `AuthenticatedHomeScaffold` recomposes → `CourseCatalogScreen`

## File Changes

| File | Action | Description |
|---|---|---|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardScreen.kt` | Create | Compose UI for greeting, summary card, empty-state learning card, and catalog CTA. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardViewModel.kt` | Create | Loads user/progress data and exposes dashboard UI state plus `openActivities()` callback intent surface. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` | Modify | Replace `CourseScreen` on `MainTab.HOME` with `HomeDashboardScreen(router = router, onLogout = onLogout)`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modify | Register `HomeDashboardViewModel` with Koin. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modify | Remove legacy HOME-only `CourseScreen` preview/wiring that is no longer used by authenticated navigation. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthGateRouter.kt` | Modify | Update comments/docs to reflect dashboard landing, while keeping routing behavior unchanged. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/HomeDashboardViewModelTest.kt` | Create | Verify greeting fallback, level/activity derivation, and empty-state CTA intent. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/AuthGateRoutingTest.kt` | Modify | Update expectations/comments so authenticated users enter the dashboard-hosting scaffold rather than legacy `CourseScreen`. |

## Interfaces / Contracts

```kotlin
data class HomeDashboardUiState(
    val isLoading: Boolean = true,
    val greeting: String = "",
    val level: Int = 0,
    val activityCount: Int = 0,
    val completedLessons: Int = 0,
    val errorMessage: String? = null
)
```

`HomeDashboardScreen` should accept `router: MainRouter` so CTA handlers can directly call `router.showActivities()` without introducing a second navigator abstraction.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Greeting fallback, level math, activity cap at 7, error state | New `HomeDashboardViewModelTest` using fake repositories and `StandardTestDispatcher`. |
| Integration | HOME tab swaps to dashboard and CTA switches tabs | Extend `MainRouterTest` and keep routing logic pure; no backend mocking required. |
| E2E | N/A | No E2E harness exists in `composeApp`; covered by `:composeApp:jvmTest`. |

## Migration / Rollout

No migration required.

## Open Questions

- [ ] Should the greeting clock come from a small injectable provider for deterministic tests, or is a ViewModel helper with parameterized test input sufficient?
