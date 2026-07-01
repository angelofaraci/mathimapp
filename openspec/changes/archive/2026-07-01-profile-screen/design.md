# Design: Profile Screen with Bottom Navigation

## Technical Approach

Keep the change inside `composeApp/commonMain` by replacing the authenticated `AuthView.COURSE` branch with a small authenticated router + `Scaffold` shell. The shell keeps `CourseScreen` as the Inicio tab, adds lightweight placeholders for Actividades/Progreso, and mounts a new `ProfileScreen` powered by a `ProfileViewModel` that composes `AuthRepository`, `UserRepository`, and `LearnerProfileRepository` data already available on-device.

## Architecture Decisions

| Decision | Options | Tradeoff | Choice / Rationale |
|---|---|---|---|
| Authenticated routing | Inline mutable tab state in `App.kt`; dedicated router class | Inline is faster but harder to test and grows `App.kt` | Add `ui/MainRouter.kt` mirroring `AuthGateRouter`; keeps routing pure and unit-testable. |
| Profile data source | Add backend/shared contracts; derive in app | Backend is more accurate but out of scope | Derive in app from `AuthSession.user`, `UserProgress`, and `LearnerProfile`; matches proposal and module ownership. |
| Gamification model | Persist new profile metrics; derive view-only metrics | Persistence would need schema work | Keep view-only derived `ProfileUiState`; no SQLDelight changes. |
| Icons/assets | Add dependency on Material icons; use compose resources | Dependency is smaller effort; resources match proposal and KMP-safe packaging | Use `composeResources/drawable` SVG/XML assets so tabs and achievements stay explicit and platform-neutral. |

## Data Flow

```text
AuthGate
  └─ resolveAuthView(...) == COURSE
      └─ AuthenticatedHomeScaffold
          ├─ MainRouter.target -> selected tab
          ├─ Home tab -> existing CourseScreen/CourseViewModel
          ├─ Activity/Progress tabs -> PlaceholderScreen
          └─ Profile tab -> ProfileScreen/ProfileViewModel
                           ├─ AuthRepository.session -> current user
                           ├─ UserRepository.getUserProgress(user.id)
                           └─ LearnerProfileRepository.getProfile()
                                -> derive badge/stats/achievements -> ProfileUiState
```

Sequence: session becomes authenticated → `AuthGate` loads scaffold → Profile tab selected → `ProfileViewModel` reads current user from session, fetches progress by `user.id`, reads learner profile, derives metrics, emits `Loading/Success/Error` state.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modify | Replace direct authenticated `CourseScreen` render with scaffold host; keep login/register/onboarding flow intact. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/MainRouter.kt` | Create | Pure tab router (`HOME`, `ACTIVITIES`, `PROGRESS`, `PROFILE`) with `StateFlow` and selection methods. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` | Create | Bottom navigation shell and tab content switcher; owns logout action placement. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileViewModel.kt` | Create | Loads user/profile/progress and derives profile metrics. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileScreen.kt` | Create | Profile layout, progress bar, stats cards, achievement grid, loading/error states. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/PlaceholderScreen.kt` | Create | Minimal reusable placeholder for Actividades/Progreso. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modify | Register `ProfileViewModel`. |
| `composeApp/src/commonMain/composeResources/drawable/*` | Create | Bottom-nav and achievement placeholder assets. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/MainRouterTest.kt` | Create | Unit coverage for tab selection defaults and transitions. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/ProfileViewModelTest.kt` | Create | Unit coverage for derived metrics and error fallback. |

## Interfaces / Contracts

```kotlin
enum class MainTab { HOME, ACTIVITIES, PROGRESS, PROFILE }

data class ProfileUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val schoolYearLabel: String? = null,
    val level: Int = 0,
    val currentXp: Int = 0,
    val xpForNextLevel: Int = 100,
    val streak: Int = 0,
    val completedLessons: Int = 0,
    val achievements: List<ProfileAchievement> = emptyList(),
    val errorMessage: String? = null
)
```

Gamification derivation rules: `level = totalScore / 100` using integer division; `currentXp = totalScore % 100`; `xpForNextLevel = 100`; `streak = min(completedLessonIds.size, 7)`; completed lessons = `completedLessonIds.size`; achievements unlocked by threshold rules (`first_lesson`, `score_100`, `lessons_5`, `lessons_10`). `displayName` comes from `session.user?.name`, badge subtitle from `LearnerProfile.schoolYear/studentTrack`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `MainRouter` defaults and tab switching | Plain `kotlin.test` like `AuthGateRouterTest`. |
| Unit | `ProfileViewModel` state mapping, loading, repository failures | Fake repositories + coroutine test dispatcher in `commonTest`. |
| Integration | Koin resolves new ViewModel and scaffold still composes authenticated flow | Extend existing `ComposeAppCommonTest` module-resolution coverage. |
| E2E | Not planned | No Compose UI test harness is configured in this repo. |

## Migration / Rollout

No migration required.

## Open Questions

None.
