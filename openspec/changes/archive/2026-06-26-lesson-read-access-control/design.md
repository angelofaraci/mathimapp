# Design: Lesson Read Access Control

## Technical Approach

This slice documents the backend behavior already implemented in `server/`. Read access is enforced in the service layer, then surfaced unchanged by Ktor routes. `LessonService.getLessonByIdForUser` and `getLessonsByCourseIdForUser` build a `CourseContentAccess` snapshot and delegate the policy decision to `canReadCourseContent` in `ContentReadAccess.kt`. The resulting behavior matches the proposal and both specs: official course access, enrolled-student access, course-owner access, and admin access.

## Architecture Decisions

| Decision | Option | Tradeoff | Decision |
|----------|--------|----------|----------|
| Policy location | Check access in routes or in services | Route checks would duplicate logic across lesson, exercise, and course endpoints | Keep policy in shared service helper `canReadCourseContent` so multiple endpoints reuse one rule |
| Access model | Per-lesson flags or derive from course metadata | Per-lesson flags would add state not present in schema | Derive lesson read access from `Courses.creatorId`, `Courses.isOfficial`, and enrollment rows |
| Student answer visibility | Separate student DTO or mapper-level masking | Separate DTOs add duplication | Keep one `Lesson`/`Exercise` model and hide `correctAnswer` in `toExercise(hideAnswers)` |

## Data Flow

```text
GET /lessons/{id} or /courses/{courseId}/lessons
        │
        ▼
lessonRoutes.kt extracts userId + role from JWT
        │
        ▼
LessonService loads lesson/course access metadata
        │
        ▼
ContentReadAccess.canReadCourseContent(...)
   ├─ ADMIN -> allow
   ├─ TEACHER -> creatorId == userId
   └─ STUDENT -> isOfficial || enrolled(userId, courseId)
        │
        ├─ false -> Forbidden
        └─ true  -> load lesson(s); mask answers for STUDENT
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `openspec/changes/lesson-read-access-control/design.md` | Create | Records the audited backend design and verification mapping |
| `server/src/main/kotlin/com/example/proyectofinal/service/ContentReadAccess.kt` | Verify | Shared access-control helper implementing the four visibility tiers |
| `server/src/main/kotlin/com/example/proyectofinal/service/LessonService.kt` | Verify | Applies the helper to lesson detail, lesson list, and student answer masking |
| `server/src/main/kotlin/com/example/proyectofinal/routes/lessonRoutes.kt` | Verify | Maps service results to `200/403/404` for lesson read endpoints |
| `server/src/main/kotlin/com/example/proyectofinal/routes/exerciseRoutes.kt` | Verify | Reuses lesson read access for exercise list visibility |
| `server/src/main/kotlin/com/example/proyectofinal/service/CourseService.kt` | Verify | Reuses the same helper for course-detail visibility evidence |
| `server/src/test/kotlin/com/example/proyectofinal/ServerIntegrationTest.kt` | Verify | Route-level evidence for visibility scopes |
| `server/src/test/kotlin/com/example/proyectofinal/ServiceLayerTest.kt` | Verify | Service-level evidence for access decisions and answer masking |

## Interfaces / Contracts

```kotlin
internal data class CourseContentAccess(
    val courseId: String,
    val creatorId: String,
    val isOfficial: Boolean
)

internal fun canReadCourseContent(access: CourseContentAccess, userId: String, role: UserRole): Boolean
```

Route contracts already in use:
- `GET /lessons/{id}` -> `LessonReadResult.Success | Forbidden | NotFound`
- `GET /courses/{courseId}/lessons` -> `LessonListReadResult.Success | Forbidden | NotFound`

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit/Service | Visibility tiers, `NotFound`, student answer masking | Existing `ServiceLayerTest.kt` cases for `getLessonByIdForUser` and `getLessonsByCourseIdForUser` |
| Integration | Route status codes across official/enrolled/owner/admin and forbidden outsiders | Existing `ServerIntegrationTest.kt` test `lesson read route enforces visibility scopes` |
| E2E | None in repo | Not required for this reconciliation slice |

## Migration / Rollout

No migration required. No backend rollout required because runtime behavior already exists. The only deliverable is OpenSpec documentation plus verification references.

## Open Questions

- [ ] None blocking. If stricter traceability is desired later, add explicit route-level assertions for `GET /lessons/{id}` returning `404` in integration tests.
