# Design: Architecture Refactor Assessment

## Technical Approach

Harden the current KMP monorepo incrementally: keep `shared` unchanged, add Koin to `composeApp`, remove model typealias indirection, and extract server business logic from Ktor routes into services. This maps to the proposal scope without adding offline-first sync or a Clean Architecture rewrite.

## Architecture Decisions

| Decision | Alternatives considered | Rationale |
|---|---|---|
| Use Koin only in `composeApp` for this change. | Add Koin to both app and server. | App has globals and manual ViewModel wiring; server can get testable seams by passing services into route functions without adding a second DI setup. |
| Keep repository behavior remote-first with local write-through. | Implement offline-first cache/read/refresh. | Offline-first sync is explicitly out of scope; only database factory wiring needed for DI is included. |
| Delete `SharedAliases.kt` and import shared models directly. | Keep aliases to hide shared package paths. | The aliases add noise, affect SQLDelight enum imports, and obscure that `shared` already owns contracts. |
| Extract services, not DAOs/use cases. | Full Clean/Hexagonal split. | Routes currently mix HTTP/auth/DB; services remove the biggest coupling while preserving the young codebase’s shape. |

## Data Flow

```text
Compose App -> KoinApplication + rememberPlatformModule() -> CourseViewModel -> CourseRepository
     -> CourseApi -> injected HttpClient + ApiConfig + TokenStore -> Ktor server

Ktor route -> auth helpers / request parsing -> Service -> dbQuery + Exposed tables
          -> shared/server DTO -> HTTP response
```

## File Changes

| File | Action | Description |
|---|---|---|
| `gradle/libs.versions.toml` | Modify | Add Koin BOM/core/compose/viewmodel aliases. |
| `composeApp/build.gradle.kts` | Modify | Add Koin dependencies in `commonMain`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Create | Koin module for `ApiConfig`, `TokenStore`, `HttpClient`, APIs, repositories, `AppDatabase`, and `CourseViewModel`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/PlatformModule.kt` | Create | `expect` composable helper exposing the platform-specific Koin module used by `App.kt`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/DatabaseDriverFactory.kt` | Create | `expect` factory used to build `AppDatabase` for production DI. |
| `composeApp/src/androidMain/.../di/PlatformModule.android.kt` | Create | Android actual that uses `LocalContext` to provide `DatabaseDriverFactory(context)` through Koin. |
| `composeApp/src/androidMain/.../di/DatabaseDriverFactory.android.kt` | Create | SQLDelight Android driver actual. |
| `composeApp/src/iosMain/.../di/PlatformModule.ios.kt` | Create | iOS actual that provides `DatabaseDriverFactory()` through Koin. |
| `composeApp/src/iosMain/.../di/DatabaseDriverFactory.ios.kt` | Create | SQLDelight Native driver actual; no Swift/Xcode wiring. |
| `composeApp/src/jvmMain/.../di/PlatformModule.jvm.kt` | Create | JVM actual that provides `DatabaseDriverFactory()` through Koin. |
| `composeApp/src/jvmMain/.../di/DatabaseDriverFactory.jvm.kt` | Create | JVM driver actual if desktop target must compile. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/NetworkClient.kt` | Modify | Replace `BASE_URL`, `TokenHolder`, `httpClient` with injected `ApiConfig`, `TokenStore`, and `createHttpClient(tokenStore, engine?)`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modify | Wrap app in `KoinApplication`; use `koinViewModel<CourseViewModel>()`; keep previews isolated with `MockCourseRepository` if needed. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/SharedAliases.kt` | Delete | Use `com.example.proyectofinal.models.*` directly. |
| `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq` | Modify | Change enum imports from domain aliases to shared model enums. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/*Api.kt` | Modify | Inject `ApiConfig`; stop reading global `BASE_URL`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/{data,domain,ui}/**/*.kt` | Modify | Replace alias imports with shared model imports. |
| `server/src/main/kotlin/com/example/proyectofinal/service/*Service.kt` | Create | `AuthService`, `UserService`, `CourseService`, `LessonService`, `ExerciseService`; services own Exposed queries inside `dbQuery`. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ServiceMappers.kt` | Create | Shared internal `ResultRow` mappers for `Course`, `Lesson`, `Exercise`, and `User` service responses. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/*.kt` | Modify | Route functions accept service parameters, keep HTTP/auth/status-code mapping only. |
| `server/src/main/kotlin/com/example/proyectofinal/Main.kt` | Modify | Instantiate services once and pass them to route registration. |

## Interfaces / Contracts

```kotlin
data class ApiConfig(val baseUrl: String)
interface TokenStore { var accessToken: String? }
class InMemoryTokenStore : TokenStore { override var accessToken: String? = null }

class CourseService {
    fun getOfficialCourses(): List<Course>
    fun getCourseById(id: String): Course?
    fun createCourse(request: CreateCourseRequest): Course
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| App unit | `TokenStore` header injection, Koin module resolution, `CourseViewModel` success/error states. | `composeApp/src/commonTest` with `kotlin.test`, `runTest`, Ktor `MockEngine`; keep SQLDelight actual drivers in `jvmTest`/`androidUnitTest`. |
| App repository | Existing remote-first write-through behavior after constructor/config changes. | Update current `Ktor*RepositoryTest` and `NetworkClientTest`; run `./gradlew :composeApp:jvmTest :composeApp:androidUnitTest`. |
| Server service | Service methods preserve query behavior and authorization-adjacent ownership lookup helpers. | Add focused service tests or extend `ServerIntegrationTest`; all Exposed access remains inside `dbQuery`. |
| Server integration | Routes still return existing status codes and DTO shapes. | Run `./gradlew :server:test`; keep Ktor `testApplication` + H2 setup. |

## Migration / Rollout

No data migration required. Deliver as review slices: app DI + alias removal, server services, then tests/coverage hardening. Ask before chained PR splitting if forecast exceeds 400 changed lines.

## Open Questions

None.
