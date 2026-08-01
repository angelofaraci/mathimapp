# Tasks: Localize UI Copy to Spanish via Compose Resources

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~670 (A ~180, B ~140, C ~150, D ~200) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Group A) → PR 2 (Group B) → PR 3 (Group C) → PR 4 (Group D) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| A | Skeleton `strings.xml`/`values-en`, `StudentTrackLabels.kt`, `OnboardingScreen.kt`, `StudentTrack` round-trip test | PR 1 | `./gradlew :composeApp:jvmTest --tests "*SqlDelightLearnerProfileRepositoryTest*"` | `:composeApp:assembleDebug` | revert PR 1 branch, delete added XML keys/`StudentTrackLabels.kt` |
| B | `LessonMapScreen`, `TheorySheet`, `PlaceholderScreen`, `LessonMapNode` | PR 2 | `./gradlew :composeApp:jvmTest --tests "*LessonMap*" --tests "*TheorySheet*"` | `:composeApp:assembleDebug` | revert PR 2 branch only |
| C | `RegisterScreen` ("Back", step indicator), `HomeDashboardScreen` (`JoinCourseCard`, level, alt text) | PR 3 | `./gradlew :composeApp:jvmTest --tests "*Register*" --tests "*HomeDashboard*"` | `:composeApp:assembleDebug` | revert PR 3 branch only |
| D | Login/Profile/AuthScaffold/AuthenticatedHomeScaffold sweep, `schoolYearLabel`→structured-state refactor, backlog sync | PR 4 | `./gradlew :composeApp:jvmTest --tests "*Profile*" --tests "*HomeDashboard*" --tests "*Login*"` | `:composeApp:assembleDebug` | revert PR 4 branch only |

Ask user for chain strategy (stacked-to-main vs feature-branch-chain) before `sdd-apply`.

## Phase 1: Group A — Resource Skeleton + Onboarding

- [x] 1.1 Create `composeApp/src/commonMain/composeResources/values/strings.xml` with `onboarding_*` and `track_*` keys (Spanish, per design table)
- [x] 1.2 Create `composeApp/src/commonMain/composeResources/values-en/strings.xml` with same key set, English
- [x] 1.3 (RED) Add round-trip test in `SqlDelightLearnerProfileRepositoryTest.kt`: persist/reload each `StudentTrack` via `displayName`, assert `parse()` returns original enum unchanged
- [x] 1.4 (GREEN) Confirm test passes with no `StudentTrack.kt` changes (contract untouched)
- [x] 1.5 Create `.../ui/StudentTrackLabels.kt` with `@Composable fun StudentTrack.localizedLabel(): String`
- [x] 1.6 Change `allowedTrackSummary()` (`OnboardingScreen.kt:385`) to `@Composable`, map via `localizedLabel()`; update its callers to composable context
- [x] 1.7 Replace all 19 literal sites in `OnboardingScreen.kt` (steps 1-4, actions, `StepSummary`, `ConfirmationStep`) with `stringResource(Res.string.*)`
- [x] 1.8 Manual sweep: check `OnboardingScreen.kt` for default-parameter-value literals and content descriptions missed by grep
- [x] 1.9 Run `rg -n '(Text\(\s*"|text = "|contentDescription = ")' composeApp/.../ui/OnboardingScreen.kt` — expect zero matches
- [x] 1.10 Run `./gradlew :composeApp:jvmTest :composeApp:assembleDebug`

## Phase 2: Group B — Lesson Map / Theory / Placeholder

- [x] 2.1 Add Group B keys to both `strings.xml` files (lesson map progress `%1$d`/`%1$d/%2$d`, theory sheet, placeholder)
- [x] 2.2 Replace literals in `.../ui/activities/LessonMapScreen.kt`, including `contentDescription` at line ~451
- [x] 2.3 Replace literals in `.../ui/activities/TheorySheet.kt`
- [x] 2.4 Replace literals in `.../ui/activities/LessonMapNode.kt`, including `contentDescription`/semantics at lines ~79-93
- [x] 2.5 Replace literals in `.../ui/PlaceholderScreen.kt`, including the `message` default-parameter-value literal (grep miss flagged in design)
- [x] 2.6 Update any `jvmTest` render test asserting an English/old literal in these files
- [x] 2.7 Grep + manual + contentDescription sweep on Group B files; expect zero remaining literals
- [x] 2.8 Run `./gradlew :composeApp:jvmTest :composeApp:assembleDebug`

