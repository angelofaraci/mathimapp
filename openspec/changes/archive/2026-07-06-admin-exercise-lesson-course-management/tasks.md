# Tasks: Admin Exercise / Lesson / Course Management

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 950–1100 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-on-risk |
| Chain strategy | feature-branch-chain |

```
Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High
```

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Schema + contracts + DTOs + ComposeApp | PR 1 | ~120 lines; base = feature/tracker |
| 2 | Server services + routes + auth + tests | PR 2 | ~590 lines; base = PR 1 branch |
| 3 | Admin-web CRUD pages + nav | PR 3 | ~365 lines; base = PR 2 branch |

## Phase 1: Schema & Contracts

- [x] 1.1 `shared/…/Models.kt` — `Lesson.courseId` nullable; add `creatorId`
- [x] 1.2 `server/…/Tables.kt` — Nullable `courseId`; add `creatorId`
- [x] 1.3 `server/…/V4__standalone_lessons.sql` — Alter FK nullable; add `creator_id`; backfill; CHECK constraint
- [x] 1.4 `server/…/LessonDto.kt` — `CreateLessonRequest.courseId` nullable; add `creatorId`
- [x] 1.5 `server/…/AdminDtos.kt` — Admin CRUD request/response DTOs for course/lesson/exercise
- [x] 1.6 `composeApp/…/AppDatabase.sq` — Nullable `courseId`; add `creatorId`; update insert queries

## Phase 2: Services & Auth

- [x] 2.1 `LessonService.kt` — Standalone ownership: fallback `creatorId` when `courseId` null
- [x] 2.2 `LessonService.kt` — Add `getLessonsByCourseIdAdmin()`, `listStandaloneLessons()`, admin-safe delete
- [x] 2.3 `ExerciseService.kt` — Resolve standalone lesson ownership for exercise auth
- [x] 2.4 `CourseService.kt` — Add `adminCreateCourse()`, `adminUpdateCourse()`, `adminDeleteCourse()`
- [x] 2.5 `ContentReadAccess.kt` — Standalone visibility: admin/creator only
- [x] 2.6 `ServiceMappers.kt` — Map `creatorId` in `toLesson()`

## Phase 3: Routes & Tests

- [x] 3.1 `adminRoutes.kt` — POST/PUT/DELETE `/admin/courses`
- [x] 3.2 `adminRoutes.kt` — GET/POST/PUT/DELETE `/admin/lessons` with `?courseId=` filter
- [x] 3.3 `adminRoutes.kt` — GET/POST/PUT/DELETE `/admin/exercises` with `?lessonId=` filter
- [x] 3.4 `lessonRoutes.kt` — Update existing routes for standalone `creatorId` auth resolution
- [x] 3.5 `exerciseRoutes.kt` — Update auth to resolve standalone lesson ownership
- [x] 3.6 `AdminIntegrationTest.kt` — Admin CRUD tests for all three entities, 400/404/403 scenarios
- [x] 3.7 `ServiceLayerTest.kt` — Standalone ownership and cascade tests

## Phase 4: Admin-Web UI

- [x] 4.1 `App.tsx` — Add nav links for Lessons and Exercises
- [x] 4.2 `Courses.tsx` — Add create/edit/delete with React Query mutations
- [x] 4.3 `pages/Lessons.tsx` — New: list+filter, CRUD with standalone toggle
- [x] 4.4 `pages/Exercises.tsx` — New: list+filter, CRUD with type selector
- [x] 4.5 Manual verification: build admin-web, walk CRUD flows in browser
