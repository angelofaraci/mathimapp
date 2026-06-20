# Apply Phase Gate Report (PR 1 / Unit 1 — W1 Follow-up): architecture-refactor-assessment

> **Gate type**: Follow-up apply-phase gate after the W1 fix from `verify-report-apply-pr1.md`.
> Scope: re-verify the W1 fix only (Android preview isolation) + scope-drift re-check + focused compile/test feasibility for PR 1 / Unit 1.
> No commit or PR has been created yet; the working tree is reviewed as-is.

- **Change**: `architecture-refactor-assessment`
- **Artifact store mode**: `openspec`
- **Artifacts inspected**: `verify-report-apply-pr1.md` (prior gate), `proposal.md`, `design.md`, `tasks.md` (referenced for scope contract)
- **Source inspected**: working-tree diff for `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` and `composeApp/src/androidMain/kotlin/com/example/proyectofinal/MainActivity.kt`; `git status` for scope-drift re-check; `@Preview`/`App(`/`PreviewAppContent`/`AppAndroidPreview` grep across `composeApp/src`.
- **Mode**: Standard (Strict TDD not active — no `strict_tdd` config/runner).

## W1 Fix Verification

**W1 (prior gate)** — Android preview regressed into real Koin + DB initialization because `MainActivity.kt`'s `AppAndroidPreview()` called production `App()` directly.

