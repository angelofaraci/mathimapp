# Apply Phase Gate Report (PR 1 / Unit 1): architecture-refactor-assessment

> **Gate type**: Apply-phase gate (post `sdd-apply`, pre commit/PR).
> Scope: PR 1 / Unit 1 only — App DI + `SharedAliases` removal.
> No commit or PR has been created yet; the working tree is reviewed as-is.

- **Change**: `architecture-refactor-assessment`
- **Artifact store mode**: `openspec`
- **Artifacts inspected**: `proposal.md`, `design.md`, `tasks.md`, `verify-report.md` (design gate), `exploration.md`
- **Source inspected**: working-tree diff (`git diff` + untracked `composeApp/src/*/kotlin/.../di/**`), `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`, `App.kt`, `NetworkClient.kt`, `*Api.kt`, `*Repository.kt`, `CourseViewModel.kt`, `AppDatabase.sq`, all `di/*.kt` (common + android/ios/jvm actuals), `MainActivity.kt`, `MainViewController.kt`, test sources, test result XML.
- **Mode**: Standard (Strict TDD not active — no `strict_tdd` config/runner).

## Completeness

| Metric | Value |
|--------|-------|
| Tasks total (in-slice: Ph1 1.1–1.3, Ph2 2.1–2.14, Ph3 3.1–3.4) | 20 |
| Tasks complete | 18 |
| Tasks incomplete (blocked by env) | 2 (2.14, 3.4) |
| Tasks out of slice (Ph4, Ph5) | not expected |

| Task | Status | Evidence |
|---|---|---|
| 1.1 Koin BOM + core/compose/viewmodel in `libs.versions.toml` | ✅ | `koin = "4.1.0"` + 4 catalog aliases added |
| 1.2 Koin deps in `composeApp/build.gradle.kts` `commonMain` | ✅ | BOM platform + `koin-core`/`koin-compose`/`koin-compose-viewmodel` |
| 1.3 `sqldelight-sqlite-driver` in `jvmMain.dependencies` | ✅ | added; addresses design-gate W2 |
| 2.1 `di/ApiConfig.kt` | ✅ | `data class ApiConfig(val baseUrl: String)` |
| 2.2 `di/TokenStore.kt` | ✅ | interface + `InMemoryTokenStore` |
| 2.3 `di/NetworkModule.kt` | ✅ | provides `ApiConfig` (default `10.0.2.2:8080`), `TokenStore`, `HttpClient` |
| 2.4 `di/DatabaseDriverFactory.kt` (expect) | ✅ | `expect class` with `createDriver()` |
| 2.5 android actual | ✅ | `AndroidSqliteDriver(Schema, context, "app.db")` |
| 2.6 ios actual | ✅ | `NativeSqliteDriver(Schema, "app.db")` |
| 2.7 jvm actual (mandatory per design-gate W1) | ✅ | `JdbcSqliteDriver("jdbc:sqlite:composeApp.db", Schema, migrateEmptySchema = true)` |
| 2.8 `di/AppModule.kt` | ✅ | binds `AppDatabase` (via factory + adapters), 4 APIs, 4 repos, `viewModelOf(::CourseViewModel)` |
| 2.9 `NetworkClient.kt` refactor | ✅ | globals removed; `createHttpClient(tokenStore, engine?)` factory |
| 2.10 4 `*Api.kt` inject `ApiConfig` + `HttpClient` | ✅ | constructors updated; `baseUrl` captured per-instance |
| 2.11 `*Repository.kt` / `*ViewModel.kt` compatible | ✅ | no structural change; import updates only |
| 2.12 `App()` wrapped in `KoinApplication`; `koinViewModel<CourseViewModel>()` | ✅ | `App.kt` restructured |
| 2.13 Preview composability with `MockCourseRepository` | ✅ (partial) | new private `AppPreview()` added; see W1 re: Android preview |
| 2.14 `:composeApp:assembleDebug` | ⏸ blocked | Android SDK missing in env |
| 3.1 Delete `domain/SharedAliases.kt` | ✅ | file deleted |
| 3.2 `AppDatabase.sq` imports → `models.*` | ✅ | `UserRole`/`ExerciseType` now from `com.example.proyectofinal.models` |
| 3.3 Update all `domain.*` model references → `models.*` | ✅ | no `domain.{Course,Exercise,...}` model imports remain; only `*Repository` interface imports remain (correct) |
| 3.4 `:composeApp:assembleDebug` | ⏸ blocked | Android SDK missing in env |

