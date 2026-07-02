## Implementation Progress

**Change**: inicio-dashboard
**Mode**: Standard

### Completed Tasks
- [x] 1.1 Create `ui/home/HomeDashboardViewModel.kt` with `HomeDashboardUiState`, inject `AuthRepository`, `UserRepository`, `LearnerProfileRepository`
- [x] 1.2 Implement greeting derivation (`salutation(now)` + displayName fallback), level math (`totalScore / 100`), activity cap (`min(completedLessonIds.size, 7)`), and `openActivities()` callback
- [x] 2.1 Create `ui/home/HomeDashboardScreen.kt` accepting `router: MainRouter` and `onLogout`; render greeting, progress chip (level + activityCount), static empty-state "Continuar aprendiendo" card with illustration placeholder, and catalog CTA
- [x] 2.2 Wire "Continuar aprendiendo" CTA and catalog CTA to `router.showActivities()`
- [x] 3.1 Modify `AuthenticatedHomeScaffold.kt`: replace `MainTab.HOME -> CourseScreen(...)` with `HomeDashboardScreen(router, onLogout)`
- [x] 3.2 Modify `AppModule.kt`: add `viewModelOf(::HomeDashboardViewModel)`
- [x] 3.3 Modify `App.kt`: remove legacy `CourseScreen`, `CourseContent`, `CourseList`, `CourseCard`, and associated previews no longer wired to HOME tab
- [x] 3.4 Modify `AuthGateRouter.kt`: update doc comments to reflect dashboard as authenticated landing; keep `AuthView.COURSE` enum unchanged
- [x] 4.1 Create `HomeDashboardViewModelTest.kt`: verify greeting fallback (null name, time-based), level math, activity cap at 7, error state propagation
- [x] 4.2 Modify `AuthGateRoutingTest.kt`: update doc comments and test name referencing `CourseScreen` → dashboard landing

### Files Changed
| File | Action | What Was Done |
|------|--------|---------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardViewModel.kt` | Created | Added the dashboard view model, UI state, greeting/activity derivation, profile label loading, and the `openActivities()` callback surface. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardScreen.kt` | Created | Built the HOME dashboard UI with greeting, progress summary card, static continue-learning empty state, and both CTA buttons targeting the Activities tab. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardClock.kt` | Created | Declared the shared local-hour contract used by dashboard greeting derivation. |
| `composeApp/src/androidMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardClock.android.kt` | Created | Added the Android local-hour implementation for dashboard salutations. |
| `composeApp/src/jvmMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardClock.jvm.kt` | Created | Added the JVM local-hour implementation for dashboard salutations. |
| `composeApp/src/iosMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardClock.ios.kt` | Created | Added the iOS local-hour implementation for dashboard salutations. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileViewModel.kt` | Modified | Reused the existing level/activity constants by widening their visibility for dashboard derivation instead of duplicating `100` and `7`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` | Modified | Swapped the HOME tab content from the legacy course screen to `HomeDashboardScreen`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modified | Registered `HomeDashboardViewModel` in Koin. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modified | Removed the legacy HOME-only `CourseScreen` implementation and its previews so the authenticated flow relies on the scaffold-backed dashboard. |
| `composeApp/src/androidMain/kotlin/com/example/proyectofinal/MainActivity.kt` | Modified | Repointed the Android preview to a sample `HomeDashboardContent` state after the legacy `PreviewAppContent()` helper was removed from `App.kt`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthGateRouter.kt` | Modified | Updated the auth-gate routing docs to describe the dashboard-backed authenticated landing while keeping `AuthView.COURSE` unchanged. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginViewModel.kt` | Modified | Updated the success-path comment to describe the dashboard landing instead of the removed course screen. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/home/HomeDashboardViewModelTest.kt` | Modified | Added focused coverage for salutation timing, greeting fallback, level math, activity cap, error propagation, and the explicit zero-progress chip state (`level=0`, `activityCount=0`, `completedLessons=0`). |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/AuthGateRoutingTest.kt` | Modified | Renamed the authenticated routing test to reference the dashboard landing. |
| `openspec/changes/inicio-dashboard/tasks.md` | Modified | Marked all dashboard implementation tasks complete. |
| `openspec/changes/inicio-dashboard/apply-progress.md` | Created | Recorded the cumulative implementation progress for the change. |

### Verification
| Command | Result |
|---------|--------|
| `./gradlew :composeApp:jvmTest --tests "com.example.proyectofinal.ui.home.HomeDashboardViewModelTest" --tests "com.example.proyectofinal.ui.AuthGateRoutingTest"` | Passed (`BUILD SUCCESSFUL`). This compiled the touched composeApp sources and ran the focused dashboard/auth routing JVM coverage requested for this slice. |
| `./gradlew :composeApp:assembleDebug` | Failed initially because `MainActivity.kt` still referenced the removed `PreviewAppContent()` helper after the `App.kt` legacy HOME cleanup. |
| `./gradlew :composeApp:assembleDebug` | Passed after updating the Android preview to render `HomeDashboardContent` with sample state (`BUILD SUCCESSFUL`). |
| `./gradlew :composeApp:jvmTest --tests "com.example.proyectofinal.ui.home.HomeDashboardViewModelTest"` | Passed (`BUILD SUCCESSFUL`). Remediation rerun after adding an explicit runtime assertion for the zero-progress chip scenario. |

### Remediation Follow-up
- Added a focused `HomeDashboardViewModelTest` that loads a user with `completedLessonIds = emptySet()` and `totalScore = 0`, then asserts `level = 0`, `activityCount = 0`, and `completedLessons = 0` after `advanceUntilIdle()`.
- Scope remained limited to the verification warning; no DI cleanup or unrelated refactors were introduced.

### Deviations from Design
- `MainRouterTest` was not expanded despite the design's broader integration suggestion. Existing router coverage already proves `showActivities()` tab switching, and the new dashboard screen only delegates both CTAs directly to that existing router method, so additional router tests were unnecessary for the touched files.

### Issues Found
- `MainActivity.kt` still referenced the removed `PreviewAppContent()` helper after the `App.kt` cleanup, which broke `:composeApp:assembleDebug`. Repointing the preview to `HomeDashboardContent` fixed the Android compile path without reintroducing legacy HOME UI.

### Remaining Tasks
- None — all tasks for this change are complete.

### Workload / PR Boundary
- Mode: single PR
- Current work unit: N/A
- Boundary: starts from the existing authenticated HOME tab and ends with the dedicated dashboard view model/screen, scaffold wiring, legacy HOME cleanup, auth-routing doc updates, and focused JVM verification.
- Estimated review budget impact: Medium; mostly localized composeApp UI/view-model wiring plus one new targeted test file.

### Status
10/10 tasks complete. Ready for verify; the zero-progress chip warning now has explicit runtime test evidence.
