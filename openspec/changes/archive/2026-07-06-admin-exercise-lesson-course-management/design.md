# Design: Admin Exercise / Lesson / Course Management

## Technical Approach

Deliver in layers: shared contract + schema, server auth/CRUD, admin-web UI, then composeApp persistence alignment. This follows the proposal and specs by making nullable lesson ownership safe before exposing admin workflows.

## Architecture Decisions

| Decision | Options | Decision |
|---|---|---|
| Lesson ownership | Derive via `Courses.creatorId` only; add `Lessons.creatorId` | Add nullable `Lessons.creatorId`; standalone lessons MUST persist ownership, and admin-created standalone lessons default `creatorId` to the authenticated admin when omitted because standalone lessons cannot join through courses |
| Admin API shape | Reuse public `/courses|lessons|exercises`; add `/admin/*` CRUD | Add `/admin/courses|lessons|exercises` CRUD/list routes so admin-web stays isolated from learner/teacher APIs |
| Frontend state | New global store; page-local React Query | Keep current React Query + route-local form state used by `admin-web/src/pages/Courses.tsx` |
| ComposeApp timing | Implement gameplay now; align contracts now, screens later | Update nullable persistence/contracts only; gameplay/admin authoring remain out of scope |

## Data Flow

Admin SPA ──JWT──> `/admin/*` routes ──> services ──> Exposed/Flyway/Postgres
                         │                 │
                         └── `requireAdmin()` ── ownership fallback for standalone lessons

Teacher/non-admin mutation flow for standalone content remains on existing `/lessons` and `/exercises` routes, but authorization resolves from `Lessons.creatorId` when `courseId` is null.

## File Changes

| File | Action | Description |
|---|---|---|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modify | Make `Lesson.courseId` nullable and add `creatorId` |
| `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` | Modify | Nullable `Lessons.courseId`, add `creatorId` |
| `server/src/main/resources/db/migration/V4__standalone_lessons.sql` | Create | Alter lesson schema and backfill ownership safely |
| `server/src/main/kotlin/com/example/proyectofinal/service/{LessonService,ExerciseService,CourseService}.kt` | Modify | Validation, admin CRUD support, standalone ownership/auth |
| `server/src/main/kotlin/com/example/proyectofinal/routes/{adminRoutes,lessonRoutes,exerciseRoutes}.kt` | Modify | Add admin endpoints and standalone-aware auth paths |
| `server/src/main/kotlin/com/example/proyectofinal/models/{AdminDtos,LessonDto,ExerciseDto,CourseDto}.kt` | Modify | Request/response DTOs for admin CRUD and filters |
| `server/src/test/kotlin/com/example/proyectofinal/{AdminIntegrationTest,ServerIntegrationTest,ServiceLayerTest}.kt` | Modify | Cover admin CRUD, visibility, cascades, standalone ownership |
| `admin-web/src/{App.tsx,lib/api.ts,pages/Courses.tsx}` | Modify | Add navigation, mutations, shared fetch helpers |
| `admin-web/src/pages/{Lessons.tsx,Exercises.tsx}` | Create | CRUD flows with optional course/lesson filters |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modify | Nullable `LessonEntity.courseId`, optional `creatorId` for later sync |

## Interfaces / Contracts

```kotlin
@Serializable
data class Lesson(
    val id: String,
    val courseId: String?,
    val creatorId: String? = null,
    val title: String,
    val theoryContent: String,
    val exercises: List<Exercise> = emptyList()
)
```

Admin DTOs stay server-owned: `CreateAdminCourseRequest`, `UpdateAdminCourseRequest`, `CreateAdminLessonRequest(courseId: String?, creatorId: String?)`, `AdminLessonListResponse`, `CreateAdminExerciseRequest`, plus `courseId`/`lessonId` query filters. Validation rules: 400 for blank required fields or bad foreign keys, 404 for unknown IDs, 403 for non-admin `/admin/*` access, and standalone admin lesson creation auto-fills `creatorId` from the authenticated admin when omitted.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit/service | Nullable ownership resolution, reassignment/unassignment, cascade-safe deletes | Extend `ServiceLayerTest` with standalone lesson/exercise cases |
| Integration | `/admin/*` CRUD, teacher standalone mutation auth, read visibility | Extend Ktor `testApplication` suites with H2-backed route tests |
| Manual | Admin-web forms, filters, optimistic refresh, delete confirmations | `npm run build` plus browser checklist because `admin-web` has no test runner |

## Migration / Rollout

Migration V4: drop/recreate lesson FK as nullable, add `creator_id`, backfill existing rows from `courses.creator_id`, then enforce `CHECK (course_id IS NOT NULL OR creator_id IS NOT NULL)` when supported by target DB. Rollout slices: (1) shared/schema, (2) server services/routes/tests, (3) admin-web, (4) composeApp alignment. Ask for chained PRs if any slice approaches the 400-line budget.

## Open Questions

- [ ] Should admin-created course-linked lessons also persist `creatorId` for audit consistency, or remain nullable until detached?
- [x] Admin course listing pagination is deferred beyond this phase; the admin list returns the full small-list dataset for now.
