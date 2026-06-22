# Tasks: Configurable API Base URL

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~80–100 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | single PR |
| Delivery strategy | auto-forecast |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | expect/actual resolver + NetworkModule wiring + build.gradle.kts + JVM test | PR 1 (single) | single PR; base = main |

## Phase 1: Infrastructure / Foundation

- [x] 1.1 Create `composeApp/src/commonMain/kotlin/.../di/ApiBaseUrl.kt` with `internal expect fun getApiBaseUrl(): String`
- [x] 1.2 Add `buildConfigField("String", "API_BASE_URL", ...)` and `buildFeatures.buildConfig = true` in `composeApp/build.gradle.kts` under `android {}`, defaulting to `http://10.0.2.2:8080`

## Phase 2: Platform Implementations

- [x] 2.1 Create `composeApp/src/androidMain/kotlin/.../di/ApiBaseUrl.android.kt` — actual returning `BuildConfig.API_BASE_URL`
- [x] 2.2 Create `composeApp/src/jvmMain/kotlin/.../di/ApiBaseUrl.jvm.kt` — actual with `System.getProperty("api.base.url")` → `System.getenv("API_BASE_URL")` → `"http://localhost:8080"` fallback
- [x] 2.3 Create `composeApp/src/iosMain/kotlin/.../di/ApiBaseUrl.ios.kt` — actual returning `"http://localhost:8080"`

## Phase 3: Wiring

- [x] 3.1 Modify `composeApp/src/commonMain/kotlin/.../di/NetworkModule.kt` — remove `DefaultBaseUrl` const, replace `ApiConfig(baseUrl = DefaultBaseUrl)` with `ApiConfig(baseUrl = getApiBaseUrl())`

## Phase 4: Testing

- [x] 4.1 Create `composeApp/src/jvmTest/kotlin/.../di/ApiBaseUrlJvmTest.kt` — test JVM default fallback and system-property override (restore property after each test)
- [x] 4.2 Run `./gradlew :composeApp:assembleDebug` to verify Android `BuildConfig.API_BASE_URL` compiles
- [x] 4.3 Run `./gradlew :composeApp:jvmTest` to verify Koin graph resolution and JVM resolver tests