## Phase 3: Group C — Register / Home Dashboard

- [x] 3.1 Add Group C keys (`register_step_indicator` `%1$d`, "Back", `home_level` `%1$d`, join-course copy)
- [x] 3.2 Replace "Back" and step-indicator literals in `.../ui/RegisterScreen.kt`
- [x] 3.3 Replace `JoinCourseCard`, level, and alt-text literals in `.../ui/home/HomeDashboardScreen.kt`, including `contentDescription` at line ~277
- [x] 3.4 Update any `jvmTest` render test asserting old literals in these files
- [x] 3.5 Grep + manual + contentDescription sweep on Group C files
- [x] 3.6 Run `./gradlew :composeApp:jvmTest :composeApp:assembleDebug`

## Phase 4: Group D — Remaining Sweep + schoolYearLabel Refactor

- [x] 4.1 (RED) Update `ProfileViewModelTest.kt`: assert `uiState.schoolYear: Int?` and `uiState.studentTrack: StudentTrack?` instead of `schoolYearLabel` string
- [x] 4.2 (RED) Update `HomeDashboardViewModelTest.kt`: same structured-field assertions
- [x] 4.3 (GREEN) Replace `schoolYearLabel: String?` with `schoolYear: Int?` + `studentTrack: StudentTrack?` in `ProfileUiState` (`ProfileViewModel.kt`); update `loadProfile()` to set both fields from `profile`, remove string interpolation
- [x] 4.4 (GREEN) Same change in `HomeDashboardUiState`/`buildDashboardState()` (`HomeDashboardViewModel.kt`)
- [x] 4.5 N/A — `schoolYearLabel`/`schoolYear` was never rendered by `ProfileScreen.kt` (dead state prior to this change; only referenced by tests and the `MainActivity.kt` Android preview). No render call site existed to update. `profile_school_year` key was still added per 4.7 for future use. Documented as a deviation from design in the apply report.
- [x] 4.6 N/A — same reason as 4.5; `HomeDashboardScreen.kt` never rendered `schoolYearLabel` either.
- [x] 4.7 Add Group D keys to both `strings.xml` files (Login/Register remaining/Profile/AuthScreenScaffold/AuthenticatedHomeScaffold/HomeDashboard remaining literals, `profile_school_year` placeholder string)
- [x] 4.8 Replace remaining literals in `.../ui/LoginScreen.kt`
- [x] 4.9 Replace remaining literals in `.../ui/ProfileScreen.kt`, including `contentDescription`/semantics at lines ~229, ~311 (now `editAvatarDescription`/`backDescription`), plus `UserRole.displayName()` converted to `@Composable UserRole.localizedLabel()`
- [x] 4.10 Replace remaining literals in `.../ui/AuthScreenScaffold.kt`, including `contentDescription` at line ~102
- [x] 4.11 Replace remaining literals in `.../ui/AuthenticatedHomeScaffold.kt` (nav tab labels + `PlaceholderScreen(title = ...)`)
- [x] 4.12 Update `ProfileScreenTest.kt`/`ProfileRedesignRenderTest.kt`/`AuthRedesignRenderTest.kt`/`HomeDashboardRedesignRenderTest.kt` and any other `jvmTest` asserting old literals or `schoolYearLabel` (JVM English-fallback gotcha applied)
- [x] 4.13 Grep + manual + contentDescription sweep across all Group D files; zero remaining literals except decorative glyph icons (`"÷"` in `HomeDashboardScreen.kt`, `"›"` in shared `ProfileNavigationCard.kt`/`ProfileListRow.kt` primitives, pre-existing/out of scope) and the `"Mathim"`/`"App"` brand-name spans in `AuthScreenScaffold.kt` (ViewModel `errorMessage`/`Throwable.message` excluded)
- [x] 4.14 Run `./gradlew :composeApp:jvmTest :composeApp:assembleDebug` — both BUILD SUCCESSFUL

## Phase 5: Backlog Sync

- [x] 5.1 In `openspec/backlog.md`, mark "App is not localized to Spanish" (item #1, "Onboarding and navigation bug fixes") resolved, referencing `localizacion-espanol`
- [x] 5.2 Add new backlog entry: "Localize ViewModel error messages via sealed error types" — covers `errorMessage: String` fields, raw `Throwable.message`, and the deferred `schoolYear`/`studentTrack` render step
