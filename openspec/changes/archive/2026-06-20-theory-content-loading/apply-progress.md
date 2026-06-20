# Apply Progress: Theory Content Loading

## Change

- Change: `theory-content-loading`
- Mode: Standard
- Delivery mode: stacked PR slice
- Current work unit: PR 2 / frontend slice

## Completed Tasks

- [x] 1.1 Add `schoolYear: Int = 0` to `Course`
- [x] 1.2 Add shared `TheoryUpdateRequest`
- [x] 2.1 Add `Courses.schoolYear` column
- [x] 2.2 Map `schoolYear` into shared `Course`
- [x] 2.3 Add `getOfficialCourses(schoolYear: Int?)`
- [x] 2.4 Add `schoolYear` query parsing and 400 validation to `GET /courses/official`
- [x] 2.5 Add theory update service with role and ownership checks
- [x] 2.6 Add `PUT /lessons/{id}/theory` with path/body validation and auth handling
- [x] 2.7 Seed official courses with `schoolYear`
- [x] 3.1 Add `CourseEntity.schoolYear` and update `insertCourse`
- [x] 3.2 Add optional `schoolYear` query parameter to `CourseApi.fetchOfficialCourses`
- [x] 3.3 Add `getOfficialCourses(schoolYear: Int?)` to `CourseRepository`
- [x] 3.4 Cache `schoolYear` in `KtorCourseRepository` and pass the filter through to the API
- [x] 3.5 Add `LessonApi.updateTheory(request: TheoryUpdateRequest)`
- [x] 3.6 Add `LessonRepository.updateTheory(lessonId, content)`
- [x] 3.7 Implement `KtorLessonRepository.updateTheory` and refresh the local lesson cache from the canonical response
- [x] 4.1 Add backend service-layer coverage for school-year filtering and theory update persistence
- [x] 4.2 Add backend integration coverage for theory auth/filtering flows
- [x] 4.3 Add composeApp repository coverage for query params, shared request serialization, and SQLDelight cache writes
- [x] 4.4 Run `./gradlew :server:test :composeApp:jvmTest`

## Files Changed

| File | Action | Summary |
|---|---|---|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modified | Added `Course.schoolYear` and shared `TheoryUpdateRequest`. |
| `server/src/main/kotlin/com/example/proyectofinal/models/CourseDto.kt` | Modified | Added `schoolYear` support to server course requests. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` | Modified | Added `Courses.schoolYear` column with default `0`. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ServiceMappers.kt` | Modified | Mapped `schoolYear` from Exposed rows into shared `Course`. |
| `server/src/main/kotlin/com/example/proyectofinal/service/CourseService.kt` | Modified | Added optional official-course filtering and `schoolYear` persistence. |
| `server/src/main/kotlin/com/example/proyectofinal/service/LessonService.kt` | Modified | Added scoped theory update flow and result contract. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/courseRoutes.kt` | Modified | Parsed optional numeric `schoolYear` query and rejected invalid values. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/lessonRoutes.kt` | Modified | Added theory update endpoint with path/body validation and role gating. |
| `server/src/main/kotlin/com/example/proyectofinal/seed/SeedData.kt` | Modified | Seeded official course `schoolYear`. |
| `server/src/test/kotlin/com/example/proyectofinal/ServiceLayerTest.kt` | Modified | Added school-year and theory authorization persistence tests. |
| `server/src/test/kotlin/com/example/proyectofinal/ServerIntegrationTest.kt` | Modified | Added school-year filtering and theory route auth/status tests. |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modified | Added `CourseEntity.schoolYear` and updated the course insert query. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/CourseApi.kt` | Modified | Added optional `schoolYear` query parameter support for official course fetches. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/CourseRepository.kt` | Modified | Exposed filtered official course loading by optional school year. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorCourseRepository.kt` | Modified | Passed the school-year filter through and cached `schoolYear` locally. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/LessonApi.kt` | Modified | Added theory update endpoint client using shared request serialization. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/LessonRepository.kt` | Modified | Exposed a theory-only lesson update operation. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorLessonRepository.kt` | Modified | Reused lesson cache writes and refreshed local lesson state from theory updates. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/MockCourseRepository.kt` | Modified | Updated official-course filtering contract to accept an optional school year. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modified | Added the SQLDelight `CourseEntity` adapter required for `schoolYear`. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ComposeAppCommonTest.kt` | Modified | Updated test DI helpers for the new SQLDelight adapter and repository signature. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorCourseRepositoryTest.kt` | Modified | Added official-course query/filter cache assertions and updated DB writes for `schoolYear`. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorLessonRepositoryTest.kt` | Modified | Added shared theory update serialization and cache refresh coverage. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorExerciseRepositoryTest.kt` | Modified | Updated SQLDelight setup and seed course inserts for `schoolYear`. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorUserRepositoryTest.kt` | Modified | Updated SQLDelight setup for the new `CourseEntity` adapter. |
| `openspec/changes/theory-content-loading/tasks.md` | Modified | Marked frontend slice tasks complete. |

## Verification

| Command | Result |
|---|---|
| `./gradlew :server:test :composeApp:jvmTest` | Passed |
| `./gradlew :composeApp:jvmTest` | Passed |

## Deviations from Design

None — implementation matches the design intent.

## Issues Found

- Existing database initialization uses `SchemaUtils.create`, so persistent environments may still need an explicit migration path for pre-existing `courses` tables.
- The SQLDelight schema change required adding a `CourseEntity` adapter anywhere `AppDatabase` is constructed so `schoolYear` continues to map as `Int`.

## Remaining Tasks

- [ ] None

## Workload / PR Boundary

- Mode: stacked PR slice
- Boundary: composeApp SQLDelight schema + client API/repository updates + common repository tests only
- Review budget impact: frontend slice stayed focused on cache, request wiring, and repository verification

## Status

20/20 total tasks complete. Frontend slice is ready for verify.
