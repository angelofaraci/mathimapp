# Tasks: Theory Content Loading

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 375–495 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (backend) → PR 2 (frontend) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

```
Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: Medium
```

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend: shared contracts + server routes/services + tests | PR 1 | base = main; ~260–340 lines |
| 2 | Frontend: composeApp SDLDelight/API/repository + tests | PR 2 | base = main (independent of PR 1 for contract alignment) |

## Phase 1: Shared Contracts

- [x] 1.1 Add `schoolYear: Int = 0` to `Course` in `shared/.../models/Models.kt`
- [x] 1.2 Add `@Serializable data class TheoryUpdateRequest(lessonId, theoryContent)` to `shared/.../models/Models.kt`

## Phase 2: Backend Implementation

- [x] 2.1 Add `schoolYear` integer column to `Courses` in `server/.../database/Tables.kt`
- [x] 2.2 Map `schoolYear` into shared `Course` in `server/.../service/ServiceMappers.kt`
- [x] 2.3 Add `getOfficialCourses(schoolYear: Int?)` to `server/.../service/CourseService.kt`
- [x] 2.4 Add `schoolYear` query param to `GET /courses/official` in `server/.../routes/courseRoutes.kt`; reject non-numeric with 400
- [x] 2.5 Add `updateTheoryContent(lessonId, content, userId, role)` with role/ownership checks to `server/.../service/LessonService.kt`
- [x] 2.6 Add `PUT /lessons/{id}/theory` in `server/.../routes/lessonRoutes.kt`; validate pathId == body.lessonId, enforce ADMIN/TEACHER auth
- [x] 2.7 Seed official courses with `schoolYear` in `server/.../seed/SeedData.kt`

## Phase 3: Frontend Consumption

- [x] 3.1 Add `schoolYear` column to `CourseEntity` in `composeApp/.../sqldelight/.../AppDatabase.sq`; update `insertCourse`
- [x] 3.2 Add optional `schoolYear` query param to `CourseApi.kt` official-course fetch
- [x] 3.3 Add `getOfficialCourses(schoolYear: Int?)` to `CourseRepository.kt` interface
- [x] 3.4 Cache `schoolYear` in `KtorCourseRepository.kt`; pass param through to API
- [x] 3.5 Add `updateTheory(request: TheoryUpdateRequest)` to `LessonApi.kt`
- [x] 3.6 Add `updateTheory(lessonId, content)` to `LessonRepository.kt` interface
- [x] 3.7 Implement `KtorLessonRepository.updateTheory` — call endpoint, refresh local cache from response

## Phase 4: Testing & Verification

- [x] 4.1 Extend `server/.../ServiceLayerTest.kt`: cover school-year filtering and theory update persistence
- [x] 4.2 Extend `server/.../ServerIntegrationTest.kt`: cover 401/403/200 for theory route, 400 for invalid schoolYear, filtered official catalog
- [x] 4.3 Extend `composeApp/.../data/*RepositoryTest.kt`: cover query-param generation, shared request serialization, SQLDelight cache writes
- [x] 4.4 Run `./gradlew :server:test :composeApp:jvmTest` and verify contract alignment
