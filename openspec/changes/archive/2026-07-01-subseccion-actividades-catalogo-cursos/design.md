# Design: Course Catalog in Activities Tab

## Technical Approach

Add discovery fields to the canonical shared `Course` contract, persist them in backend and SQLDelight storage, and render a new `CourseCatalogScreen` in `MainTab.ACTIVITIES`. The first slice keeps server filtering limited to existing `schoolYear`; search and topic chips stay client-side over the fetched official-course list. Enrollment CTA remains presentational only.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Reuse course contract | Extend `shared` `Course` with nullable `topic`, `difficulty`, `durationMinutes`, `xpReward` | New catalog DTO | Existing API/repository/serialization path already centers on `Course`; nullable additive fields keep backward compatibility and reduce mapping layers. |
| Filter ownership | Keep `schoolYear` on server, search/topic in `CourseCatalogViewModel` | New backend search/topic params | Spec requires client-side text matching; first slice avoids premature endpoint expansion and keeps review size down. |
| UI boundary | New `ui/catalog/` package with dedicated screen + view model; `AuthenticatedHomeScaffold` only swaps tab content | Put catalog into `App.kt` / reuse `CourseScreen` | Current tab scaffold should stay thin; catalog behavior differs from HOME and must preserve HOME as future enrolled-courses surface. |

## Data Flow

```text
LearnerProfileRepository -> schoolYear
        |                    
CourseCatalogViewModel -> CourseRepository.getOfficialCourses(schoolYear)
        |                    
    CourseApi ------> GET /courses/official?schoolYear=n
        |                    
   Ktor backend -> Exposed Courses -> Course JSON with discovery fields
        |                    
KtorCourseRepository -> SQLDelight CourseEntity cache -> UI state
        |                    
search text + selectedTopic applied in-memory -> visible cards
```

## File Changes

| File | Action | Description |
|---|---|---|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modify | Add nullable discovery fields to `Course`. |
| `server/src/main/resources/db/migration/V3__add_course_discovery_fields.sql` | Create | Add nullable course metadata columns. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` | Modify | Map new Exposed columns. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ServiceMappers.kt` | Modify | Serialize DB rows into shared `Course`. |
| `server/src/main/kotlin/com/example/proyectofinal/service/CourseService.kt` | Modify | Preserve discovery fields on reads/creates. |
| `server/src/main/kotlin/com/example/proyectofinal/seed/SeedData.kt` | Modify | Seed at least reference topics for official courses. |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modify | Add cached discovery columns to `CourseEntity`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorCourseRepository.kt` | Modify | Persist new fields locally. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modify | Register any new SQLDelight adapters if needed and new view model. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` | Modify | Route ACTIVITIES to catalog screen. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseCatalogScreen.kt` | Create | Search field, chips, list, empty/error/loading states. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseCatalogViewModel.kt` | Create | Own fetch + UI filters/state reduction. |

## Interfaces / Contracts

```kotlin
data class Course(
    ...,
    val topic: String? = null,
    val difficulty: String? = null,
    val durationMinutes: Int? = null,
    val xpReward: Int? = null,
)
```

`CourseCatalogUiState` should separate remote state (`Loading|Error|Success`) from local filters (`query`, `selectedTopic`). Topic chips are fixed v1 values: `Fracciones`, `Álgebra`, `Geometría`.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Course serialization/null handling | Update shared/client tests to decode nullable discovery fields. |
| Integration | `/courses/official` returns metadata and keeps school-year behavior | Extend `ServerIntegrationTest` with seeded metadata + invalid filter case. |
| Integration | Repository caches metadata in SQLDelight | Extend `KtorCourseRepositoryTest` DB assertions. |
| UI/ViewModel | Search/topic filtering and empty results | Add `commonTest` view-model tests; keep Compose UI tests optional for this slice. |

## Migration / Rollout

Migration is additive-only: nullable columns on `courses`, mirrored in local SQLDelight schema. Rollback is safe by reverting app/server code; old rows remain valid and null metadata is supported. No enrollment or HOME behavior changes ship in this slice.

## Implementation Slices

1. **PR1 contracts + backend**: shared `Course`, Flyway, Exposed/service/seed/tests.
2. **PR2 local persistence + repository**: SQLDelight schema, repository writes, client tests.
3. **PR3 catalog UI**: new `ui/catalog`, tab wiring, filter tests/previews.

## Open Questions

- [ ] Should v1 show raw backend topic strings only, or normalize accents/casing in the client before chip matching?