## Build & Tests Execution

**Build (JVM target)**: ✅ Passed
```text
$ ./gradlew :composeApp:compileKotlinJvm --console=plain
BUILD SUCCESSFUL in 10s
```
```text
$ ./gradlew :composeApp:jvmTest --console=plain --rerun-tasks
BUILD SUCCESSFUL in 1m 46s
w: DatabaseDriverFactory.kt: expect/actual classes are in Beta (see W2)
w: DatabaseDriverFactory.jvm.kt: expect/actual classes are in Beta
```

**Tests (JVM target)**: ✅ 25 passed / 0 failed / 0 skipped
```text
composeApp/build/test-results/jvmTest/*.xml
  KtorCourseRepositoryTest    tests=8 failures=0 errors=0 skipped=0
  KtorExerciseRepositoryTest  tests=4 failures=0 errors=0 skipped=0
  KtorLessonRepositoryTest    tests=5 failures=0 errors=0 skipped=0
  KtorUserRepositoryTest      tests=5 failures=0 errors=0 skipped=0
  NetworkClientTest           tests=2 failures=0 errors=0 skipped=0
  ComposeAppCommonTest        tests=1 failures=0 errors=0 skipped=0
```
Tests cover: token header injection/clearing (`NetworkClientTest`), repository remote-first write-through after constructor changes (4 repo test classes). All `*Api(httpClient, apiConfig)` constructor updates exercised at runtime.

**Build (Android target)**: ➖ Not run — `ANDROID_HOME`/`ANDROID_SDK_ROOT` unset, no `local.properties`, no `~/Android/Sdk`. Tasks 2.14 and 3.4 blocked by environment, as expected by orchestrator.

**Build (iOS target)**: ➖ Not run — Kotlin/Native toolchain not invoked in this gate. iOS actuals mirror JVM structure and use canonical `NativeSqliteDriver` API; low risk but unverified.

**Coverage**: ➖ Not available — no coverage plugin configured; coverage target not stated in design (design-gate S2).

## Spec Compliance Matrix

➖ **N/A** — no `specs/` delta authored for this change (artifacts are proposal + design + tasks only). Spec-scenario compliance is not applicable; verification falls back to task completion + design coherence + runtime evidence. Recorded as a skipped dimension.

## Correctness (Static Evidence)

| Requirement (from proposal success criteria) | Status | Notes |
|---|---|---|
| Koin injects `HttpClient`, repos, ViewModels with no globals | ✅ Implemented | `appModule` + `networkModule` bind all; `viewModelOf(::CourseViewModel)`; `BASE_URL`/`TokenHolder`/`httpClient` globals gone |
| `SharedAliases.kt` removed; project compiles | ✅ Implemented | deleted; JVM compile + tests green |
| Server routes contain no Exposed queries | ➖ Out of slice | Phase 4 (PR 2) — not expected here |
| Existing tests pass; no auth/contract regressions | ✅ Verified (JVM) | 25/25 pass; token-header behavior preserved (see behavioral matrix) |

