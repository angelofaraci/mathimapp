## Implementation Progress

**Change**: configurable-api-base-url
**Mode**: Standard

### Completed Tasks
- [x] 1.1 Create `composeApp/src/commonMain/kotlin/.../di/ApiBaseUrl.kt` with `internal expect fun getApiBaseUrl(): String`
- [x] 1.2 Add `buildConfigField("String", "API_BASE_URL", ...)` and `buildFeatures.buildConfig = true` in `composeApp/build.gradle.kts` under `android {}`, defaulting to `http://10.0.2.2:8080`
- [x] 2.1 Create `composeApp/src/androidMain/kotlin/.../di/ApiBaseUrl.android.kt` — actual returning `BuildConfig.API_BASE_URL`
- [x] 2.2 Create `composeApp/src/jvmMain/kotlin/.../di/ApiBaseUrl.jvm.kt` — actual with `System.getProperty("api.base.url")` → `System.getenv("API_BASE_URL")` → `"http://localhost:8080"` fallback
- [x] 2.3 Create `composeApp/src/iosMain/kotlin/.../di/ApiBaseUrl.ios.kt` — actual returning `"http://localhost:8080"`
- [x] 3.1 Modify `composeApp/src/commonMain/kotlin/.../di/NetworkModule.kt` — remove `DefaultBaseUrl` const, replace `ApiConfig(baseUrl = DefaultBaseUrl)` with `ApiConfig(baseUrl = getApiBaseUrl())`
- [x] 4.1 Create `composeApp/src/jvmTest/kotlin/.../di/ApiBaseUrlJvmTest.kt` — test JVM default fallback and system-property override (restore property after each test)
- [x] 4.2 Run `./gradlew :composeApp:assembleDebug` to verify Android `BuildConfig.API_BASE_URL` compiles
- [x] 4.3 Run `./gradlew :composeApp:jvmTest` to verify Koin graph resolution and JVM resolver tests

### Files Changed
| File | Action | What Was Done |
|------|--------|---------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/ApiBaseUrl.kt` | Created | Declared the common `internal expect fun getApiBaseUrl()` seam. |
| `composeApp/src/androidMain/kotlin/com/example/proyectofinal/di/ApiBaseUrl.android.kt` | Created | Wired Android base URL resolution to generated `BuildConfig.API_BASE_URL`. |
| `composeApp/src/jvmMain/kotlin/com/example/proyectofinal/di/ApiBaseUrl.jvm.kt` | Created | Added JVM property → environment → localhost fallback resolution. |
| `composeApp/src/iosMain/kotlin/com/example/proyectofinal/di/ApiBaseUrl.ios.kt` | Created | Added iOS localhost default resolution. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/NetworkModule.kt` | Modified | Replaced the hardcoded emulator URL with `getApiBaseUrl()` when creating `ApiConfig`. |
| `composeApp/build.gradle.kts` | Modified | Added `apiBaseUrl` Gradle property handling plus Android `buildConfigField` and `buildFeatures.buildConfig = true`. |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/di/ApiBaseUrlJvmTest.kt` | Created | Added focused JVM coverage for default resolution and system-property override precedence. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/NetworkClientTest.kt` | Modified | Aligned the JVM test with the existing coroutine test pattern using `StandardTestDispatcher`, `Dispatchers.setMain()`, and `Dispatchers.resetMain()` so full `jvmTest` can run green. |
| `openspec/changes/configurable-api-base-url/tasks.md` | Modified | Marked all implementation and verification tasks complete. |

### Verification
| Command | Result |
|---------|--------|
| `./gradlew :composeApp:jvmTest` | Passed |
| `./gradlew :composeApp:assembleDebug` | Previous pass retained — not rerun because only `commonTest` dispatcher setup changed after the successful build evidence |

### Deviations from Design
None — implementation matches design.

### Issues Found
None.

### Remaining Tasks
- [ ] None

### Workload / PR Boundary
- Mode: single PR
- Current work unit: Unit 1 — configurable API base URL resolver + wiring + focused JVM coverage
- Boundary: composeApp-only config seam, platform actuals, Android build config generation, NetworkModule wiring, and verification commands for this change
- Estimated review budget impact: Low; localized build/config wiring across composeApp source sets with one focused JVM test file

### Status
8/8 tasks complete. Ready for verify.
