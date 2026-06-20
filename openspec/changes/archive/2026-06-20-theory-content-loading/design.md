# Design: Theory Content Loading

## Technical Approach

Implement the minimal extension from the proposal: keep `Lesson` as the MVP theory/topic container, add `schoolYear` to `Course`, expose filtered official-course loading, and add a dedicated theory update route. Contracts stay in `shared`, persistence/auth in `server`, and KMP consumption/cache in `composeApp`. Backend changes land first so frontend code only consumes stable payloads.

## Architecture Decisions

| Option | Tradeoff | Decision |
|---|---|---|
| Keep theory inline on `Lessons.theoryContent` | Fast and matches current payloads, but keeps topic/lesson terminology debt | Chosen for MVP to stay within the review budget and avoid a `Topic` or `TheoryContent` refactor. |
| Put `TheoryUpdateRequest` in `shared` | Slightly broadens shared contracts, but both KMP client and server serialize it | Chosen because the delta spec requires a shared request shape. |
| Filter `/courses/official` with query parameter | Preserves existing route while adding curriculum filtering | Chosen as `GET /courses/official?schoolYear={year}`; invalid non-numeric values return 400. |
| Authorize theory separately from generic lesson update | Duplicates some ownership checks, but prevents accidental broad admin/teacher writes | Chosen: admin only official lessons; teacher only own courses; unauthenticated users rely on JWT challenge. |

## Data Flow

    KMP CourseViewModel/Repository ──GET /courses/official?schoolYear=N──→ courseRoutes
              │                                                   │
              └──── SQLDelight CourseEntity.schoolYear ←── CourseService/Courses

    KMP LessonRepository ──PUT /lessons/{id}/theory──→ lessonRoutes
         TheoryUpdateRequest                         │
              │                                      ├─ currentUserId/currentRole
              └──── SQLDelight LessonEntity ←── LessonService.updateTheoryContent

## File Changes

| File | Action | Description |
|---|---|---|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modify | Add `Course.schoolYear: Int`; add shared `TheoryUpdateRequest(lessonId, theoryContent)`. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` | Modify | Add `Courses.schoolYear` integer column with default for existing rows. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ServiceMappers.kt` | Modify | Map `schoolYear` into shared `Course`. |
| `server/src/main/kotlin/com/example/proyectofinal/service/CourseService.kt` | Modify | Add `getOfficialCourses(schoolYear: Int?)` and persist `schoolYear` on create. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/courseRoutes.kt` | Modify | Parse optional `schoolYear`; reject non-numeric values with 400. |
| `server/src/main/kotlin/com/example/proyectofinal/service/LessonService.kt` | Modify | Add server-only lesson/course lookup for auth and `updateTheoryContent`. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/lessonRoutes.kt` | Modify | Add `PUT /lessons/{id}/theory` using explicit role/ownership checks. |
| `server/src/main/kotlin/com/example/proyectofinal/seed/SeedData.kt` | Modify | Seed official courses with a real `schoolYear`. |
| `server/src/test/kotlin/com/example/proyectofinal/ServerIntegrationTest.kt` | Modify | Cover filtered official courses and theory authorization/status codes. |
| `server/src/test/kotlin/com/example/proyectofinal/ServiceLayerTest.kt` | Modify | Cover school-year persistence/filtering and theory update persistence. |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modify | Add `CourseEntity.schoolYear`; update `insertCourse`; add filtered official query if needed. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/CourseApi.kt` | Modify | Add optional `schoolYear` query to official-course fetch. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/CourseRepository.kt` | Modify | Expose official course loading by optional school year. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorCourseRepository.kt` | Modify | Cache `schoolYear` with courses. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/LessonApi.kt` | Modify | Add `updateTheory(TheoryUpdateRequest)`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/LessonRepository.kt` | Modify | Expose theory update without requiring title changes. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorLessonRepository.kt` | Modify | Call theory endpoint and refresh local lesson cache from response. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/*RepositoryTest.kt` | Modify | Update DB adapters/insert calls and assert query/body behavior. |

## Interfaces / Contracts

```kotlin
@Serializable
data class Course(..., val schoolYear: Int = 0, ...)

@Serializable
data class TheoryUpdateRequest(val lessonId: String, val theoryContent: String)
```

`PUT /lessons/{id}/theory` validates the path id against `request.lessonId` and returns the updated `Lesson` so clients can cache the canonical `theoryContent`.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit/service | `schoolYear` mapping/filtering, theory update persistence | Extend `ServiceLayerTest` with H2 + Exposed fixtures. |
| Integration/backend | 401/403/200 for theory route; 400 for invalid `schoolYear`; filtered official catalog | Extend Ktor `ServerIntegrationTest`. |
| Client/common | Query parameter generation, shared request serialization, SQLDelight cache writes | Extend Ktor mock-engine repository tests and DB assertions. |
| Build | Contract alignment across modules | Run `./gradlew :server:test :composeApp:jvmTest`; use `:composeApp:androidUnitTest` if SQLDelight/common changes are risky. |

## Migration / Rollout

Database migration is required for `courses.school_year` and `CourseEntity.schoolYear`. Seed/default values should backfill existing official courses; local cache can be recreated or migrated because it is derived data.

## Open Questions

- [ ] Valid school-year range is still product-defined; implementation should initially enforce numeric input only unless product confirms bounds.
