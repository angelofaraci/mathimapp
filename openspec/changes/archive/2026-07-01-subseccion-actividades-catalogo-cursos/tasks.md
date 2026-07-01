# Tasks: Course Catalog in Activities Tab

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 450-550 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Shared contract + backend persistence | PR 1 | base: feature-branch; includes server tests |
| 2 | Client local SQLDelight persistence | PR 2 | base: PR 1 branch; includes repo tests |
| 3 | Catalog screen + ACTIVITIES wiring | PR 3 | base: PR 2 branch; includes ViewModel tests |

## Phase 1: Contracts + Backend — PR 1

- [x] 1.1 Add `topic`, `difficulty`, `durationMinutes`, `xpReward` to `shared` `Course` model
- [x] 1.2 Create Flyway migration `V3__add_course_discovery_fields.sql` with nullable columns
- [x] 1.3 Add Exposed column definitions in `Tables.kt`
- [x] 1.4 Map new columns in `ServiceMappers.toCourse()`
- [x] 1.5 Populate discovery fields in `CourseService` create/update
- [x] 1.6 Add discovery metadata to seed official courses in `SeedData.kt`
- [x] 1.7 Add server integration test: `/courses/official` returns discovery fields

## Phase 2: Client Persistence — PR 2

- [x] 2.1 Add discovery columns to `CourseEntity` definition in `AppDatabase.sq`
- [x] 2.2 Update `insertCourse` query to persist new fields
- [x] 2.3 Persist discovery fields in `KtorCourseRepository.insertCourseToLocal()`
- [x] 2.4 Add repository test verifying SQLDelight caches discovery fields

## Phase 3: Catalog UI — PR 3

- [x] 3.1 Create `CourseCatalogViewModel` with fetch, search/topic filter, and `CourseCatalogUiState`
- [x] 3.2 Create `CourseCatalogScreen` with search bar, topic chips, course cards, and loading/error/empty states
- [x] 3.3 Add visual-only "Inscribirse" button on each card (no network call)
- [x] 3.4 Wire `MainTab.ACTIVITIES` to `CourseCatalogScreen` in `AuthenticatedHomeScaffold`
- [x] 3.5 Add ViewModel tests for search text + topic chip filtering
