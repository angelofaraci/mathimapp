# Tasks: Profile Visual Refresh

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~480–550 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (data+primitives) → PR 2 (screen+screen-tests) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | ViewModel fields + 3 primitives + VM tests | PR 1 | Independent; base = main |
| 2 | ProfileScreen hub + sub-screens + screen tests | PR 2 | Depends on PR 1; base = main or PR 1 branch |

## Phase 1: Foundation — ViewModel & Primitives

- [x] 1.1 Add `email: String = ""` and `role: UserRole = UserRole.STUDENT` to `ProfileUiState` data class in `ProfileViewModel.kt`; preserve all existing fields
- [x] 1.2 Map `sessionUser.email` and `sessionUser.role` in the success branch of `loadProfile()` in `ProfileViewModel.kt`; set empty string / default in error fallback
- [x] 1.3 Create `ProfileNavigationCard.kt` in `ui/primitives/`: clickable `MCard` with icon box, title, subtitle, chevron
- [x] 1.4 Create `ProfileToggleRow.kt` in `ui/primitives/`: labeled row with stateless `Switch` and TODO/no-op callback
- [x] 1.5 Create `ProfileListRow.kt` in `ui/primitives/`: label + value row with trailing action/chevron
- [x] 1.6 Add `email` and `role` assertions in `ProfileViewModelTest.kt` success and error test cases

## Phase 2: Core — ProfileScreen Hub & Sub-Screens

- [x] 2.1 Define local `ProfileSubScreen` enum (`HUB`, `ACCOUNT`, `PREFERENCES`, `HELP`, `ABOUT`) + `remember { mutableStateOf(HUB) }` destination + `BackHandler(enabled = destination != HUB)` inside `ProfileContent`
- [x] 2.2 Build `ProfileHub` composable: centered `ProfileIdentity` (avatar initials, edit badge TODO, name, email, role chip), four `ProfileNavigationCard`s, `MButton` logout, version caption
- [x] 2.3 Build `ProfileSubScreenScaffold` (back button + centered title) + `AccountScreen`: name/email/password rows (TODO) + legal + delete button (TODO)
- [x] 2.4 Build `PreferencesScreen`: static notification/sound toggles + Spanish language row (all TODO/no-op); no dark-mode row
- [x] 2.5 Build `HelpScreen` and `AboutScreen`: hardcoded list rows with TODO trailing actions
- [x] 2.6 Wire `AnimatedContent(destination)` switcher in `ProfileContent` routing to `ProfileHub` or the active sub-screen; preserve loading/error branches above

## Phase 3: Testing & Verification

- [x] 3.1 Create `ProfileScreenTest.kt` in `jvmTest`: semantic text assertions for hub identity, all four nav cards, logout button, version caption, initials fallback
- [x] 3.2 Add tests for local navigation: card tap switches sub-screen, in-app back + Android `BackHandler` returns to `HUB`, default sub-screen is `HUB`
- [x] 3.3 Run `./gradlew :composeApp:jvmTest` — all existing VM tests pass + new screen tests pass
