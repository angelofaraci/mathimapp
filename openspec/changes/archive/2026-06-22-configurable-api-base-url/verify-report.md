# Verification Report

**Change**: configurable-api-base-url
**Version**: N/A
**Mode**: Standard (Strict TDD = false per `sdd-init/proyecto-final`)

## Completeness

| Metric | Value |
|--------|-------|
| Tasks total | 8 |
| Tasks complete | 8 |
| Tasks incomplete | 0 |

All implementation and verification tasks in `tasks.md` are checked and corroborated by `apply-progress.md`.

## Build & Tests Execution

**Build** (`./gradlew :composeApp:assembleDebug`): ✅ Passed

```text
> Task :composeApp:generateDebugBuildConfig
> Task :composeApp:assembleDebug
BUILD SUCCESSFUL in 4s
69 actionable tasks: 2 executed, 67 up-to-date
```

Generated `BuildConfig.java` contains the required field with the emulator default:

```java
// composeApp/build/generated/source/buildConfig/debug/com/example/proyectofinal/BuildConfig.java
public static final String API_BASE_URL = "http://10.0.2.2:8080";
```

**Tests** (`./gradlew :composeApp:jvmTest --rerun-tasks`): ✅ 32 passed / 0 failed / 0 skipped

```text
> Task :composeApp:compileTestKotlinJvm
> Task :composeApp:jvmTest
BUILD SUCCESSFUL in 1m 38s
19 actionable tasks: 19 executed
```

Test-suite breakdown (from `composeApp/build/test-results/jvmTest/TEST-*.xml`):

| Suite | tests | failures | errors | skipped |
|-------|-------|----------|--------|---------|
| `AppModuleTest` | 1 | 0 | 0 | 0 |
| `CourseViewModelTest` | 2 | 0 | 0 | 0 |
| `data.KtorCourseRepositoryTest` | 8 | 0 | 0 | 0 |
| `data.KtorExerciseRepositoryTest` | 4 | 0 | 0 | 0 |
| `data.KtorLessonRepositoryTest` | 6 | 0 | 0 | 0 |
| `data.KtorUserRepositoryTest` | 7 | 0 | 0 | 0 |
| `di.ApiBaseUrlJvmTest` | 2 | 0 | 0 | 0 |
| `NetworkClientTest` | 2 | 0 | 0 | 0 |
| **Total** | **32** | **0** | **0** | **0** |

**Coverage**: ➖ Not available (no coverage plugin configured for `composeApp`).

### Android Override Path (extra runtime evidence)

`./gradlew :composeApp:assembleDebug -PapiBaseUrl=http://192.168.1.42:8080` → BUILD SUCCESSFUL, and the generated field reflects the override:

```java
public static final String API_BASE_URL = "http://192.168.1.42:8080";
```

A subsequent plain `./gradlew :composeApp:assembleDebug` restored the default `http://10.0.2.2:8080`, confirming the property is optional and the default is preserved.

## Spec Compliance Matrix

| Requirement | Scenario | Test / Evidence | Result |
|-------------|----------|-----------------|--------|
| Platform Default Base URL Resolution | Default works without source edits | `ApiBaseUrlJvmTest > resolver uses environment value or localhost by default` (JVM, PASSED); Android `BuildConfig.API_BASE_URL` default generated via `assembleDebug`; iOS `ApiBaseUrl.ios.kt` returns `http://localhost:8080` (source) | ✅ COMPLIANT (iOS manual per design — see Warnings) |
| Platform Default Base URL Resolution | Base URL stays fixed during the session | `NetworkModule.kt` registers `single { ApiConfig(baseUrl = getApiBaseUrl()) }`; `*Api` classes cache `baseUrl = apiConfig.baseUrl` at construction; `AppModuleTest` PASSED (Koin graph resolves once); no runtime switching mechanism | ✅ COMPLIANT |
| Build-Time API Base URL Override | Developer overrides the base URL | `assembleDebug -PapiBaseUrl=http://192.168.1.42:8080` → `BuildConfig.API_BASE_URL = "http://192.168.1.42:8080"` (build evidence) | ✅ COMPLIANT |
| Build-Time API Base URL Override | LAN IP testing is supported | Same override build with a LAN IP succeeded and produced the LAN URL; no source edit required | ✅ COMPLIANT |
| Existing API Client Behavior Is Preserved | Requests still look the same | `NetworkClientTest` (2 PASSED) — auth header injection unchanged; `createHttpClient` untouched; repository suites (User/Course/Lesson/Exercise, 25 PASSED) verify serialization + request construction with `ApiConfig("https://example.test")` | ✅ COMPLIANT |
| Existing API Client Behavior Is Preserved | Platform defaults do not alter client semantics | Only the source of `baseUrl` changed (`getApiBaseUrl()` vs removed `DefaultBaseUrl`); `createHttpClient`, `ContentNegotiation`, and auth logic unchanged; repository suites PASSED | ✅ COMPLIANT |

