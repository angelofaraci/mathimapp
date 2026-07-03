## Exploration: Course Detail Screen (detalle-curso)

### Current State

The app uses a tab-only navigation model inside `AuthenticatedHomeScaffold`. There is **no screen-stack or detail navigation** today. The `ACTIVITIES` tab renders `CourseCatalogScreen`, which lists official courses fetched via `KtorCourseRepository` and displays them as cards with a no-op **"Inscribirse"** button.

The backend already serves `GET /courses/{id}` and embeds `lessons` (ordered by `orderIndex`) via `CourseService.getCourseById()`. However, the embedded `lessons` do **not** include their `exercises` list (the mapper calls `toLesson()` without exercises), so `Lesson.exercises` is always empty in the course response.

`UserProgress` is fetched via `GET /progress/{userId}` and synced locally. It tracks `completedLessonIds`, `completedExerciseIds`, `enrolledCourseIds`, and `totalScore`. Local SQLDelight already supports `EnrolledCourse`, `CompletedLesson`, and `CompletedExercise` tables.

There is **no endpoint** to enroll in an official course without a join code. `POST /courses/join` only accepts a `code`. Official courses may not have one, so the enrollment flow in the design is currently unimplemented on the backend.

### Affected Areas

- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/` — currently hosts the catalog; needs a sibling detail screen or a parent router.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` — `ACTIVITIES` tab content needs to host a local back-stack (Catalog → Detail).
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` — new ViewModel registration.
- `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` — may need `exerciseCount` on `Lesson` to avoid N+1 calls.
- `server/src/main/kotlin/com/example/proyectofinal/routes/courseRoutes.kt` — needs a codeless enrollment endpoint (`POST /courses/{id}/enroll`).
- `server/src/main/kotlin/com/example/proyectofinal/service/CourseService.kt` — needs enrollment logic for official courses.
- `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` / `ServiceMappers.kt` — may need `exerciseCount` aggregation or embedding.

### Domain Interpretation

Per the user's clarification, the reference image (`Fracciones - Nivel Básico`) is a **Course Detail** screen for a full course. The list items under **"Lecciones del curso"** are `Lesson` entities belonging to that `Course`. The progress bar ("2/8 lecciones") is derived from the user's `completedLessonIds` versus the total lessons in the course.

The screen therefore needs:
1. Course metadata (title, description, difficulty, lesson count, XP).
2. Enrollment CTA ("Inscribirse en este curso").
3. Course-level progress summary.
4. Ordered lesson list with per-lesson status (completed, available, locked).

### Approaches

#### Navigation

1. **ActivitiesTabRouter (local)**
   - Add a small sealed-class router inside the `ACTIVITIES` tab scope (e.g., `ActivitiesDestination.Catalog` / `ActivitiesDestination.Detail(courseId)`).
   - `CourseCatalogScreen` and `CourseDetailScreen` are siblings driven by this router.
   - **Pros:** Minimal blast radius; no scaffold changes; bottom bar stays visible (matches design); any tab can later get its own router.
   - **Cons:** Cannot deep-link to detail from `HOME` tab without extra plumbing (acceptable for first slice).
   - **Effort:** Low

2. **Extend MainRouter**
   - Add detail targets to the global `MainRouter` and teach `AuthenticatedHomeScaffold` to render them.
   - **Pros:** Any tab can navigate to detail globally.
   - **Cons:** Touches scaffold, router, and bottom-bar visibility logic. More files changed.
   - **Effort:** Medium

3. **Introduce Compose Navigation library**
   - Replace tab switching with `NavHost` inside the scaffold.
   - **Pros:** Standard long-term solution.
   - **Cons:** Heavy dependency addition; overkill for a single screen; requires significant refactor of existing tab setup.
   - **Effort:** High

#### Data / Backend

1. **Add `exerciseCount` to `Lesson` model**
   - Backend mapper computes `COUNT(exercises)` per lesson when building the course response.
   - `shared` model gets a new `exerciseCount: Int = 0` field.
   - **Pros:** Single payload; no extra API calls.
   - **Cons:** Requires shared + backend + DB migration if persisted locally (but can be nullable/default 0 to avoid migration).
   - **Effort:** Low

2. **Fetch exercises per lesson client-side**
   - `CourseDetailViewModel` calls `getExercisesByLessonId` for each lesson.
   - **Pros:** No backend contract change.
   - **Cons:** N+1 API calls; poor performance.
   - **Effort:** Low (client only), but bad UX.

#### Enrollment

1. **Add `POST /courses/{id}/enroll`**
   - New server endpoint inserts into `EnrolledCourses` for the current user, restricted to official or accessible courses.
   - **Pros:** Clean contract; enables the CTA.
   - **Cons:** Requires backend route + service + test.
   - **Effort:** Low

2. **Use local-only enrollment**
   - Write to `EnrolledCourse` table locally and sync later.
   - **Pros:** No backend work in slice 1.
   - **Cons:** Violates data consistency; other devices won't see enrollment.
   - **Effort:** Lowest, but **not recommended**.

### Recommendation

- **Navigation:** Approach 1 — `ActivitiesTabRouter` local to the `ACTIVITIES` tab. Create a new `ui/activities/` package or extend `ui/catalog/` to host `ActivitiesScreen` + `ActivitiesRouter`.
- **Data:** Approach 1 — Add `exerciseCount` to `Lesson` in `shared` and compute it in `CourseService.getCourseById()`.
- **Enrollment:** Approach 1 — Add `POST /courses/{id}/enroll` on the backend and expose it through `CourseRepository.enroll(courseId)`.
- **First slice scope:**
  1. `ActivitiesTabRouter` + `CourseDetailScreen` + `CourseDetailViewModel`.
  2. Fetch `Course` by ID (with lessons) and `UserProgress` (for completed/enrolled state).
  3. Render header, progress bar, lesson list with title + description + simple status (completed checkmark if in `completedLessonIds`, else arrow). **Do not implement locking logic yet** — show all lessons as available.
  4. Wire "Inscribirse" button to the new enrollment endpoint.
  5. Add `exerciseCount` to `Lesson` and display "{count} ejercicios" per lesson card.

### Risks

- **No navigation stack today.** Adding even a local router is a new pattern in the codebase. Must document it clearly so future screens follow suit.
- **Enrollment API gap.** If the backend slice is skipped, the "Inscribirse" button will be a no-op, which breaks the core design intent.
- **Lesson locking logic is undefined.** The design shows locked lessons (padlock). Implementing sequential unlocking requires a policy decision (e.g., lesson N must be completed before lesson N+1 is unlocked). This is a business rule that should be specified in the proposal/spec phase.
- **Student count missing.** The design shows "1,240 estudiantes". `Course` model has no such field. Adding it requires backend aggregation. Deferrable, but noted.
- **Cross-module change.** This touches `shared`, `composeApp`, and `server`. The first slice is small but spans three modules, so proposal/spec must define the contract changes first.

### Ready for Proposal

**Yes.** The exploration confirms the domain model (Course containing Lessons) and identifies the main blockers (navigation stack, enrollment endpoint, exercise count). The next phase should produce a proposal that:
1. Defines the `ActivitiesTabRouter` navigation pattern.
2. Specifies the `POST /courses/{id}/enroll` contract.
3. Specifies the `Lesson.exerciseCount` shared model change.
4. Keeps lesson-locking and student-count for a follow-up slice.
