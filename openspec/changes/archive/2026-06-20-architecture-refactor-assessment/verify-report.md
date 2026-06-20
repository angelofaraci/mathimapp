# Design Phase Gate Report: architecture-refactor-assessment

> **Gate type**: Design-phase gate (post `sdd-design`, pre `sdd-tasks`).
> No implementation has been performed; runtime test evidence is intentionally
> out of scope. This report judges design conformance, feasibility, and scope
> drift only.

- **Change**: `architecture-refactor-assessment`
- **Artifact store mode**: `openspec`
- **Artifacts inspected**: `exploration.md`, `proposal.md`, `design.md`
- **Source inspected**: `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`,
  `composeApp/.../NetworkClient.kt`, `App.kt`, `ui/CourseViewModel.kt`,
  `domain/SharedAliases.kt`, `sqldelight/.../db/AppDatabase.sq`,
  `commonTest/.../db/TestDriver.kt` (+ android/jvm/ios actuals),
  `server/.../Main.kt`, `server/.../routes/courseRoutes.kt`.

## 1. Contract Conformance

`design.md` exists and contains all required sections:

| Section | Present |
|---|---|
| Technical Approach | yes |
| Architecture Decisions (table) | yes |
| Data Flow | yes |
| File Changes (table) | yes |
| Interfaces / Contracts | yes |
| Testing Strategy | yes |
| Migration / Rollout | yes |
| Open Questions | yes ("None.") |

Verdict: **PASS** — contract complete.

## 2. Hallucination Check (concrete paths)

All concrete file paths / source sets / dependencies referenced in `design.md`
were validated against the repository:

| Path claim in design | Verified |
|---|---|
| `gradle/libs.versions.toml` | exists |
| `composeApp/build.gradle.kts` | exists; `commonMain.dependencies` block + version-catalog pattern confirmed |
| `composeApp/.../NetworkClient.kt` | exists; `BASE_URL`, `TokenHolder`, `createHttpClient(engine?)`, `httpClient` singleton confirmed exactly as described |
| `composeApp/.../App.kt` | exists; manual `MockCourseRepository()` + `CourseViewModel(repository)` via `remember` confirmed |
| `composeApp/.../ui/CourseViewModel.kt` | exists; single-arg constructor `CourseViewModel(repository: CourseRepository)` — Koin `koinViewModel()` compatible |
| `composeApp/.../domain/SharedAliases.kt` | exists; 7 typealiases (`Course`, `Exercise`, `ExerciseType`, `Lesson`, `User`, `UserProgress`, `UserRole`) confirmed |
| `composeApp/.../sqldelight/.../db/AppDatabase.sq` | exists; **imports `com.example.proyectofinal.domain.UserRole` and `...domain.ExerciseType`** (lines 1-2) — design's claim that alias deletion forces `.sq` import update is CORRECT |
| `composeApp/.../data/*Api.kt` | exists (`CourseApi`, `LessonApi`, `ExerciseApi`, `UserApi`) |
| `composeApp/src/{androidMain,iosMain,jvmMain}` | all exist; expect/actual pattern already established via `Platform.kt` |
| `server/.../routes/*.kt` | exists (`auth/user/course/lesson/exercise` Routes) |
| `server/.../Main.kt` | exists; route registration via `Application.*Routes()` extension functions confirmed |
| `server/.../service/*Service.kt` | new package; plausible, does not conflict with existing `database/`, `models/`, `routes/`, `plugins/`, `seed/` packages |
| New `di/` package in `composeApp` | new; plausible Koin convention, no conflict |

No hallucinated paths. No claim references a file that does not exist or a path
that conflicts with existing structure.

Verdict: **PASS** — no hallucination.

## 3. Scope Drift Check

| User / proposal decision | Design alignment |
|---|---|
| Incremental hardening, not a rewrite | "Harden the current KMP monorepo incrementally"; rejects Clean/Hexagonal rewrite in decision table |
| Feature scalability | DI + service layer directly enable scalability |
| No full rewrite | Explicitly preserved module shape; services-not-DAOs/use-cases decision |
| No production offline-first | "Keep repository behavior remote-first with local write-through"; offline-first sync out of scope; only DB factory wiring included |
| `shared` unchanged | "keep `shared` unchanged"; no `shared` rows in File Changes |
| iOS: no native wiring | iOS driver actual noted as "no Swift/Xcode wiring" |
| Koin in `composeApp` only | Design decision "Use Koin only in `composeApp`"; server uses passed-in services |

The design resolves all three proposal open questions (app-only Koin, PR
splitting conditional on 400-line forecast, coverage target). Resolving proposal
open questions is within design-phase authority.

Verdict: **PASS** — no drift.

## 4. Technical Feasibility

