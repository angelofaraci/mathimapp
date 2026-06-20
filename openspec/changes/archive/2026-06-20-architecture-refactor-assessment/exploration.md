# Exploration: Architecture Refactor Assessment

## Current State

The project is a Kotlin Multiplatform monorepo with three modules: `composeApp` (Compose Multiplatform app), `shared` (cross-module contracts), and `server` (Ktor JVM backend with Exposed/PostgreSQL). An `iosApp/` Swift/Xcode wrapper exists but contains no business logic.

### Module Boundaries
Module boundaries largely **respect AGENTS.md ownership rules**:
- `shared` contains only `@Serializable` models and enums (`User`, `Course`, `Lesson`, `Exercise`, `UserProgress`, `UserRole`, `ExerciseType`). It is minimal, platform-agnostic, and framework-light. This is a strength.
- `server` owns Ktor routes, JWT auth, Exposed tables, and seed data.
- `composeApp` owns Compose UI, ViewModels, Ktor client, SQLDelight schema, and repository implementations.

### Server Architecture
- **Entry point**: `server/src/main/kotlin/com/example/proyectofinal/Main.kt` wires plugins, routes, and seeding.
- **Routes**: Directly contain HTTP handling, JWT authorization checks (`requireSelfOrAdmin`), and Exposed database queries. There is **no service layer**.
- **Persistence**: Exposed DSL tables in `database/Tables.kt` with a `dbQuery` helper for transaction wrapping.
- **Auth**: JWT via `auth0/java-jwt`, BCrypt password hashing, role-based access control.
- **Tests**: One integration test file (`ServerIntegrationTest.kt`, ~696 lines) covering auth, courses, lessons, exercises, progress, and cascade deletes. Tests use Ktor `testApplication` + H2 in-memory.

### App Architecture
- **UI layer**: Single screen `App.kt` with `CourseList`/`CourseCard` composables. Only one ViewModel (`CourseViewModel`) exists.
- **State management**: `MutableStateFlow` inside `ViewModel`, collected in Compose via `collectAsState()`.
- **Repositories**: Interfaces in `domain/` (`CourseRepository`, `LessonRepository`, `ExerciseRepository`, `UserRepository`). Implementations in `data/` (`Ktor*Repository`). A `MockCourseRepository` exists for previews/testing.
- **API layer**: Separate `*Api` classes (`CourseApi`, `LessonApi`, etc.) wrapping Ktor client calls.
- **Local persistence**: SQLDelight (`AppDatabase.sq`) with tables mirroring server entities. However, repositories **always hit the remote API first** and only write to local DB as a side effect. They never read from local DB, so the app is not offline-first.
- **Networking**: Global `httpClient` singleton, global `TokenHolder` for JWT, hardcoded `BASE_URL`. No dependency injection.
- **Tests**: Almost none. `ComposeAppCommonTest.kt` is a placeholder (`1 + 2 == 3`). Some repository tests exist (`KtorCourseRepositoryTest`, etc.) but rely on mocked engines.

