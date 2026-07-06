## Exploration: Admin Exercise / Lesson / Course Management

### Current State

**Admin Panel (`admin-web`)**
- React 18 + TypeScript + Vite + `react-router-dom` + `@tanstack/react-query`.
- Pages: `Login`, `Users`, `Courses`.
- `Courses` page is read-only (lists courses from `GET /admin/courses`).
- No create/update/delete for courses, lessons, or exercises.
- No lesson or exercise admin pages at all.

**Shared Contracts (`shared`)**
- `Course`, `Lesson`, `Exercise`, `ExerciseType` (MULTIPLE_CHOICE, TRUE_FALSE, INPUT_VALUE) live in `Models.kt`.
- `Lesson.courseId` is **non-nullable** (`String`).
- `Exercise.lessonId` is **non-nullable** (`String`).

**Backend (`server`)**
- Ktor + Exposed + PostgreSQL + Flyway.
- `Lessons.course_id` is a non-nullable FK to `courses.id` (`ON DELETE CASCADE`).
- `Exercises.lesson_id` is a non-nullable FK to `lessons.id` (`ON DELETE CASCADE`).
- Routes exist for basic CRUD on `/courses`, `/lessons`, `/exercises`.
- Admin routes (`/admin/users`, `/admin/courses`) are read-only listings.
- Auth for mutations is course-creator-based (`requireSelfOrAdmin`).
- `LessonService.getCreatorId` and `ExerciseService.getLessonCreatorId` derive ownership by joining through `Courses`, which breaks for standalone lessons.

**Compose App (`composeApp`)**
- No dedicated `LessonScreen` or `ExerciseScreen` exists.
- `CourseDetailScreen` only renders metadata (title, description, topic, difficulty, duration, XP) — it does **not** list lessons.
- `AuthenticatedHomeScaffold` has tabs: Home, Activities (catalog + detail), Progress, Profile.
- SQLDelight `LessonEntity.courseId` is non-nullable.
- The "existing screens for exercise types" referenced by the user are PNG mockups in `docs/ui/screens/` and `docs/ui/excercises/`, not implemented Compose code.

### Affected Areas

- `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` — `Lesson.courseId` must become nullable.
- `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` — `Lessons.courseId` FK must be nullable.
- `server/src/main/resources/db/migration/` — New migration to alter `lessons.course_id` to nullable.
- `server/src/main/kotlin/com/example/proyectofinal/service/LessonService.kt` — Ownership resolution via `Courses` join will fail for standalone lessons.
- `server/src/main/kotlin/com/example/proyectofinal/service/ExerciseService.kt` — `getLessonCreatorId` needs fallback for standalone lessons.
- `server/src/main/kotlin/com/example/proyectofinal/routes/lessonRoutes.kt` — `createLesson` currently requires `courseId`.
- `server/src/main/kotlin/com/example/proyectofinal/routes/adminRoutes.kt` — Needs admin CRUD endpoints for courses, lessons, exercises.
- `admin-web/src/App.tsx` — New routes for course/lesson/exercise management.
- `admin-web/src/pages/` — New pages: `CourseEditor`, `LessonManager`, `ExerciseManager`.
- `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` — `LessonEntity.courseId` nullable.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/` — Future lesson list and exercise gameplay screens.

### Approaches

1. **Big Bang (all at once)**
   - Change schema, shared model, backend routes, admin-web CRUD, and composeApp screens in a single PR.
   - **Pros:** Fastest end-to-end delivery.
   - **Cons:** PR likely exceeds 400 lines; hard to review; high rollback risk; crosses all modules.
   - **Effort:** High

2. **Sliced by Layer (Contracts → Backend → Admin-Web → ComposeApp)**
   - **Phase 1 — Contracts & Schema:** Make `Lesson.courseId` nullable in `shared`, add DB migration, update Exposed tables, add `creatorId` to `Lessons` table for standalone auth.
   - **Phase 2 — Backend:** Update services/routes to support standalone lessons, add admin CRUD endpoints.
   - **Phase 3 — Admin-Web:** Build course/lesson/exercise CRUD and assignment UI against the new APIs.
   - **Phase 4 — ComposeApp:** Add lesson list, exercise gameplay templates, and local persistence updates.
   - **Pros:** Reviewable slices; safe rollback per layer; respects module ownership from `AGENTS.md`.
   - **Cons:** Slightly longer timeline; composeApp work is deferred.
   - **Effort:** Medium

3. **Sliced by Entity (Courses → Lessons → Exercises)**
   - Deliver full vertical CRUD for courses first, then lessons (with nullable course support), then exercises.
   - **Pros:** Each slice is a complete user-facing feature.
   - **Cons:** The nullable `courseId` change is cross-cutting and would be split awkwardly across slices.
   - **Effort:** Medium

### Recommendation

**Approach 2 (Layered slicing)** is recommended.

Rationale:
- The nullable `courseId` is a foundational contract/schema change that must be resolved before any UI work can be stable.
- The `admin-web` React app can be developed independently once backend APIs are ready.
- ComposeApp exercise gameplay screens are the most complex and should be built after the admin management workflow is solid, preventing template rework.
- This aligns with the project's `AGENTS.md` guidance: define contracts first, then backend, then client.

### Risks

- **Schema auth ripple:** Making `courseId` nullable breaks the current creator-ownership model (which joins `Lessons → Courses`). Standalone lessons need their own `creatorId` column or an admin-only access rule. This must be designed in the spec phase.
- **Review budget:** Even a single layer (e.g., backend + admin-web) may exceed 400 changed lines. Chained PRs should be planned.
- **No admin-web tests:** `openspec/config.yaml` confirms `admin_web` has 0 tests and no test runner. Admin UI changes will require manual verification.
- **Misalignment on "existing screens":** The user believes exercise-type screens already exist. They do not — only PNG mockups exist in `docs/ui/`. This should be clarified before task planning so effort is not underestimated.

### Ready for Proposal

**Yes**, with the following clarifications for the orchestrator to communicate to the user:

1. The "existing screens for exercise types" are PNG mockups in `docs/ui/`, not implemented Compose code. The admin panel work will create the management UI; gameplay templates are a later phase.
2. Supporting lessons without courses requires a database schema change (nullable `course_id`) and adding a `creator_id` column to `lessons` for authorization. This is non-trivial but feasible.
3. The work should be sliced into layer-based PRs (contracts/backend first, then admin-web, then composeApp) to stay within review budget and reduce risk.
