# Tasks: Profile Screen with Bottom Navigation

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~480 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Shell) → PR 2 (Profile) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Bottom nav shell + router + placeholders | PR 1 | base=main; MainRouter, AuthenticatedHomeScaffold, PlaceholderScreen, assets, App.kt wiring, MainRouterTest |
| 2 | Profile screen + ViewModel | PR 2 | base=main; ProfileViewModel, ProfileScreen, AppModule.kt, ProfileViewModelTest |

## Phase 1: Foundation

- [x] 1.1 Create `ui/MainRouter.kt` with `MainTab` enum and `StateFlow<MainTab>`
- [x] 1.2 Create `composeResources/drawable/` tab icons and achievement placeholder assets

## Phase 2: Shell

- [x] 2.1 Create `ui/PlaceholderScreen.kt` with title + "under development" message
- [x] 2.2 Create `ui/AuthenticatedHomeScaffold.kt` with 4-tab `NavigationBar` and content switcher
- [x] 2.3 Modify `App.kt` to replace direct CourseScreen with AuthenticatedHomeScaffold

## Phase 3: Profile Screen

- [x] 3.1 Create `ui/ProfileViewModel.kt` deriving level, XP, streak, and achievements from local data
- [x] 3.2 Create `ui/ProfileScreen.kt` with avatar, name, level badge, XP bar, stats, achievements grid
- [x] 3.3 Modify `di/AppModule.kt` to register `ProfileViewModel`

## Phase 4: Tests

- [x] 4.1 Write `ui/MainRouterTest.kt` covering defaults, tab selection, and rapid switching
- [x] 4.2 Write `ui/ProfileViewModelTest.kt` covering metrics derivation and error fallback

## Implementation Order

Phase 1 → Phase 2 → Phase 3 → Phase 4. Phases 1 and 2 form the shell (PR 1), Phase 3 is the profile feature (PR 2), Phase 4 tests accompany each unit in their respective PR.