**Compliance summary**: 6/6 scenarios compliant (iOS platform-default runtime coverage is manual by design; see Warnings).

## Correctness (Static Evidence)

| Requirement / Scope Item | Status | Notes |
|--------------------------|--------|-------|
| `ApiConfig.baseUrl` + `expect/actual getApiBaseUrl()` | ✅ Implemented | `commonMain/di/ApiBaseUrl.kt` declares `internal expect fun`; actuals present for android/jvm/ios |
| Android `BuildConfig.API_BASE_URL` from `-PapiBaseUrl`, default `http://10.0.2.2:8080` | ✅ Implemented | `build.gradle.kts` reads `providers.gradleProperty("apiBaseUrl").orElse("http://10.0.2.2:8080")`; `buildConfigField` + `buildFeatures.buildConfig = true`; generated field verified |
| JVM system property / env / default localhost | ✅ Implemented | `ApiBaseUrl.jvm.kt`: `System.getProperty("api.base.url") ?: System.getenv("API_BASE_URL") ?: "http://localhost:8080"` |
| iOS default localhost | ✅ Implemented | `ApiBaseUrl.ios.kt` returns `http://localhost:8080` |
| `NetworkModule.kt` no longer hardcodes base URL | ✅ Implemented | `DefaultBaseUrl` const removed; `ApiConfig(baseUrl = getApiBaseUrl())`; no leftover `10.0.2.2:8080` in `composeApp/src` |
| Existing client auth/serialization/request behavior preserved | ✅ Implemented | `NetworkClient.kt` (`createHttpClient`) unchanged; `*Api` classes unchanged; only `ApiConfig` source changed |
| `NetworkClientTest` dispatcher fix | ✅ Implemented | Uses `StandardTestDispatcher`, `Dispatchers.setMain()`/`resetMain()`, `runTest(dispatcher)`; restored full `jvmTest` green |

## Coherence (Design)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| Platform seam: `internal expect fun getApiBaseUrl()` under `commonMain/.../di` with per-platform actuals | ✅ Yes | Matches existing `DatabaseDriverFactory`/`Platform` pattern; no new Gradle plugin |
| Android override: `BuildConfig.API_BASE_URL` from Gradle property `apiBaseUrl`, default `http://10.0.2.2:8080` | ✅ Yes | Property optional; default preserved; override verified |
| Runtime behavior: resolve once via Koin `single { ApiConfig(baseUrl = getApiBaseUrl()) }` | ✅ Yes | No runtime switching; matches spec exclusion |
| Server CORS: do not change `server/.../Cors.kt` | ✅ Yes | No server files modified |
| File changes match design table | ✅ Yes | All 7 designed files created/modified; `NetworkClientTest` dispatcher fix is an in-scope testing repair noted in `apply-progress.md` |
| Testing strategy: JVM unit + Koin integration via `jvmTest` + Android build via `assembleDebug` + manual platform connectivity | ✅ Yes | All automated layers executed green; iOS/manual designated as manual |

## Issues Found

**CRITICAL**: None

**WARNING**:
- iOS default base URL has no automated runtime test. Verification relies on source inspection of `ApiBaseUrl.ios.kt` (a trivial constant). This is explicitly designated as manual in `design.md` (Testing Strategy → "Manual | Platform connectivity"), and no iOS test runner is available on the Linux build host, so per the "manual verification allowed" exception it is not CRITICAL. The `expect`/`actual` contract is satisfied structurally (commonMain expect has an iosMain actual).

**SUGGESTION**:
- If an macOS/iOS CI host becomes available, consider adding an `iosTest` (or shared `commonTest` run on iOS) that asserts `getApiBaseUrl()` returns the expected localhost default, to move iOS from manual to automated.
- `ApiBaseUrlJvmTest > resolver uses environment value or localhost by default` defensively accounts for a pre-set `API_BASE_URL` env var; no change needed — noted as good practice for CI environments where that variable may be set.

## Verdict

**PASS WITH WARNINGS**

All 8 tasks complete; both required commands pass (32 tests, 0 failures; `assembleDebug` green); Android `-PapiBaseUrl` override verified concretely and restored to default; existing client auth/serialization/request behavior preserved across 25 repository tests + 2 network tests. The single warning is the iOS default lacking an automated runtime test, which is explicitly manual per the agreed design and not feasible on the Linux host.
