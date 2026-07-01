## Implementation Progress

**Change**: subseccion-actividades-catalogo-cursos
**Mode**: Standard

### Completed Tasks
- [x] 1.1 Add `topic`, `difficulty`, `durationMinutes`, `xpReward` to `shared` `Course` model
- [x] 1.2 Create Flyway migration `V3__add_course_discovery_fields.sql` with nullable columns
- [x] 1.3 Add Exposed column definitions in `Tables.kt`
- [x] 1.4 Map new columns in `ServiceMappers.toCourse()`
- [x] 1.5 Populate discovery fields in `CourseService` create/update
- [x] 1.6 Add discovery metadata to seed official courses in `SeedData.kt`
- [x] 1.7 Add server integration test: `/courses/official` returns discovery fields
- [x] 2.1 Add discovery columns to `CourseEntity` definition in `AppDatabase.sq`
- [x] 2.2 Update `insertCourse` query to persist new fields
- [x] 2.3 Persist discovery fields in `KtorCourseRepository.insertCourseToLocal()`
- [x] 2.4 Add repository test verifying SQLDelight caches discovery fields
- [x] 3.1 Create `CourseCatalogViewModel` with fetch, search/topic filter, and `CourseCatalogUiState`
- [x] 3.2 Create `CourseCatalogScreen` with search bar, topic chips, course cards, and loading/error/empty states
- [x] 3.3 Add visual-only "Inscribirse" button on each card (no network call)
- [x] 3.4 Wire `MainTab.ACTIVITIES` to `CourseCatalogScreen` in `AuthenticatedHomeScaffold`
- [x] 3.5 Add ViewModel tests for search text + topic chip filtering

