# Exploration: post-onboarding-course-navigation-crash

## Current State

Full trace read end-to-end: `OnboardingViewModel.completeOnboarding()` → `OnboardingScreen`'s `LaunchedEffect(state.isCompleted)` → `App.kt AuthGate`'s `onboardingRefreshKey += 1` → `produceState<Boolean?>` re-reads `learnerProfileRepository.isOnboardingComplete()` → `resolveAuthView` → `AuthView.COURSE` → `AuthenticatedHomeScaffold` → `HomeDashboardScreen`/`HomeDashboardViewModel.loadDashboard()`.

Every hop in this path is unusually well-guarded against Kotlin exceptions:
- `OnboardingViewModel.completeOnboarding()` (`composeApp/.../ui/OnboardingViewModel.kt:196-218`) wraps `upsertProfile` in `runCatching`.
- `HomeDashboardViewModel.loadDashboard()` (`.../ui/home/HomeDashboardViewModel.kt:99-116`) wraps `buildDashboardState` in `try/catch(Exception)` with a separate `catch(CancellationException) { throw error }` rethrow.
- `loadInProgressCourses` (`HomeDashboardViewModel.kt:139-155`) wraps `courseRepository.getEnrolledCourses` in `runCatching { }.getOrElse { emptyList() }`.
- `KtorCourseRepository.getCourseById` / `joinCourseByCode` catch internally and return `null`; `KtorUserRepository.getCurrentUser` / `getUserRole` / `getUserProgress` all catch and fall back to local/defaults.

## Affected Areas

- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt:45-89` — AuthGate transition logic, `produceState` key handling.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/OnboardingViewModel.kt:174-219` — `completeOnboarding()`.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardViewModel.kt` — first real consumer of `CourseRepository`/`UserRepository` together after onboarding.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/SqlDelightLearnerProfileRepository.kt` — profile round-trip (checked, not buggy).
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorUserRepository.kt:83-106` — `syncUserProgressToLocal` (FK insert order).
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/LocalDatabaseSchemaFixes.kt:99,140,185,232` — conditional `PRAGMA foreign_keys` toggling.
- `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq:65-71` — `LearnerProfileEntity` singleton-row design.

## Ruled Out (checked with evidence, not the bug)

1. **Profile enum round-trip mismatch** — the obvious first suspect. `upsertProfile` stores `studentTrack.displayName` (e.g. `"Technical Secondary"`); `getProfile()` calls `StudentTrack.parse(entity.studentTrack)`, whose `normalizeStudentTrack()` uppercases and replaces space/hyphen with `_`, so `"Technical Secondary"` → `"TECHNICAL_SECONDARY"`, matching `track.name`. Round-trips correctly. Not a bug.
2. **Division by zero / NaN in level or XP math** — `XpPerLevel = 100` (`ui/ProfileViewModel.kt:16`), never 0; `MLinearProgressIndicator` progress calc already guards `xpForNextLevel > 0`.
3. **`!!` / force-unwrap crashes** — grep across all of `commonMain/kotlin/com/example/proyectofinal` found zero `!!` operators. `ProfileScreen.kt:432` has a `.first()` inside `toInitials()`, but it's guarded by `.filter { it.isNotBlank() }` first, and that screen isn't even in this navigation path (default tab is HOME).
4. **Missing Koin bindings** — `HomeDashboardViewModel`'s constructor deps (`AuthRepository`, `CourseRepository`, `UserRepository`, `LearnerProfileRepository`) are all bound in `appModule` (`di/AppModule.kt:51-56`); `CourseApi`/`UserApi` constructors match their `get(), get()` wiring.
5. **Missing drawable resources** — `ic_flame`, `achievement_placeholder`, `tab_home` all exist under `composeResources/drawable/`.
6. **Koin ViewModel scope teardown on AuthView transition** — `koinViewModel<X>()` resolves against `LocalViewModelStoreOwner.current` (Activity-scoped here, no nested `NavBackStackEntry` scoping). Switching `ONBOARDING → COURSE` in the same `AuthGate` composition doesn't tear down any scope; `HomeDashboardViewModel` is simply created fresh (first use). No race found.
7. **`produceState` restart behavior** — bumping `onboardingRefreshKey` does restart the `produceState` producer from `initialValue` (briefly `null` again since `session.isAuthenticated` is still true), causing a momentary re-flash of the top-level loading spinner before flipping to `AuthView.COURSE`. This is expected `produceState` semantics on key change — a UX flash, not a crash.

## Approaches (for closing the gap between "static review found nothing" and "user says it crashes")

1. **Get a real stack trace first** — capture with `adb logcat -s AndroidRuntime:E *:F` on next repro. Authoritative, zero speculation. Blocks investigation until repro happens. Effort: Low.
2. **Add defensive instrumentation now** (wrap `AuthGate` body and/or `HomeDashboardScreen`/`AuthenticatedHomeScaffold` composition in a try/catch + `Log.e`, or install a global `Thread.setDefaultUncaughtExceptionHandler` in `MainActivity`) so that if it recurs before a clean repro, the log captures the real exception. Catches it even if the reporter forgets to capture logcat carefully; also converts a hard crash into a graceful error screen. Temporary/diagnostic unless made permanent deliberately. Effort: Low-Medium.
3. **Proactively harden the two genuinely speculative gaps found** (see Risks below) even without a confirmed stack trace, since both are real code smells independent of this specific bug. Fixes real latent issues regardless of root cause, but is "shotgun debugging" without confirmation. Effort: Medium.

