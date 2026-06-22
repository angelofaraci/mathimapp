# Proposal: Configurable API Base URL

## Intent

The API base URL is hardcoded to `http://10.0.2.2:8080` in `NetworkModule.kt`, which only works on the Android emulator. iOS Simulator, desktop JVM, and physical devices cannot reach the backend. We need platform-appropriate defaults and a build-time override mechanism for developer flexibility.

## Scope

### In Scope
- Platform-specific default base URLs (Android emulator, iOS Simulator, JVM desktop)
- Android `BuildConfig.API_BASE_URL` for build-time override
- JVM system property / environment fallback
- Update `NetworkModule.kt` to use resolved URL
- Server CORS note for LAN IP testing

### Out of Scope
- Runtime URL switching in the UI
- iOS runtime override (Info.plist bridge)
- Production deployment configuration
- BuildKonfig or third-party plugins

## Capabilities

### New Capabilities
None

### Modified Capabilities
None

## Approach

Adopt `expect/actual` pattern consistent with existing `DatabaseDriverFactory` and `Platform` abstractions:

1. Declare `expect fun getApiBaseUrl(): String` in `commonMain/di/`
2. Provide `actual` implementations:
   - Android: `BuildConfig.API_BASE_URL` (default `http://10.0.2.2:8080`, overridable in `build.gradle.kts`)
   - JVM: `System.getProperty("api.base.url") ?: System.getenv("API_BASE_URL") ?: "http://localhost:8080"`
   - iOS: `"http://localhost:8080"`
3. Wire `NetworkModule.kt` to `ApiConfig(baseUrl = getApiBaseUrl())`
4. Add `buildConfigField` to `composeApp/build.gradle.kts`

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/di/NetworkModule.kt` | Modified | Replace hardcoded `DefaultBaseUrl` with `getApiBaseUrl()` |
| `composeApp/src/commonMain/di/` | New | `expect fun getApiBaseUrl()` declaration |
| `composeApp/src/androidMain/di/` | New | `actual` reading `BuildConfig.API_BASE_URL` |
| `composeApp/src/jvmMain/di/` | New | `actual` with system property/env fallback |
| `composeApp/src/iosMain/di/` | New | `actual` returning `localhost:8080` |
| `composeApp/build.gradle.kts` | Modified | Add `buildConfigField("String", "API_BASE_URL", ...)` |
| `server/src/main/kotlin/plugins/Cors.kt` | Note | Developers may need to add LAN IPs for physical device testing |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| CORS mismatch on LAN IPs | Med | Document that `server/plugins/Cors.kt` must include the chosen LAN IP |
| BuildConfig deprecation in future AGP | Low | Minor maintenance; `buildFeatures.buildConfig = true` if needed later |
| Koin graph resolution failure | Low | Run `composeApp:jvmTest` to verify `ApiConfig` still resolves |

## Rollback Plan

Revert `NetworkModule.kt` to `private const val DefaultBaseUrl = "http://10.0.2.2:8080"` and delete the `expect/actual` source files and `buildConfigField` entry.

## Dependencies

None

## Success Criteria

- [ ] `./gradlew :composeApp:assembleDebug` succeeds with new `buildConfigField`
- [ ] `./gradlew :composeApp:jvmTest` passes (Koin graph resolves)
- [ ] Desktop target connects to `localhost:8080` by default
- [ ] Android emulator connects to `10.0.2.2:8080` by default
- [ ] Developer can override Android URL via `buildConfigField` for physical device testing