**Fix applied** (per orchestrator's change description, confirmed in diff):

1. `composeApp/src/commonMain/.../App.kt`:
   - `App()` no longer annotated `@Preview` (the `@Preview` was removed from `App()`).
   - New public `PreviewAppContent()` composable that uses `MockCourseRepository` + `remember { CourseViewModel(repository) }` directly — NO `KoinApplication`, NO `koinViewModel`, NO `rememberPlatformModule`.
   - New private `@Preview AppPreview()` that delegates to `PreviewAppContent()`.
2. `composeApp/src/androidMain/.../MainActivity.kt`:
   - `AppAndroidPreview()` now calls `PreviewAppContent()` instead of production `App()`.

### W1 Compliance Evidence

| Check | Result | Evidence |
|---|---|---|
| `AppAndroidPreview()` does not call `App()` | ✅ | `MainActivity.kt:24` calls `PreviewAppContent()` |
| `PreviewAppContent()` does not touch Koin | ✅ | `App.kt:65-72` — body uses only `MockCourseRepository` + `CourseViewModel(repository)`; no `KoinApplication`/`koinViewModel`/`rememberPlatformModule` symbols |
| `App()` is no longer a `@Preview` entry on any target | ✅ | grep: only `@Preview` annotations in `composeApp/src` are on `AppAndroidPreview` (androidMain) and `AppPreview` (commonMain); `App()` has none |
| All `@Preview` entries route through the no-Koin path | ✅ | both `@Preview` sites call `PreviewAppContent()` |
| Android preview can no longer instantiate `AndroidSqliteDriver` via `PlatformModule` | ✅ | structural — `PreviewAppContent()` does not import or invoke `rememberPlatformModule`/`appModule`; cannot be runtime-verified (no Android SDK, see env blocker) |

**W1 status**: RESOLVED (structural proof; Android runtime verification still blocked by environment — same caveat as prior gate W3).

## Scope Drift Re-check

`git status` shows the same modified/untracked file set as the prior gate. The W1 fix touched ONLY:

- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` (in-slice)
- `composeApp/src/androidMain/kotlin/com/example/proyectofinal/MainActivity.kt` (in-slice)

No new files modified outside the PR 1 / Unit 1 slice contract (`tasks.md`: "Unit 1: composeApp + gradle deps only").

**Pre-existing C1 (unrelated spec file `openspec/changes/plataforma-aprendizaje-matematica/specs/learning/spec.md`)**: still present in the working tree. Not introduced by the W1 fix; not a new regression. The prior gate's recommendation stands — exclude/revert this file from the PR 1 staging set before committing.

## Build & Tests Execution

**Build (JVM target)**: ✅ Passed
```text
$ ./gradlew :composeApp:compileKotlinJvm --console=plain
BUILD SUCCESSFUL in 50s
w: DatabaseDriverFactory.kt: expect/actual classes are in Beta (KT-61573)   <- known W2, unchanged
w: DatabaseDriverFactory.jvm.kt: expect/actual classes are in Beta           <- known W2, unchanged
```
No new warnings introduced by the W1 fix. `PreviewAppContent()` (commonMain) compiles cleanly.

**Tests (JVM target)**: ✅ 25 passed / 0 failed / 0 skipped (unchanged from prior gate)
```text
composeApp/build/test-results/jvmTest/*.xml
  ComposeAppCommonTest        tests=1 failures=0 errors=0 skipped=0
  KtorCourseRepositoryTest    tests=8 failures=0 errors=0 skipped=0
  KtorExerciseRepositoryTest  tests=4 failures=0 errors=0 skipped=0
  KtorLessonRepositoryTest    tests=5 failures=0 errors=0 skipped=0
  KtorUserRepositoryTest      tests=5 failures=0 errors=0 skipped=0
  NetworkClientTest           tests=2 failures=0 errors=0 skipped=0
```
```text
$ ./gradlew :composeApp:jvmTest --console=plain --rerun-tasks
BUILD SUCCESSFUL in 1m 45s
```

**Build (Android target)**: ➖ Not run — `ANDROID_HOME`/`ANDROID_SDK_ROOT` unset, no `local.properties`, no `~/Android/Sdk`. Known environment blocker (prior gate W3). `MainActivity.kt` change is a one-line call swap to a public commonMain composable, so Android-target resolution risk is low, but unverified.

**Build (iOS target)**: ➖ Not run — Kotlin/Native toolchain not invoked. Unchanged from prior gate; not affected by the W1 fix.

## Issues Found

### CRITICAL

- None introduced by the W1 fix.
- **Carried forward from prior gate**: C1 (unrelated `plataforma-aprendizaje-matematica/specs/learning/spec.md` modification) remains in the working tree and must still be excluded from the PR 1 commit. Not a regression of this follow-up; recorded for continuity.

### WARNING

- **W3 (carried forward)** — Android and iOS targets unverified at runtime. `MainActivity.kt`'s `AppAndroidPreview()` change cannot be runtime-verified without Android SDK. Structural proof (call swap to a commonMain public composable that compiles on JVM) is strong but not a substitute for `:composeApp:assembleDebug`/Android-target compile.
- **W2 (carried forward)** — `expect class DatabaseDriverFactory` Kotlin 2.3 Beta warning. Unchanged by this fix.

### SUGGESTION

- **S4 (new, minor)** — `PreviewAppContent()` is `public` (explicit `fun PreviewAppContent()` with no visibility modifier). It was made public so `MainActivity.kt` (androidMain) can call it. This is the correct way to expose a commonMain composable to a platform source set, but consider annotating with `@Stable` or adding a KDoc line stating "Preview-only — do not call from production UI" to prevent future misuse. Not blocking.

## Verdict

**PASS WITH WARNINGS**

The W1 fix is structurally correct and minimal: Android preview no longer initializes production `App()`/Koin/`AndroidSqliteDriver`. All `@Preview` entry points now route through the isolated `PreviewAppContent()` which uses `MockCourseRepository` directly. JVM compile and 25/25 tests remain green; no new warnings. No scope drift introduced by the W1 fix. Remaining warnings (W2 Beta, W3 Android/iOS runtime unverified) and the carried-forward C1 (unrelated spec file in working tree) are pre-existing and unchanged.

## Recommendation

**proceed-next-slice** (after the carried-forward C1 hygiene step)

Before committing PR 1:
1. Exclude/revert `openspec/changes/plataforma-aprendizaje-matematica/specs/learning/spec.md` from the PR 1 staging set (carried-forward C1).

After that, commit PR 1 (Unit 1) and proceed to PR 2 (server service layer). Carry W2/W3 forward as notes in the PR 1 description (environment limitations + Beta warning). Optional: address S4 (KDoc on `PreviewAppContent`) either now or at archive time.

## Verification commands run

| Command | Run? | Result |
|---|---|---|
| `git status` / `git diff` for `App.kt` + `MainActivity.kt` | yes | scoped to W1 fix only; no drift |
| grep `@Preview` / `App(` / `PreviewAppContent` / `AppAndroidPreview` in `composeApp/src` | yes | 2 `@Preview` sites, both via `PreviewAppContent()`; `App()` has no `@Preview` |
| `./gradlew :composeApp:compileKotlinJvm` | yes | BUILD SUCCESSFUL (only known W2 warnings) |
| `./gradlew :composeApp:jvmTest --rerun-tasks` | yes | BUILD SUCCESSFUL; 25 tests, 0 failures |
| `./gradlew :composeApp:assembleDebug` | no | Android SDK missing (env blocker, expected) |
| iOS compile | no | not invoked; unaffected by W1 fix |

## skill_resolutions

- `sdd-verify` SKILL.md loaded via `skill()` tool. Executor-override path (this agent IS the `sdd-verify` sub-agent); not delegated further.
- `strict-tdd-verify.md` **not** loaded — Strict TDD not active (no `strict_tdd` config/runner; standard verify path used).
- `_shared` SDD references: not separately loaded; structured status was provided inline by the orchestrator (artifact paths + context files + assigned scope).
- Report persisted to openspec artifact store as a new distinct file (`verify-report-apply-pr1-w1-followup.md`) to preserve the prior gate's audit trail.