## Recommendation

Do **not** guess-fix yet. The mainstream Kotlin/JVM-catchable path is unusually well-guarded (see "Ruled Out" above), so a confirmed root cause requires the actual stack trace (Approach 1), ideally paired with Approach 2's lightweight instrumentation in case the crash is intermittent. Approach 3's two flagged smells are worth tracking as separate follow-up items regardless of what the stack trace shows.

## Risks (ranked, with explicit confidence)

1. **[Medium confidence, most concrete open lead]** `SqlDelightLearnerProfileRepository`/`AppDatabase.sq:65-71`: `LearnerProfileEntity` is a **single global row** keyed by `profileId = 1` (`CHECK (profileId = 1)`), not scoped per user ID. On any device/emulator that previously ran onboarding for a different account (common during dev/QA testing with repeated sign-ups on the same emulator), `getProfile()`/`isOnboardingComplete()` return stale/wrong data for the new user. This doesn't by itself explain a hard crash for a genuinely fresh signup, but is a real data-integrity bug and a strong candidate if the reporter's repro device/emulator has prior test accounts on it.
2. **[Medium-low confidence, requires a specific precondition]** `di/LocalDatabaseSchemaFixes.kt` only sets `PRAGMA foreign_keys=OFF`→`ON` inside the conditional `ensureLessonEntityShape`/`ensureExerciseEntityShape` migration blocks, and only when those migrations actually fire (i.e., only on a device upgrading from an older local schema — not a clean install, where `AndroidSqliteDriver` never enables FK enforcement at all). On a device where that migration previously ran, `foreign_keys` stays `ON` for the rest of the connection's life; a subsequent `syncUserProgressToLocal` (`KtorUserRepository.kt:83-106`) inserting `EnrolledCourse`/`CompletedLesson` rows referencing a `courseId`/`lessonId` not yet locally cached (very plausible right after fresh onboarding, before any course/lesson has been synced) would throw `SQLiteConstraintException`. However, this is still caught by `getUserProgress`'s own `catch (e: Exception)` fallback in the current code, so on today's code it degrades to empty/stale progress rather than crashing — flagged as speculative because it requires the "device previously migrated from the old lesson schema" precondition, which we cannot confirm statically.
3. **[Low confidence, unconfirmed]** Something outside commonMain's reach entirely — an Android-specific Compose snapshot/threading issue, ANR, or R8/minification artifact — since no exception path in the shared Kotlin business logic appears reachable and uncaught. This can only be confirmed with an actual stack trace.
4. Reporter has not yet provided a stack trace; all of the above remain hypotheses until repro evidence is available.

## Ready for Proposal

**No** — recommend asking the user to reproduce and attach a logcat/stack trace before running `sdd-propose`. If they cannot get one immediately, propose Approach 2 (temporary instrumentation) as a fast, safe first PR to capture the exception on next occurrence, and independently track Risk #1 (global singleton learner-profile row) as a real bug worth fixing regardless.

## Confirmed Root Cause (2026-08-01, from device logcat)

The user reproduced the crash and provided the real `FATAL EXCEPTION` stack trace:

```
org.koin.core.error.InstanceCreationException: Could not create instance for '[Factory: 'com.example.proyectofinal.ui.MainRouterViewModel']'
Caused by: org.koin.core.error.NoDefinitionFoundException: No definition found for type 'com.example.proyectofinal.ui.MainTab'. Check your Modules configuration and add missing type and/or qualifier!
    at com.example.proyectofinal.di.AppModuleKt$appModule$lambda$0$$inlined$viewModelOf$default$6.invoke(ViewModelOf.kt:226)
```

Crash site: `AuthenticatedHomeScaffold.kt:96` (`koinViewModel<MainRouterViewModel>()`), reached via `App.kt:72` → `AuthGate` → `AuthView.COURSE`.

Root cause: `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/MainRouter.kt:10` declares
```kotlin
class MainRouterViewModel(initialTab: MainTab = MainTab.HOME) : ViewModel()
```
and `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt:63` registers it with
```kotlin
viewModelOf(::MainRouterViewModel)
```
Koin's `viewModelOf`/`constructorOf` reflection DSL resolves **every** constructor parameter through the container, ignoring Kotlin default parameter values. Since no module binds `MainTab` (an enum with no business reason to be DI-resolved), resolution fails and the ViewModel factory throws — every single time `AuthenticatedHomeScaffold` composes, i.e. every time onboarding completes or the app cold-starts already onboarded.

This was invisible during earlier manual testing of `AuthView.COURSE` in isolation because... it wouldn't have been — this crash is unconditional on any first entry to `AuthView.COURSE`. It only reads as "onboarding-specific" because onboarding completion is the transition that first triggers it in the flow under test.

Fix: replace the reflective `viewModelOf(::MainRouterViewModel)` registration with an explicit lambda `viewModel { MainRouterViewModel() }` (no Koin-managed parameter), or drop the constructor parameter's default and make `MainTab.HOME` the hardcoded initial state. The lambda form is preferred — it matches how Koin recommends handling constructor params with local/default values instead of DI-resolved dependencies.

**Ready for Proposal: Yes.**
