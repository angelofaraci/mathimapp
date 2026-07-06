# Proposal: Admin Exercise / Lesson / Course Management

## Intent

The admin panel currently lists courses read-only and has no lesson or exercise management. Teachers and admins cannot create content without direct database access. This change adds full CRUD for courses, lessons, and exercises in the admin-web panel, while allowing lessons to exist standalone (outside any course) with their own creator.

## Scope

### In Scope
- Make `Lesson.courseId` nullable and add `creatorId` to standalone lessons.
- Database migration for nullable `course_id` and lesson ownership.
- Admin backend CRUD endpoints for courses, lessons, exercises.
- Admin-web pages: course editor, lesson manager (with optional course assignment), exercise manager (with lesson assignment).
- Update auth services to resolve standalone lesson ownership.

### Out of Scope
- Exercise-type-specific gameplay implementations (MULTIPLE_CHOICE, TRUE_FALSE, INPUT_VALUE).
- ComposeApp lesson/exercise gameplay screens.
- Bulk import/export of content.

## Capabilities

### New Capabilities
- `admin-course-crud`: Admin-only create, update, delete courses.
- `admin-lesson-crud`: Admin-only create, update, delete lessons with optional course link.
- `admin-exercise-crud`: Admin-only create, update, delete exercises and assign to lessons.

### Modified Capabilities
- `client-server-contract`: `Lesson.courseId` becomes nullable.
- `lesson-read-access`: Add visibility rules for standalone lessons (admin/creator access).
- `backend-auth-security`: Lesson/exercise mutation auth supports standalone `creatorId`.
- `database-integrity`: Cascade rules updated for nullable `course_id`.

## Approach

Layer-based delivery: contracts and schema first, then backend services and admin routes, then admin-web UI, then ComposeApp persistence. This respects module ownership and keeps each review slice under 400 lines.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/models/Models.kt` | Modified | `Lesson.courseId` nullable. |
| `server/database/Tables.kt` | Modified | `Lessons.courseId` nullable; add `creatorId`. |
| `server/db/migration/` | New | Migration for schema changes. |
| `server/service/LessonService.kt` | Modified | Standalone ownership resolution. |
| `server/routes/adminRoutes.kt` | Modified | New CRUD endpoints. |
| `admin-web/src/pages/` | New | CourseEditor, LessonManager, ExerciseManager. |
| `composeApp/sqldelight/...AppDatabase.sq` | Modified | `LessonEntity.courseId` nullable. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Nullable `courseId` breaks existing auth joins | Med | Add `creatorId` column and fallback logic in services. |
| Review budget exceeded | Med | Slice by layer; chain PRs if any slice exceeds 400 lines. |
| Admin-web has no tests | High | Manual verification checklist per PR. |

## Rollback Plan

If critical failure occurs after deployment: stop admin-web releases, revert backend to previous Docker image (or run compensating migration to restore non-null `course_id` for rows that have it), and roll back admin-web build. Data created during failure window can be cleaned via admin endpoints or manual DB script.

## Dependencies

- None external. Relies on existing JWT auth and PostgreSQL.

## Success Criteria

- [ ] Admin can create, edit, and delete courses.
- [ ] Admin can create, edit, and delete lessons with or without a course.
- [ ] Admin can create, edit, and delete exercises and link them to lessons.
- [ ] Standalone lessons are editable by their creator and admin.
- [ ] Existing course-linked lessons remain fully functional.