### Files Changed
| File | Action | What Was Done |
|------|--------|---------------|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modified | Extended the shared `Course` contract with nullable discovery metadata fields. |
| `server/src/main/kotlin/com/example/proyectofinal/models/CourseDto.kt` | Modified | Added optional discovery fields to course create/update request payloads so server mutations can preserve the new metadata. |
| `server/src/main/resources/db/migration/V3__add_course_discovery_fields.sql` | Created | Added the additive Flyway migration for nullable topic, difficulty, duration, and XP columns with H2-compatible statements. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` | Modified | Declared nullable Exposed columns for the new course discovery fields. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ServiceMappers.kt` | Modified | Mapped persisted discovery metadata into the shared `Course` model. |
| `server/src/main/kotlin/com/example/proyectofinal/service/CourseService.kt` | Modified | Persisted discovery fields during course creation and update flows and returned them in responses. |
| `server/src/main/kotlin/com/example/proyectofinal/seed/SeedData.kt` | Modified | Seeded official courses with initial discovery metadata for catalog consumers. |
| `server/src/test/kotlin/com/example/proyectofinal/ServiceLayerTest.kt` | Modified | Extended course mutation coverage to assert discovery fields survive create/update flows. |
| `server/src/test/kotlin/com/example/proyectofinal/ServerIntegrationTest.kt` | Modified | Added `/courses/official` integration coverage for discovery metadata in the official course response. |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modified | Added SQLDelight cache columns for topic, difficulty, duration minutes, and XP reward, and extended the `insertCourse` statement to persist them. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorCourseRepository.kt` | Modified | Persisted discovery metadata whenever remote course responses are cached locally. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modified | Registered SQLDelight adapters for the new nullable `Int` discovery columns in the app database factory and added the new catalog view model to DI. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorCourseRepositoryTest.kt` | Modified | Added repository coverage asserting discovery metadata is cached in SQLDelight and updated database setup for the new schema. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ComposeAppCommonTest.kt` | Modified | Updated the shared composeApp test database helper with adapters for the new cached `Int` columns. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/SqlDelightLearnerProfileRepositoryTest.kt` | Modified | Updated the SQLDelight test database helper for the expanded `CourseEntity` schema. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorUserRepositoryTest.kt` | Modified | Updated course insert fixtures and database adapters so user repository tests compile against the expanded cache schema. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorExerciseRepositoryTest.kt` | Modified | Updated course seed fixtures and database adapters for the expanded cache schema. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorLessonRepositoryTest.kt` | Modified | Updated course seed fixtures and database adapters for the expanded cache schema. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseCatalogViewModel.kt` | Created | Added the ACTIVITIES catalog view model with school-year fetch, remote-state handling, and client-side search/topic filtering. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseCatalogScreen.kt` | Created | Built the catalog screen with search bar, fixed topic chips, metadata-rich course cards, loading/error/empty states, and a visual-only enrollment CTA. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` | Modified | Replaced the ACTIVITIES placeholder with the new course catalog screen while leaving HOME unchanged. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/catalog/CourseCatalogViewModelTest.kt` | Created | Added focused view-model tests for school-year fetch, search filtering, and topic-chip toggling. |
| `openspec/changes/subseccion-actividades-catalogo-cursos/tasks.md` | Modified | Marked Phase 1, Phase 2, and Phase 3 tasks complete across PR 1, PR 2, and PR 3. |
| `openspec/changes/subseccion-actividades-catalogo-cursos/apply-progress.md` | Modified | Merged PR 3 catalog UI progress into the cumulative implementation record without losing PR 1 or PR 2 status. |

### Verification
| Command | Result |
|---------|--------|
| `./gradlew :server:test` | Failed initially because `V3__add_course_discovery_fields.sql` used a multi-column `ALTER TABLE` statement that H2 rejected during Flyway migration execution. |
| `./gradlew :server:test` | Passed after splitting the migration into four `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` statements (`BUILD SUCCESSFUL`). |
| `./gradlew :composeApp:jvmTest --tests "com.example.proyectofinal.data.KtorCourseRepositoryTest"` | Passed (`BUILD SUCCESSFUL`). This was the smallest relevant client verification for the PR 2 SQLDelight/repository slice and also recompiled the composeApp test source set against the expanded `CourseEntity` schema. |
| `./gradlew :composeApp:jvmTest --tests "com.example.proyectofinal.ui.catalog.CourseCatalogViewModelTest"` | Failed initially because the new test asserted `List<Int>` against tracked `List<Int?>`, and the catalog screen's named `weight` import resolved to an internal property in commonMain during compilation. |
| `./gradlew :composeApp:jvmTest --tests "com.example.proyectofinal.ui.catalog.CourseCatalogViewModelTest"` | Passed after fixing the nullable assertion and switching the screen layout import (`BUILD SUCCESSFUL`). This was the smallest relevant composeApp verification for the PR 3 catalog UI/view-model slice. |

### Deviations from Design
None — implementation matches design.

### Issues Found
- H2 rejected the first draft of the Flyway migration when multiple `ADD COLUMN IF NOT EXISTS` clauses were combined in a single `ALTER TABLE` statement. Splitting the migration into separate additive statements fixed test compatibility without changing the schema outcome.
- Expanding `CourseEntity` with nullable `Int` columns required updating every composeApp `AppDatabase` factory/test helper with matching SQLDelight adapters before JVM tests would compile.
- The first PR 3 composeApp verification failed until the new catalog test used a nullable `schoolYear` assertion and the screen stopped using a named `weight` import that compiled against an internal layout property in commonMain.

### Remaining Tasks
- None — all tasks for this change are complete.

### Workload / PR Boundary
- Mode: stacked PR slice
- Current work unit: PR 3 — Phase 3 catalog UI
- Boundary: starts from the merged PR 2 persistence foundation and ends with the ACTIVITIES catalog screen, its dedicated view model/state, visual-only enrollment CTA, scaffold wiring, and focused view-model verification. It intentionally excludes HOME enrolled-course behavior and any enrollment endpoint/network call.
- Chain strategy: `stacked-to-main` — this slice is intended to stack on top of the previous PR branch until earlier slices merge.
- Estimated review budget impact: Focused composeApp UI slice with one new screen, one new view model, one targeted test class, and minimal integration changes in DI and scaffold wiring.

### Status
16/16 total tasks complete. Post-verify local DB remediation applied; ready for targeted re-verify/archive.

### Post-Verify Remediation: Local DB Upgrade Safety

| File | Action | What Was Done |
|------|--------|---------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/LocalDatabaseSchemaFixes.kt` | Created | Added a targeted SQLDelight schema repair step that inspects `CourseEntity` and adds the new discovery columns only when an existing persistent `app.db` is missing them. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modified | Applied the local schema repair immediately after driver creation and before constructing `AppDatabase`, so Android, iOS, and JVM app flows repair persisted databases before generated queries run. |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/di/LocalDatabaseSchemaFixesTest.kt` | Created | Added a focused JVM test that starts from the pre-remediation `CourseEntity` shape, runs the repair step, and proves the current generated SQLDelight insert/select path works afterward. |
| `openspec/changes/subseccion-actividades-catalogo-cursos/apply-progress.md` | Modified | Recorded the post-verify local DB remediation without overwriting the prior implementation batches. |
| `openspec/changes/subseccion-actividades-catalogo-cursos/verify-report.md` | Modified | Added a post-verify remediation note so the prior verification artifact no longer hides the discovered local DB upgrade blocker. |

#### Remediation Verification

| Command | Result |
|---------|--------|
| `./gradlew :composeApp:jvmTest --tests "com.example.proyectofinal.di.LocalDatabaseSchemaFixesTest"` | Passed (`BUILD SUCCESSFUL`). The targeted JVM test creates the pre-remediation `CourseEntity` schema, applies the repair step, and then exercises the current generated SQLDelight insert/select path with the new discovery fields. |

#### Remediation Notes

- Investigation confirmed `composeApp` had no checked-in SQLDelight `.sqm` migrations and still opened persistent `app.db` files directly on Android/iOS/JVM.
- Because the project already had versionless persisted databases in the wild, the smallest safe fix for this change was a targeted, idempotent column repair before `AppDatabase` opens, rather than introducing a broad historical migration chain during remediation.