| Behavioral risk area | Status | Evidence |
|---|---|---|
| Token header behavior | ✅ Preserved | `defaultRequest` reads `tokenStore.accessToken` per request; `NetworkClientTest` proves setting token after client creation injects header on next request, and clearing removes it |
| Base URL usage | ✅ Preserved | `ApiConfig` is Koin `single`; APIs cache `baseUrl` at construction — equivalent to old top-level `val BASE_URL` (fixed for app lifetime) |
| Preview compatibility | ⚠️ Partial | `AppPreview()` isolated with `MockCourseRepository` (good); but `MainActivity.kt`'s `AppAndroidPreview()` still calls `App()` directly → see W1 |
| DB driver initialization | ✅ Coherent | adapter wiring in `AppModule.createAppDatabase` matches test setup exactly (`EnumColumnAdapter` for `type`/`role`, `Int<->Long` adapter for `totalScore`); JVM `migrateEmptySchema = true` handles empty DB |

## Coherence (Design)

| Design decision | Followed? | Notes |
|---|---|---|
| Koin only in `composeApp` | ✅ Yes | no server changes; `server/**` untouched (grep: no `Service` files) |
| Remote-first write-through, no offline-first | ✅ Yes | repository bodies unchanged; only imports + constructor wiring |
| Delete `SharedAliases.kt`, import `shared` models directly | ✅ Yes | all model imports → `com.example.proyectofinal.models.*`; `AppDatabase.sq` updated |
| Services-not-DAOs (server) | ➖ Out of slice | Phase 4 |
| `DatabaseDriverFactory` expect/actual for all 3 targets (design-gate W1) | ✅ Yes | android + ios + jvm actuals all present; JVM treated as mandatory |
| `jvmMain` sqlite-driver dependency (design-gate W2) | ✅ Yes | `libs.sqldelight.sqlite.driver` added to `jvmMain.dependencies` |
| File Changes table fidelity | ⚠️ Deviation | `PlatformModule.kt` + 3 actuals were created but are NOT listed in design's File Changes table (see S3) |

## Issues Found

### CRITICAL

