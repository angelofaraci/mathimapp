# Design: Configurable API Base URL

## Technical Approach

Replace the emulator-only `DefaultBaseUrl` in `networkModule` with a small `expect/actual` resolver owned by `composeApp`. `ApiConfig` remains the injected contract and is still registered once in Koin, so the resolved URL is fixed for the app session as required by the spec. Android gets a generated `BuildConfig.API_BASE_URL` with a local-dev default and optional Gradle property override; JVM reads process configuration before falling back to localhost; iOS keeps the simulator-friendly localhost default.

## Architecture Decisions

| Concern | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Platform seam | Add `internal expect fun getApiBaseUrl(): String` under `composeApp/src/commonMain/.../di` with actuals per platform | BuildKonfig plugin; keep one common constant | Existing code already uses `expect/actual` for platform and database wiring, and this avoids a new Gradle plugin for one string. |
| Android override | Generate `BuildConfig.API_BASE_URL` from Gradle property `apiBaseUrl`, defaulting to `http://10.0.2.2:8080` | Hardcode LAN IP; source edit per device | Preserves emulator local dev while allowing `-PapiBaseUrl=http://<LAN-IP>:8080` without committing machine-specific values. |
| Runtime behavior | Resolve once through Koin `single { ApiConfig(baseUrl = getApiBaseUrl()) }` | Runtime settings screen; mutable config | The spec excludes runtime switching, and existing APIs cache `apiConfig.baseUrl`, so startup/build-time config matches current architecture. |
| Server CORS | Do not change `server/.../Cors.kt` for this change | Add LAN hosts or wildcard CORS | Current app targets are native Ktor clients, not browsers, so CORS is not the network gate for Android/iOS/JVM requests. Revisit only for a browser target or explicit `Origin` use. |

## Data Flow

```text
Android Gradle property/default ─┐
JVM system property/env/default ─┼─→ getApiBaseUrl() actual ─→ Koin ApiConfig ─→ Course/Lesson/Exercise/UserApi ─→ Ktor request URL
iOS localhost default ───────────┘
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/ApiBaseUrl.kt` | Create | Declares the common resolver contract. |
| `composeApp/src/androidMain/kotlin/com/example/proyectofinal/di/ApiBaseUrl.android.kt` | Create | Returns `BuildConfig.API_BASE_URL`. |
| `composeApp/src/jvmMain/kotlin/com/example/proyectofinal/di/ApiBaseUrl.jvm.kt` | Create | Reads `api.base.url`, then `API_BASE_URL`, then localhost. |
| `composeApp/src/iosMain/kotlin/com/example/proyectofinal/di/ApiBaseUrl.ios.kt` | Create | Returns `http://localhost:8080`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/NetworkModule.kt` | Modify | Replace `DefaultBaseUrl` with `getApiBaseUrl()`. |
| `composeApp/build.gradle.kts` | Modify | Enable/build Android `BuildConfig.API_BASE_URL` from `-PapiBaseUrl` with emulator default. |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/di/ApiBaseUrlJvmTest.kt` | Create | Verifies JVM default and system-property override. |

## Interfaces / Contracts

```kotlin
// commonMain
package com.example.proyectofinal.di

internal expect fun getApiBaseUrl(): String
```

Android build contract: `./gradlew :composeApp:assembleDebug -PapiBaseUrl=http://192.168.1.42:8080` produces `BuildConfig.API_BASE_URL` with that value; omitting the property keeps `http://10.0.2.2:8080`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | JVM resolver default and `api.base.url` override | New `jvmTest`, restoring the system property after each test. |
| Integration | Koin graph still resolves `ApiConfig`, `HttpClient`, repositories | Existing `ComposeAppCommonTest` via `./gradlew :composeApp:jvmTest`. |
| Build | Android generated field compiles | `./gradlew :composeApp:assembleDebug`. |
| Manual | Platform connectivity | Android emulator uses `10.0.2.2`; JVM/iOS simulator use `localhost`; physical Android uses `-PapiBaseUrl` LAN override. |

## Migration / Rollout

No data migration required. Roll out as an app-side configuration change only. Existing local Android emulator behavior remains the default; developers opt into LAN testing through a Gradle property.

## Open Questions

- [ ] None.