| Area | Feasibility | Evidence |
|---|---|---|
| Koin DI in `composeApp` | Feasible | `commonMain.dependencies` uses version-catalog aliases; `androidx.lifecycle.viewmodel.compose` already present → `koinViewModel()` integration supported; Koin KMP/compose/viewmodel artifacts exist |
| `ApiConfig` / `TokenStore` / `createHttpClient(tokenStore, engine?)` | Feasible | `createHttpClient(engine?)` factory already exists; `defaultRequest` reads `TokenHolder.accessToken` — parameterizing via `TokenStore` is a direct, mechanical change |
| SQLDelight `DatabaseDriverFactory` expect/actual | Feasible | `AppDatabase` configured in `sqldelight {}` (packageName `com.example.proyectofinal.db`); `AppDatabase.Schema` already referenced by existing `createTestDriver()` actuals; driver deps in catalog (`android-driver`, `native-driver`, `sqlite-driver`) |
| `koinViewModel<CourseViewModel>()` | Feasible | `CourseViewModel(repository)` is a single constructor param; Koin resolves it once `CourseRepository` is bound |
| Server service extraction | Feasible | `courseRoutes.kt` confirmed to mix `dbQuery` (Exposed), `requireSelfOrAdmin`, HTTP parsing, and DTO mapping across 8 endpoints — directly extractable into `CourseService` |
| Server route thinning | Feasible | `Main.kt` registers routes via extension functions; passing service instances into route registration is a coherent change |

Verdict: **PASS WITH WARNINGS** — see findings; two feasibility gaps need attention
during task planning but do not block it.

## 5. Findings

### CRITICAL
None.

### WARNING

- **W1 — JVM `DatabaseDriverFactory` actual is mandatory, not conditional.**
  The File Changes row for `DatabaseDriverFactory.jvm.kt` is hedged "if desktop
  target must compile." But `jvm()` is an active KMP target in
  `composeApp/build.gradle.kts`, and an `expect` declared in `commonMain`
  requires an `actual` for **every** active target or compilation breaks.
  Task planning must treat the JVM actual as required, not optional.

- **W2 — JVM production driver dependency is missing from the plan.**
  `sqldelight.sqlite.driver` is currently declared only in `androidUnitTest` and
  `jvmTest`, not in `jvmMain`. A JVM production `DatabaseDriverFactory` actual
  (e.g., `JdbcSqliteDriver`) requires adding `libs.sqldelight.sqlite.driver` to
  `jvmMain.dependencies`. The `build.gradle.kts` modification row only mentions
  Koin; this driver dependency addition is unstated.

- **W3 — Authorization-adjacent DB lookups are under-specified.**
  Several routes perform a DB lookup *before* the auth check, e.g. in
  `courseRoutes.kt` `put /courses/{id}` and `delete /courses/{id}` fetch
  `creatorId` via `dbQuery`, then call `requireSelfOrAdmin(creatorId)`. The
  design says services "own Exposed queries inside `dbQuery`" and routes "keep
  HTTP/auth", but this fetch-then-authorize pattern spans both layers. The
  Testing Strategy row mentions "authorization-adjacent ownership lookup
  helpers" without placing them. Task planning must decide explicitly (service
  exposes `getCreatorId(id)` and route does auth, *or* service receives the
  principal and enforces ownership) or tasks will produce awkward/broken splits.

### SUGGESTION

- **S1 — `CourseService` interface sketch is illustrative only.** The design's
  Interfaces section lists 3 methods (`getOfficialCourses`, `getCourseById`,
  `createCourse`), but `courseRoutes.kt` has 8 endpoints (official, byId,
  byCreator, enrolled, create, update, delete, join). Task planning should
  enumerate the full method set per service to avoid under-scoped tasks.

- **S2 — Coverage target from proposal open question #3 is not reflected.** The
  proposal asked whether a 60% line-coverage target for new service tests is
  acceptable; the design's Testing Strategy gives no numeric target. Consider
  stating the target so verification has an objective bar.

- **S3 — Relationship to existing `createTestDriver()` is unstated.** A
  `createTestDriver()` expect/actual already exists across `commonTest` /
  `androidUnitTest` / `jvmTest` / `iosTest`. The new production
  `DatabaseDriverFactory` is distinct (production vs test), so there is no
  conflict, but noting the coexistence would prevent confusion during
  implementation.

- **S4 — Koin version compatibility with Kotlin 2.3.10 / Compose MP 1.8.0.**
  Koin 4.x targets KMP + Compose; using the Koin BOM (as the design proposes)
  is the right approach. Task planning should pin a Koin version confirmed
  compatible with `kotlin = 2.3.10` and `composeMultiplatform = 1.8.0` in
  `libs.versions.toml`.

## 6. Final Verdict

**PASS WITH WARNINGS**

The design is contract-complete, free of hallucinated paths, aligned with the
proposal and user decisions (incremental hardening, no rewrite, no
production offline-first, `shared` unchanged), and technically feasible across
Koin DI, SQLDelight driver factory, ViewModel wiring, and server service
extraction. The three WARNINGs are feasibility-precision gaps that task
planning must address (mandatory JVM actual, missing JVM driver dependency,
auth-adjacent DB lookup placement); none invalidate the design.

## 7. Recommendation

**proceed-to-tasks**

Carry the three WARNINGs into `sdd-tasks` as explicit task notes:
- Mark JVM `DatabaseDriverFactory` actual as required, plus its
  `jvmMain` sqlite-driver dependency addition.
- Specify where creatorId/userId ownership lookups live relative to
  `requireSelfOrAdmin` before authoring server service tasks.

## skill_resolutions

- `sdd-verify` SKILL.md loaded. Strict TDD module **not** loaded — this is a
  design-phase gate, not implementation verification; no runtime test evidence
  applies. Standard (non-TDD) path used, adapted to design-gate criteria per
  the orchestrator's instructions.
- `_shared` SDD references: not separately loaded; structured status was
  provided inline by the orchestrator (artifact paths + context files).
- Report persisted to openspec artifact store:
  `openspec/changes/architecture-refactor-assessment/verify-report.md`.
