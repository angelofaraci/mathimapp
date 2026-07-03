# Design: Course Detail Screen with Enrollment

## Technical Approach

Implement the slice as a tab-local flow inside `MainTab.ACTIVITIES`: catalog remains the entry state, and detail is a second local state keyed by `courseId`. The app reuses current repository/viewmodel patterns: `CourseDetailViewModel` loads `Course` from `CourseRepository.getCourseById()` and `UserProgress` from `UserRepository.getUserProgress()`, then derives enrollment and lesson completion locally. Specs covered: `course-detail-screen`, `course-enrollment`, `lesson-display`, `session-hydration`, and the catalog delta.

## Architecture Decisions

| Decision | Options | Tradeoff | Choice / Rationale |
|---|---|---|---|
| Activities navigation scope | Extend `MainRouter`; add local router | Global router is heavier and unnecessary for one tab | Add `ActivitiesTabRouter` local to ACTIVITIES so the bottom bar stays mounted and change remains isolated to `composeApp/ui/catalog/`. |
| Detail state ownership | Screen-only state; shared catalog VM; dedicated VM | Reusing catalog VM mixes list and detail concerns | Create `CourseDetailViewModel`; keep catalog filtering/enrollment in `CourseCatalogViewModel`; share only navigation callbacks. |
| Enrollment response | Return `Course`; return empty 200; return `UserProgress` | Returning progress avoids a second refresh call | `POST /courses/{id}/enroll` returns `UserProgress`, matching existing progress sync patterns in `KtorUserRepository` / `UserService`. |
| Lesson metrics source | Fetch each lesson separately; embed count in course response | Separate calls create N+1 and slower first paint | Add `Lesson.exerciseCount` in `shared` and compute it in one aggregated server query when building `GET /courses/{id}`. |

## Data Flow

```text
Catalog card tap ──→ ActivitiesTabRouter.Detail(courseId)
                     └─→ CourseDetailViewModel.load(courseId)
                           ├─→ CourseRepository.getCourseById(courseId)
                           └─→ UserRepository.getUserProgress(userId)
                                     ↓
                              Detail UI state

Enroll CTA ──→ CourseRepository.enroll(courseId)
              └─→ POST /courses/{id}/enroll
                    └─→ CourseService.enrollOfficialCourse(userId, courseId)
                          └─→ UserService.readUserProgress(userId)
                                     ↓
                              updated enrolledCourseIds
```

Server note: `CourseService.getCourseById()` already orders lessons by `Lessons.orderIndex`; the UI should render backend order and not invent client-side ordering logic.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` | Modify | Host the ACTIVITIES local router instead of rendering catalog directly. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/ActivitiesTabRouter.kt` | Create | Sealed router state and helpers for `Catalog` / `Detail(courseId)`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseCatalogScreen.kt` | Modify | Accept navigation/enroll callbacks; navigate to detail after successful enrollment or card tap. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseCatalogViewModel.kt` | Modify | Add enrollment action state and call repository `enroll(courseId)`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseDetailScreen.kt` | Create | Header, enrolled-only progress bar, ordered lesson list, retry/back actions. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseDetailViewModel.kt` | Create | Own loading/error/content state for a single course detail. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/CourseRepository.kt` | Modify | Add `suspend fun enroll(courseId: String): UserProgress`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/{CourseApi,KtorCourseRepository,MockCourseRepository}.kt` | Modify | Add enrollment API/client behavior and test doubles. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modify | Register `CourseDetailViewModel`. |
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modify | Add `exerciseCount: Int = 0` to `Lesson`. |
| `server/src/main/kotlin/com/example/proyectofinal/service/{CourseService,ServiceMappers,UserService}.kt` | Modify | Aggregate exercise counts, insert idempotent enrollment, expose progress reuse. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/courseRoutes.kt` | Modify | Add authenticated `POST /courses/{id}/enroll`. |
| `server/src/test/kotlin/com/example/proyectofinal/ServerIntegrationTest.kt` | Modify | Cover success, 401, 404, non-official, already-enrolled, and exerciseCount payload. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/{data/KtorCourseRepositoryTest.kt,ui/catalog/*}.kt` | Modify/Create | Verify enroll request, detail derivation, and catalog-to-detail state changes. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/auth/` | Modify | Session restore flow calls user-info endpoint and populates current user. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/domain/auth/` | Create | Verify hydration triggers on token-without-user, skips on hydrated session, handles 401/network errors. |

## Interfaces / Contracts

```kotlin
@Serializable
data class Lesson(
    val id: String,
    val courseId: String,
    val title: String,
    val theoryContent: String,
    val exercises: List<Exercise> = emptyList(),
    val exerciseCount: Int = 0,
)

interface CourseRepository {
    suspend fun enroll(courseId: String): UserProgress
}
```

`POST /courses/{id}/enroll` has no body, requires JWT, and returns `UserProgress`. For official courses, duplicate enrollment is idempotent and still returns 200 with current progress.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Detail VM derives `isEnrolled`, `completedLessonIds`, progress visibility, retry, inert lesson tap | `commonTest` with fake repositories and dispatcher control. |
| Integration | Repository POST path/auth handling and progress response sync | Extend `KtorCourseRepositoryTest` with MockEngine assertions. |
| Integration | Route auth/status codes, idempotent enrollment, official-course guard, exerciseCount in course payload | Extend `ServerIntegrationTest` with `testApplication` + H2 helpers. |
| Unit | Session hydration triggers on token-without-user, skips on hydrated session, handles 401/network errors | `commonTest` with fake auth session state and MockEngine. |

## Migration / Rollout

Sequence: (1) shared contract, (2) server route/service/tests, (3) app repository + DI, (4) catalog router/detail UI. Rollback points: revert router/screen wiring first; keep endpoint additive if client rollback is needed; `Lesson.exerciseCount` is backward-compatible because it defaults to `0`.

## Open Questions

- [x] Session hydration: INCLUDED in this change. When the app restores a valid authenticated session after restart, it must also recover the current user (not only the token) so course detail, home, and profile screens load without forcing re-login.