### Key Files
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` — root Composable, manually wires ViewModel
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/NetworkClient.kt` — global client + token holder
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/SharedAliases.kt` — unnecessary typealias indirection
- `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq` — local schema
- `server/src/main/kotlin/com/example/proyectofinal/routes/*.kt` — route files mixing HTTP/auth/DB
- `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` — Exposed schema
- `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` — shared DTOs

## Affected Areas
- `server/src/main/kotlin/com/example/proyectofinal/routes/*.kt` — mixed concerns (HTTP + auth + DB queries)
- `server/src/main/kotlin/com/example/proyectofinal/models/` — DTOs mixed with request/response objects; could be better organized
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/NetworkClient.kt` — global mutable state
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` — manual ViewModel creation, no DI
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/Ktor*Repository.kt` — local DB is write-only side effect; no offline-first strategy
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/SharedAliases.kt` — unnecessary indirection

## Approaches

### 1. Enhanced Layered Architecture (Incremental)
Add missing layers without changing the overall module structure or tech stack.

- **Server**: Introduce a `service/` package between `routes/` and `database/`. Routes delegate to services; services own business logic and call repositories/DAOs.
- **App**: Introduce Koin for DI. Replace globals with injected `HttpClient` and config. Add `domain/usecase/` for coarse-grained operations (e.g., `LoadCoursesUseCase`). Make repositories offline-first (read local, fetch remote, update local).
- **Pros**: Low risk, incremental, preserves working code, easy to review in slices.
- **Cons**: Does not enforce strict dependency direction; long-term could still accumulate coupling.
- **Effort**: Low to Medium

### 2. Clean Architecture / Hexagonal (Full Refactor)
Strict separation: Entities → Use Cases → Interface Adapters → Frameworks.

- **Server**: Domain entities in a `domain/` module/package, use cases (interactors) for each feature, Ktor routes as adapters, Exposed as infrastructure.
- **App**: Domain models (or reuse `shared`), use cases in `domain/`, repositories as interfaces in `domain/`, ViewModels in `presentation/`, Compose in `ui/`, Ktor/SQLDelight in `data/`.
- **Pros**: Excellent testability, clear dependency direction, scales well.
- **Cons**: High effort for current codebase size; overkill if the team is small and the app is not yet feature-complete; heavy refactor risk.
- **Effort**: High

### 3. MVVM + Repository + DI + Offline-First (Pragmatic)
Keep the existing MVVM + Repository pattern but harden it with DI, offline-first, and small cleanups.

- **Server**: Add service layer and thin route handlers.
- **App**: Add Koin DI, replace `SharedAliases.kt` with direct imports, implement offline-first repository strategy, add unit tests for ViewModels and use cases.
- **Pros**: Matches current mental model, KMP community standard, good balance of quality and velocity.
- **Cons**: Less strict than Clean Architecture; requires discipline to not leak framework code into ViewModels.
- **Effort**: Medium

## Recommendation

**Adopt Approach 3 (Pragmatic MVVM + Repository + DI + Offline-First) with incremental server service-layer extraction.**

Reasoning:
- The codebase is young (one main screen, few ViewModels). A full Clean Architecture rewrite would stall feature delivery without proportional benefit.
- The existing module boundaries and `shared` contracts are already correct. Refactoring them would be wasted motion.
- The highest pain points are: lack of DI, global mutable state, server routes mixing concerns, and unused local persistence. All of these are fixable incrementally.
- Koin is the natural DI choice for KMP and integrates well with Compose `ViewModel`.

## What Should Be Refactored First (Priority Order)

1. **Dependency Injection in `composeApp`**
   - Replace manual ViewModel creation in `App.kt` with Koin.
   - Inject `HttpClient`, `BASE_URL`, and token storage instead of global singletons.
   - Impact: `NetworkClient.kt`, `App.kt`, all `*Api` and `*Repository` classes.

2. **Remove `SharedAliases.kt`**
   - Use `com.example.proyectofinal.models.*` directly. The typealias indirection adds noise without value.
   - Impact: `domain/SharedAliases.kt`, every file in `composeApp` that imports `domain.*` for models.

3. **Offline-First Repository Strategy**
   - Change `Ktor*Repository` to query SQLDelight first, return cached data immediately, then fetch remote and update DB + emit new data.
   - This makes the existing SQLDelight schema actually useful.
   - Impact: `data/KtorCourseRepository.kt`, `data/KtorLessonRepository.kt`, `data/KtorExerciseRepository.kt`, `data/KtorUserRepository.kt`.

4. **Server Service Layer**
   - Extract business logic from route files into `service/CourseService.kt`, `service/LessonService.kt`, etc.
   - Keep routes thin: parse input, call service, format response, handle auth.
   - Impact: `routes/*.kt`, new `service/` package.

## What Should NOT Be Refactored Now

- **Module structure**: `composeApp`/`shared`/`server` split is correct.
- **`shared` module**: Already minimal and clean.
- **Tech stack**: Ktor, Exposed, SQLDelight, Compose Multiplatform are appropriate.
- **Database schema**: Both server (Exposed) and app (SQLDelight) schemas are functional and aligned.
- **iOS bootstrap**: No native Swift logic to refactor.

## Risks

- **Scope creep**: Refactoring DI + offline-first + server services could balloon beyond the 400-line review budget if done in one PR. Must be split into reviewable slices.
- **Test debt**: The app has almost no meaningful tests. Refactoring without tests increases regression risk. Add tests for the slice being touched before refactoring it.
- **Offline-first complexity**: Implementing proper sync (conflict resolution, retries, queueing) is non-trivial. For now, a simple "local-first with remote refresh" pattern is sufficient; full sync can come later.
- **Koin learning curve**: If the team is unfamiliar with Koin in KMP, initial setup may take extra time.

## Ready for Proposal

**Yes.** The next phase should be `sdd-propose` for the pragmatic refactor path (DI + offline-first + server service layer). The orchestrator should tell the user:

- The architecture is **not critically broken**; it is a young codebase with missing layers, not a mess.
- The recommended path is **incremental hardening**, not a rewrite.
- Work should be delivered in **sliced PRs** (e.g., slice 1: Koin DI setup + `SharedAliases` removal; slice 2: offline-first repositories; slice 3: server service layer).
