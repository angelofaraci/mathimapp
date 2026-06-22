## Exploration: configurable-api-base-url

### Current State

The API base URL is hardcoded to `http://10.0.2.2:8080` in `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/NetworkModule.kt` (line 7). This address is the Android emulator loopback alias and is unreachable from:

- Physical Android devices (need host LAN IP, e.g. `192.168.x.x:8080`)
- iOS Simulator (`localhost:8080` works, `10.0.2.2` does not)
- Desktop JVM target (`localhost:8080`)
- Any staging/production environment

`ApiConfig(baseUrl: String)` is injected via Koin and consumed by four API clients: `CourseApi`, `LessonApi`, `ExerciseApi`, and `UserApi`. The server CORS plugin already allows `localhost:8080`, `127.0.0.1:8080`, and `10.0.2.2:8080`, but will block requests from arbitrary LAN IPs unless expanded.

No build-configuration plugin (e.g. BuildKonfig) is currently in use. The codebase already employs `expect/actual` patterns for `Platform`, `DatabaseDriverFactory`, and `rememberPlatformModule`, so adding a new `expect/actual` pair for configuration resolution is idiomatic and consistent.

### Affected Areas

- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/NetworkModule.kt` — hardcoded `DefaultBaseUrl` must be replaced
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/ApiConfig.kt` — may be extended or kept as-is depending on approach
- `composeApp/build.gradle.kts` — may need `buildConfigField` if Android BuildConfig is used
- `composeApp/src/androidMain/kotlin/com/example/proyectofinal/di/` — new or updated `actual` implementation
- `composeApp/src/iosMain/kotlin/com/example/proyectofinal/di/` — new or updated `actual` implementation
- `composeApp/src/jvmMain/kotlin/com/example/proyectofinal/di/` — new or updated `actual` implementation
- `composeApp/src/commonTest/kotlin/com/example/proyectofinal/NetworkClientTest.kt` — tests do not assert `ApiConfig`; no breakage expected, but verify Koin graph still resolves
- `server/src/main/kotlin/com/example/proyectofinal/plugins/Cors.kt` — if developers switch to LAN IPs for physical-device testing, CORS must allow those hosts

### Approaches

#### 1. expect/actual with platform-specific constants (no new dependencies)

Create `expect fun getApiBaseUrl(): String` in `commonMain` and provide `actual` implementations per source set with sensible defaults:

- Android: `http://10.0.2.2:8080`
- iOS: `http://localhost:8080`
- JVM: `http://localhost:8080`

Then `networkModule` uses `ApiConfig(baseUrl = getApiBaseUrl())`.

- **Pros**: Zero new Gradle dependencies; leverages existing KMP patterns already in the repo; ~15 lines of Kotlin; trivial to review.
- **Cons**: Still compile-time constants; changing the URL for physical devices or staging requires editing source code or maintaining custom source-set branches; iOS physical device testing is not addressed automatically.
- **Effort**: Low

#### 2. expect/actual with environment / build-config override

Same `expect/actual` structure, but each platform reads from an override mechanism before falling back:

- **Android**: reads `BuildConfig.API_BASE_URL` defined in `composeApp/build.gradle.kts` via `buildConfigField`; changing the field value per build type or flavor enables physical-device and staging configurations without source edits.
- **JVM**: reads `System.getProperty("api.base.url")` or `System.getenv("API_BASE_URL")`, then falls back to `http://localhost:8080`.
- **iOS**: hardcodes `http://localhost:8080` for now (no Info.plist bridge exists yet).

- **Pros**: Android and desktop become truly configurable without touching Kotlin source; aligns with roadmap wording "build-config or environment-driven"; minimal additional complexity over Option 1.
- **Cons**: iOS still lacks runtime override; requires a small Gradle change (`buildConfigField`); Android `BuildConfig` is deprecated in newer AGP, though still widely used.
- **Effort**: Low

#### 3. BuildKonfig Gradle plugin

Add the `build-konfig` plugin to the version catalog and `composeApp/build.gradle.kts`. Configure per-target default values (e.g. `android` → `10.0.2.2:8080`, others → `localhost:8080`). Generated `BuildConfig` object is usable in `commonMain`.

- **Pros**: Single source of truth in Gradle; supports debug/release variants natively; no `expect/actual` boilerplate.
- **Cons**: New plugin dependency to maintain; not present in the current toolchain; overkill for a single string constant; adds Gradle DSL complexity.
- **Effort**: Medium

### Recommendation

**Adopt Approach 2** (expect/actual with environment / build-config override).

Rationale:
- It solves the immediate multi-platform default problem (emulator vs simulator vs desktop).
- It gives Android and desktop real configurability without source edits, satisfying the roadmap goal.
- It follows the existing `expect/actual` convention already established for `DatabaseDriverFactory` and `Platform`.
- It adds no new plugins or dependencies, keeping the change within the ~50-line estimate.
- It leaves a clear extension point for iOS runtime configuration later (Info.plist bridge or Swift-side injection).

Implementation sketch:
1. In `commonMain/di/`, declare `expect fun getApiBaseUrl(): String`.
2. Provide `actual` implementations:
   - `androidMain`: `BuildConfig.API_BASE_URL`
   - `jvmMain`: `System.getProperty("api.base.url") ?: System.getenv("API_BASE_URL") ?: "http://localhost:8080"`
   - `iosMain`: `"http://localhost:8080"`
3. Update `NetworkModule.kt` to use `ApiConfig(baseUrl = getApiBaseUrl())`.
4. Add `buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")` to `composeApp/build.gradle.kts` inside `android.defaultConfig`.
5. Document in the proposal how developers can override the URL for physical devices or staging.

### Risks

- **CORS mismatch**: If a developer sets a LAN IP (e.g. `192.168.1.42:8080`) on the client but forgets to update `server/plugins/Cors.kt`, requests will be rejected. The proposal should mention updating CORS for development scenarios.
- **BuildConfig deprecation**: AGP is moving away from `BuildConfig` by default. If the project upgrades AGP later, `buildConfigField` may require `buildFeatures.buildConfig = true`. This is a minor future maintenance note, not a blocker today.
- **iOS physical device gap**: iOS actual will still be `localhost:8080`. Physical iOS devices will need either a future runtime override mechanism or manual source edit. This is acceptable for the current roadmap scope but should be noted as a known limitation.
- **Test impact**: `NetworkClientTest` mocks the engine and does not assert `ApiConfig` values. The change should not break tests, but `ComposeAppCommonTest` verifies Koin resolution and should be run to confirm the graph still assembles.

### Verification Notes

- Run `./gradlew :composeApp:jvmTest` — Koin graph must resolve `ApiConfig` and `HttpClient` successfully.
- Run `./gradlew :composeApp:assembleDebug` — Android build must succeed with the new `buildConfigField`.
- Manual check: start the server, run the desktop target (`./gradlew :composeApp:run`), confirm courses load via `localhost:8080`.
- Manual check: run Android emulator, confirm courses still load via `10.0.2.2:8080`.
- If a physical Android device is available, override `API_BASE_URL` to the host LAN IP and verify connectivity.

### Ready for Proposal

**Yes.** The scope is clear, the affected files are identified, and the recommended approach is idiomatic for this KMP codebase. The orchestrator can proceed to `sdd-propose`. The proposal should include:
- rollback plan (revert `NetworkModule.kt` and remove `expect/actual` files)
- note about CORS coordination
- explicit mention of the iOS physical-device limitation