- **C1 — Out-of-scope file modified in working tree.**
  `openspec/changes/plataforma-aprendizaje-matematica/specs/learning/spec.md` (a DIFFERENT change's spec) has been modified (+32 lines: "Lesson theory access" and "Topic-scoped learner chatbot" requirements). This is unrelated to `architecture-refactor-assessment` PR 1 and violates the slice contract in `tasks.md` ("Unit 1: composeApp + gradle deps only"). It must be excluded from the PR 1 commit (unstaged/reverted) before proceeding. If the change is intentional, it belongs in a separate commit under the `plataforma-aprendizaje-matematica` change, not here.

### WARNING

- **W1 — Android preview regresses into real Koin + DB initialization.**
  `composeApp/src/androidMain/.../MainActivity.kt` still defines `AppAndroidPreview()` calling `App()` directly. `App()` now wraps content in `KoinApplication { modules(platformModule, appModule) }`, and `PlatformModule.android.kt` provides `DatabaseDriverFactory(context)` backed by `AndroidSqliteDriver`. Rendering the Android preview will therefore attempt to start a real Koin context and instantiate a real SQLite driver. The executor added an isolated `AppPreview()` in `App.kt` using `MockCourseRepository` (good), but did not route the Android preview through it. Fix: make `AppAndroidPreview()` call the isolated preview composable (or a no-Koin preview helper) instead of `App()`. Cannot be runtime-verified here (no Android SDK), but the structural risk is clear.

- **W2 — `expect class DatabaseDriverFactory` triggers Kotlin 2.3 Beta warning.**
  `compileKotlinJvm` emits: `'expect'/'actual' classes ... are in Beta. Consider using '-Xexpect-actual-classes' flag.` Compilation succeeds (warning only). To future-proof against the stabilization of KT-61573, either add `-Xexpect-actual-classes` to the Kotlin compiler args or refactor to an `expect fun createDriver(): SqlDriver` (no class). Not blocking for this PR.

- **W3 — Android and iOS targets unverified at runtime.**
  Tasks 2.14 and 3.4 (`:composeApp:assembleDebug`) remain unchecked due to missing Android SDK. Android-target compilation of the Koin wiring, `PlatformModule.android.kt`, and `DatabaseDriverFactory.android.kt` is NOT verified. iOS-target compilation is also NOT run. Only JVM-target compile + tests are green. This matches the orchestrator's stated environment blocker, but it means the PR 1 "compiles on all targets" success criterion is only partially evidenced.

### SUGGESTION

- **S1 — Base URL is cached per-API-instance.** `CourseApi`/`LessonApi`/`ExerciseApi`/`UserApi` cache `private val baseUrl = apiConfig.baseUrl` at construction. Since both `ApiConfig` and the APIs are Koin `single`s, the URL is immutable for the app lifetime — identical to the old top-level `val BASE_URL`. Acceptable and design-aligned; flagging only in case dynamic base-URL switching is ever desired.

- **S2 — Adapter wiring duplicated between `AppModule.createAppDatabase` and test setup.** Consider extracting a shared `AppDatabase` factory (`AppDatabaseFactory(driver): AppDatabase`) so production and test wiring cannot drift. Not blocking.

- **S3 — `PlatformModule.kt` + 3 actuals are not in the design's File Changes table.** The `rememberPlatformModule()` expect/actual is a sensible addition (it cleanly delivers the Android `Context`-dependent `DatabaseDriverFactory` into Koin), and it compiles. It is a design-deviation in documentation only; consider back-filling the design File Changes table for traceability before archive.

## Verdict

**PASS WITH WARNINGS**

The in-slice scope (Phase 1 + Phase 2.1–2.13 + Phase 3.1–3.3) is complete, internally coherent, and backed by passing JVM runtime evidence (25/25 tests, 0 failures). Token-header behavior, base-URL semantics, and DB adapter wiring are preserved/consistent. No server work leaked into the slice. The single CRITICAL is a scope-hygiene issue (an unrelated spec file modified in the working tree) that does not affect build/tests but must be excluded from the PR 1 commit. The WARNINGs are a preview-compatibility gap on Android (W1), a benign Kotlin Beta warning (W2), and the expected Android/iOS runtime-verification gap (W3).

## Recommendation

**fix-before-continuing** (scoped to the PR 1 commit)

Before committing PR 1:
1. Exclude/revert `openspec/changes/plataforma-aprendizaje-matematica/specs/learning/spec.md` from the PR 1 staging set (C1).
2. (Recommended) Update `MainActivity.kt`'s `AppAndroidPreview()` to use the isolated no-Koin preview path (W1).

After those, commit PR 1 and proceed to PR 2 (server service layer) — it is independent of PR 1 and can be launched in parallel per the tasks.md work-unit plan. Carry W2/W3 forward as notes for the PR 1 description (environment limitations + Beta warning).

## Verification commands considered/run

| Command | Run? | Result |
|---|---|---|
| `git status` / `git diff --stat` | yes | 26 files changed (composeApp + gradle + 1 out-of-scope spec) |
| `grep` for residual `domain.{Model}` imports | yes | only `*Repository` interface imports remain (correct) |
| `grep` for residual `SharedAliases`/`TokenHolder`/`BASE_URL`/`val httpClient` | yes | none in production sources |
| `./gradlew :composeApp:compileKotlinJvm` | yes | BUILD SUCCESSFUL |
| `./gradlew :composeApp:jvmTest --rerun-tasks` | yes | BUILD SUCCESSFUL; 25 tests, 0 failures |
| `./gradlew :composeApp:assembleDebug` | no | Android SDK missing (env blocker, expected) |
| iOS compile (`compileKotlinIosArm64`) | no | not invoked; iOS actuals low-risk by structural parity |

## skill_resolutions

- `sdd-verify` SKILL.md loaded via `skill()` tool. Executor-override path (this agent IS the `sdd-verify` sub-agent); not delegated further.
- `strict-tdd-verify.md` **not** loaded — Strict TDD not active (no `strict_tdd` config/runner; standard verify path used).
- `_shared` SDD references: not separately loaded; structured status was provided inline by the orchestrator (artifact paths + context files + assigned scope).
- Report persisted to openspec artifact store as a distinct file (`verify-report-apply-pr1.md`) to preserve the pre-existing design-gate `verify-report.md` audit trail.
