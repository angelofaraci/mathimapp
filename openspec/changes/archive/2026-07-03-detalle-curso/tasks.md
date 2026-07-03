# Tasks: Course Detail Screen with Enrollment

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~650-850 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (shared+server) → PR 2 (repo+hydration) → PR 3 (UI) |
| Delivery strategy | auto-forecast |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Shared contract + server backend | PR 1 | independent; base=main or tracker |
| 2 | App repository + DI + session hydration | PR 2 | depends on PR 1 shared contract |
| 3 | Activities router + detail screen + ViewModel | PR 3 | depends on PR 2 repository layer |

## Phase 1: Shared Contract & Backend

- [x] 1.1 Add `exerciseCount: Int = 0` to `Lesson` in `shared/src/commonMain/.../Models.kt`
- [x] 1.2 Add `suspend fun enroll(courseId: String): UserProgress` to `CourseRepository`
- [x] 1.3 Add enrollment in `server/CourseService.kt` — idempotent insert + return UserProgress
- [x] 1.4 Add `POST /courses/{id}/enroll` route in `server/courseRoutes.kt` (JWT-guarded)
- [x] 1.5 Compute `exerciseCount` via `COUNT(exercises)` in `CourseService.getCourseById()`
- [x] 1.6 Extend `ServerIntegrationTest.kt`: success, 401, 404, non-official, already-enrolled, exerciseCount

## Phase 2: App Repository, DI & Session Hydration

- [x] 2.1 Add enrollment to `CourseApi`, `KtorCourseRepository`, and `MockCourseRepository`
- [x] 2.2 Register `CourseDetailViewModel` in `AppModule.kt`
- [x] 2.3 Add session hydration in `domain/auth/`: call user-info endpoint when token exists but user is null
- [x] 2.4 Write hydration `commonTest`: triggers on token-without-user, skips when hydrated, handles 401/network error

## Phase 3: Activities Router & Detail UI

- [x] 3.1 Create `ActivitiesTabRouter` sealed class with `Catalog` / `Detail(courseId)` states
- [x] 3.2 Modify `AuthenticatedHomeScaffold.kt` to host router instead of raw catalog
- [x] 3.3 Modify `CourseCatalogScreen.kt` to accept navigation + enrollment callbacks
- [x] 3.4 Modify `CourseCatalogViewModel.kt` to call `repository.enroll()` and emit navigation event
- [x] 3.5 Create `CourseDetailScreen.kt`: header, progress bar, ordered lesson list with inert taps
- [x] 3.6 Create `CourseDetailViewModel.kt`: load course + progress, derive isEnrolled/completedLessonIds
- [x] 3.7 Write UI `commonTest` for detail VM derivation, catalog-to-detail nav, enroll state update
