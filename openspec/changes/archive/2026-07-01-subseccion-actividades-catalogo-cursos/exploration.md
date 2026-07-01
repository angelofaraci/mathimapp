## Exploration: Course Catalog in Activities Tab (Catálogo de Cursos)

### Current State
- The app scaffold has four bottom-nav tabs: **Inicio**, **Actividades**, **Progreso**, **Perfil**.
- `MainTab.ACTIVITIES` renders `PlaceholderScreen(title = "Actividades")`.
- `MainTab.HOME` renders `CourseScreen`, a simple `LazyColumn` of official courses filtered by `schoolYear`.
- The shared `Course` model contains: `id`, `title`, `description`, `creatorId`, `isOfficial`, `joinCode`, `lessons`, `schoolYear`.
- The backend exposes `GET /courses/official` and `POST /courses/join` (code-based only).
- The reference image (`docs/ui/screens/catalogo-de-cursos.png`) shows a full catalog with: search bar, horizontal topic chips (e.g., Fracciones, Álgebra, Geometría), course cards showing difficulty level, topic badge, lesson count, estimated duration, XP reward, and a one-tap **"Inscribirse"** button.
- None of the image-specific fields (duration, XP, difficulty, topic) exist in the current data model or database schema.

### Affected Areas
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` — swap `PlaceholderScreen` for the real catalog screen.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/App.kt` and new files — `CourseCatalogScreen`, `CourseCatalogViewModel`, search/filter state, card composables.
- `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` — extend `Course` (or add a catalog DTO) with `durationMinutes`, `xpReward`, `difficulty`, `topic`.
- `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` — add columns to `Courses`.
- `server/src/main/kotlin/com/example/proyectofinal/service/CourseService.kt` and routes — support new fields, search/filter, and direct enrollment.
- `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` — update `CourseEntity` and queries if caching the new fields locally.
- Flyway migration script (server) — required for new `Courses` columns.

### Approaches

1. **UI-Only (reuse existing data)**
   - Build the catalog layout using existing `Course` fields. Hide or mock unsupported fields (duration, XP, level, topic). Keep the existing join-code enrollment flow.
   - **Pros:** Fastest, zero backend changes, no migration.
   - **Cons:** Degrades UX; does not match the reference image; enrollment still requires a teacher-generated code, which contradicts the one-tap "Inscribirse" card action.
   - **Effort:** Low

2. **UI + Backend Model Expansion (recommended)**
   - Add new display/discovery fields to the shared `Course` model. Update the Exposed table, SQLDelight schema, and Flyway migration. Add a direct-enrollment endpoint. Build the full UI with search, chips, and cards.
   - **Pros:** Matches the reference image end-to-end; supports real self-service enrollment; the new fields are reusable by roadmap slices `gamification-rewards` and `learning-paths`.
   - **Cons:** Touches `shared`, `server`, `composeApp`, and the database; requires a migration; review will likely exceed 400 lines.
   - **Effort:** Medium-High

3. **UI + Lightweight Catalog DTO**
   - Create a separate `CourseCatalogItem` in `shared` with read-only display fields, leaving the core `Course` unchanged. Backend returns a catalog-specific projection.
   - **Pros:** Does not bloat the existing `Course` contract used in other flows.
   - **Cons:** Introduces a second course-like DTO, increases API surface, and may confuse future maintainers. The fields are still needed for upcoming slices, so the core model will likely need them later anyway.
   - **Effort:** Medium

### Recommendation
Adopt **Approach 2 (UI + Backend Model Expansion)**. The `Course` model is the canonical shared contract for official courses; extending it with discovery fields is simpler than maintaining a parallel DTO, and the fields are needed for upcoming roadmap slices.

Because the diff will likely exceed **400 lines**, plan **chained PRs**:
- **PR 1 (backend + shared):** `Course` model additions, Exposed/SQLDelight schema updates, Flyway migration, new direct-enrollment endpoint, updated service tests.
- **PR 2 (app):** `CourseCatalogScreen`, `CourseCatalogViewModel`, search/filter state, card composables, `AuthenticatedHomeScaffold` wiring, UI tests.

### Risks
- **Enrollment mechanism mismatch:** Current `joinCourse` is code-based. The image implies one-tap enrollment. A new `POST /courses/{id}/enroll` endpoint must be designed without breaking the existing classroom join-code flow.
- **Data model ripple:** Adding fields to `Course` in `shared` forces recompilation of both `composeApp` and `server`. All mock fixtures and seed data must be updated.
- **Tab ownership confusion:** `HOME` already shows a course list. Product must clarify whether `HOME` is repurposed, removed, or kept distinct (e.g., HOME = enrolled courses, ACTIVITIES = discover/catalog).
- **Topic/Category taxonomy:** The filter chips need a stable enum or tag list. This should be defined contract-first in `shared` (e.g., `enum class CourseTopic { FRACTIONS, ALGEBRA, GEOMETRY, ... }`).
- **Review size:** Without chaining, the change could become unreviewable. Enforce the 400-line chained-PR rule from the project convention.

### Ready for Proposal
Yes. The scope is clear enough to proceed to `sdd-propose`.

The orchestrator should ask the user to confirm:
1. **Final change name** — Recommend `activities-course-catalog` to match the existing English kebab-case convention (`exercise-practice-ui`, `learning-paths`, etc.). `subseccion-actividades-catalogo-cursos` is acceptable if the user strongly prefers Spanish, but it breaks naming consistency.
2. **HOME vs. ACTIVITIES** — Should the existing `HOME` course list be removed, repurposed, or kept alongside the new catalog?
3. **Enrollment semantics** — Does "Inscribirse" mean direct enrollment (no code), or should the card display a code entry dialog?
4. **Topic enum values** — What is the v1 list of topics/categories (e.g., Fracciones, Álgebra, Geometría, Aritmética, etc.)?
