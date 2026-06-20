# Tasks: Architecture Refactor Assessment

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~950–1100 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: App DI + alias removal → PR 2: Server service layer → PR 3: Test hardening |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | App DI + SharedAliases removal | PR 1 | Base: `main`. composeApp + gradle deps only. Includes `DatabaseDriverFactory` expect/actual for all 3 targets, JVM driver dep, Koin wiring. |
| 2 | Server service layer extraction | PR 2 | Base: `main`. Server only. 5 services, route thinning, auth-boundary helpers. Independent of PR 1. |
| 3 | Test hardening | PR 3 | Base: `main` (after PR 1 + PR 2). ViewModel tests, service tests, Koin module verification. |

PR 1 and PR 2 are independent and can be reviewed in parallel. PR 3 depends on both.

---

## Phase 1: Dependency Infrastructure

- [x] 1.1 Add Koin BOM + Koin-core/compose/viewmodel to `gradle/libs.versions.toml` (Koin 4.x compatible with Kotlin 2.3.10)
- [x] 1.2 Add Koin dependencies to `composeApp/build.gradle.kts` `commonMain.dependencies`
- [x] 1.3 Add `libs.sqldelight.sqlite.driver` to `composeApp/build.gradle.kts` `jvmMain.dependencies`

## Phase 2: Koin DI Setup (composeApp)

- [x] 2.1 Create `di/ApiConfig.kt` with `ApiConfig(baseUrl)` data class inside `com.example.proyectofinal.di`
- [x] 2.2 Create `di/TokenStore.kt` with `TokenStore` interface + `InMemoryTokenStore` in `com.example.proyectofinal.di`
- [x] 2.3 Create `di/NetworkModule.kt` — Koin module providing `ApiConfig`, `TokenStore`, `createHttpClient(tokenStore)` replacing global `BASE_URL`/`TokenHolder`/`httpClient`
- [x] 2.4 Create `di/DatabaseDriverFactory.kt` — `expect class DatabaseDriverFactory` in `com.example.proyectofinal.di`
- [x] 2.5 Create `di/DatabaseDriverFactory.android.kt` — Android SQLDelight driver actual in `androidMain`
- [x] 2.6 Create `di/DatabaseDriverFactory.ios.kt` — iOS Native driver actual in `iosMain`
- [x] 2.7 Create `di/DatabaseDriverFactory.jvm.kt` — JVM `JdbcSqliteDriver` actual in `jvmMain` (mandatory — `jvm()` is an active target)
- [x] 2.8 Create `di/AppModule.kt` — Koin module binding `ApiConfig`, `TokenStore`, `HttpClient`, `AppDatabase` (via driver factory), all 4 API classes, all 4 repository implementations, `CourseViewModel`
- [x] 2.9 Refactor `NetworkClient.kt` — Remove `BASE_URL`, `TokenHolder`, `httpClient` globals; keep `createHttpClient(tokenStore, engine?)` factory
- [x] 2.10 Refactor all 4 `*Api.kt` classes — Inject `ApiConfig` and `HttpClient` via constructor; stop reading global `BASE_URL`
- [x] 2.11 Update all `*Repository.kt` and `*ViewModel.kt` — No structural changes; constructors remain compatible with Koin resolution
- [x] 2.12 Wrap `App()` in `KoinApplication {}`; replace `remember { CourseViewModel(...) }` with `koinViewModel<CourseViewModel>()`
- [x] 2.13 Keep preview composability: `MockCourseRepository` constructor compatible with Koin or manual wiring
- [x] 2.14 Verify all existing gradle targets compile (`./gradlew :composeApp:assembleDebug`)

## Phase 3: SharedAliases Removal

- [x] 3.1 Delete `domain/SharedAliases.kt`
- [x] 3.2 Update `AppDatabase.sq` imports from `com.example.proyectofinal.domain.*` to `com.example.proyectofinal.models.*` (UserRole, ExerciseType)
- [x] 3.3 Update all 18 `import com.example.proyectofinal.domain.*` references across composeApp to `com.example.proyectofinal.models.*`
- [x] 3.4 Verify compile: `./gradlew :composeApp:assembleDebug`

## Phase 4: Server Service Layer

- [x] 4.1 Create `service/CourseService.kt` with full method set: `getOfficialCourses()`, `getCourseById(id)`, `getCoursesByCreator(creatorId)`, `getEnrolledCourses(userId)`, `createCourse(req)`, `updateCourse(id, req)`, `deleteCourse(id)`, `joinCourse(userId, code)`, `getCreatorId(id)` — all Exposed queries inside `dbQuery`
- [x] 4.2 Create `service/AuthService.kt` — auth/registration queries: `findUserByEmail`, `createUser`, `validateCredentials`
- [x] 4.3 Create `service/UserService.kt` — user/profile queries: `getUserById`, `updateUser`, `getUserProgress`, `updateProgress`
- [x] 4.4 Create `service/LessonService.kt` — lesson queries: `getLessonsByCourseId`, `getLessonById`, `createLesson`, `updateLesson`, `deleteLesson`
- [x] 4.5 Create `service/ExerciseService.kt` — exercise CRUD/ownership queries: `getExercisesByLessonId(lessonId, hideAnswers)`, `createExercise`, `updateExercise`, `deleteExercise`, `getLessonCreatorId`, `getCreatorId`
- [x] 4.6 Refactor `courseRoutes.kt` — Accept `CourseService` param; keep HTTP parsing + status mapping + auth only. Use `service.getCreatorId(id)` + `requireSelfOrAdmin()` pattern for fetch-then-authorize on PUT/DELETE
- [x] 4.7 Refactor `authRoutes.kt` — Accept `AuthService` param; keep HTTP/auth only
- [x] 4.8 Refactor `userRoutes.kt` — Accept `UserService` param; keep HTTP/auth only
- [x] 4.9 Refactor `lessonRoutes.kt` — Accept `LessonService` param; keep HTTP/auth only
- [x] 4.10 Refactor `exerciseRoutes.kt` — Accept `ExerciseService` param; keep HTTP/auth only
- [x] 4.11 Modify `Main.kt` — Instantiate services once; pass to route registration functions
- [x] 4.12 Verify compile + existing integration tests pass: `./gradlew :server:test`

## Phase 5: Test Hardening

- [x] 5.1 Write Koin module resolution test in `commonTest`: verify `AppModule.kt` resolves `HttpClient`, `CourseRepository`, `CourseViewModel`
- [x] 5.2 Write `CourseViewModel` test: assert Loading→Success and Loading→Error state transitions via mock repository
- [x] 5.3 Write `NetworkClientTest` update: verify `createHttpClient(tokenStore, engine?)` factory with `MockEngine`
- [x] 5.4 Write `CourseServiceTest` (or extend `ServerIntegrationTest`): verify all 8 course queries/commands with H2-backed tests; direct service tests are acceptable and `testApplication` remains optional
- [x] 5.5 Write `AuthServiceTest`: verify credential validation + user lookup paths
- [x] 5.6 Write `LessonServiceTest` / `ExerciseServiceTest`: verify CRUD operations via test H2
- [x] 5.7 Verify full test suite: `./gradlew :composeApp:jvmTest :server:test`
