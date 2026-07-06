## Implementation Progress

**Change**: admin-exercise-lesson-course-management
**Mode**: Standard

### Completed Tasks
- [x] 1.1 `shared/.../Models.kt` — made `Lesson.courseId` nullable and added `creatorId`
- [x] 1.2 `server/.../Tables.kt` — made `Lessons.courseId` nullable and added `creatorId`
- [x] 1.3 `server/.../V4__standalone_lessons.sql` — added the standalone-lesson migration with backfill and ownership check constraint
- [x] 1.4 `server/.../LessonDto.kt` — made `CreateLessonRequest.courseId` nullable and added `creatorId`
- [x] 1.5 `server/.../AdminDtos.kt` — added foundational admin CRUD DTOs for course, lesson, and exercise work
- [x] 1.6 `composeApp/.../AppDatabase.sq` — aligned local persistence with nullable `courseId` and optional `creatorId`
- [x] 2.1 `LessonService.kt` — resolved standalone lesson ownership/read access by falling back to `creatorId` when `courseId` is null
- [x] 2.2 `LessonService.kt` — added admin lesson list helpers, standalone filtering, admin patch/update semantics, and admin-safe delete usage
- [x] 2.3 `ExerciseService.kt` — resolved standalone lesson ownership for exercise mutation authorization and admin CRUD validation
- [x] 2.4 `CourseService.kt` — added admin create/update/delete course operations
- [x] 2.5 `ContentReadAccess.kt` — enforced standalone lesson visibility for admin or creator only, with admin answer redaction
- [x] 2.6 `ServiceMappers.kt` — preserved the existing `creatorId` lesson mapping and exercised it through the new backend flows/tests
- [x] 3.1 `adminRoutes.kt` — added POST/PUT/DELETE `/admin/courses`
- [x] 3.2 `adminRoutes.kt` — added GET/POST/PUT/DELETE `/admin/lessons` with `?courseId=` filter semantics
- [x] 3.3 `adminRoutes.kt` — added GET/POST/PUT/DELETE `/admin/exercises` with `?lessonId=` filter semantics
- [x] 3.4 `lessonRoutes.kt` — updated existing lesson routes for standalone `creatorId` auth resolution
- [x] 3.5 `exerciseRoutes.kt` — updated exercise auth to resolve standalone lesson ownership
- [x] 3.6 `AdminIntegrationTest.kt` — added admin CRUD and standalone-route integration coverage, including 400/404/403 paths
- [x] 3.7 `ServiceLayerTest.kt` — added standalone ownership, theory-access, patch-semantics, and cascade coverage
- [x] 4.1 `admin-web/src/App.tsx` — added Lessons and Exercises navigation/routes behind the existing admin auth guard
- [x] 4.2 `admin-web/src/pages/Courses.tsx` — added create/edit/delete forms with React Query mutations and refetch-based refresh
- [x] 4.3 `admin-web/src/pages/Lessons.tsx` — added lesson list/filter CRUD with standalone toggle and course assignment flow
- [x] 4.4 `admin-web/src/pages/Exercises.tsx` — added exercise list/filter CRUD with lesson assignment and exercise type selector
- [x] 4.5 Manual verification support — built `admin-web` successfully and documented the remaining human browser walkthrough checklist
- [x] Verify blocker remediation — added runtime assertions for the 6 previously untested admin backend scenarios in `AdminIntegrationTest.kt`
- [x] Verify blocker remediation — aligned the standalone creator default and no-pagination phase decision across the affected OpenSpec specs/design

### Files Changed
| File | Action | What Was Done |
|------|--------|---------------|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modified | Updated the shared `Lesson` contract to allow standalone lessons and carry optional ownership metadata. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` | Modified | Made `Lessons.courseId` nullable and added nullable `creatorId` in the Exposed schema. |
| `server/src/main/resources/db/migration/V4__standalone_lessons.sql` | Created | Added the Flyway migration to backfill `creator_id`, relax the lesson/course FK, and enforce ownership presence. |
| `server/src/main/kotlin/com/example/proyectofinal/models/LessonDto.kt` | Modified | Allowed nullable `courseId` and added optional `creatorId` on lesson creation requests. |
| `server/src/main/kotlin/com/example/proyectofinal/models/AdminDtos.kt` | Modified | Added admin create/update/list DTOs needed for the next backend/admin slices without wiring routes yet. |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modified | Updated local lesson storage to persist nullable `courseId` plus optional `creatorId`. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ServiceMappers.kt` | Modified | Mapped nullable `courseId` and optional `creatorId` into the shared `Lesson` contract. |
| `server/src/main/kotlin/com/example/proyectofinal/service/UserService.kt` | Modified | Added a defensive non-null requirement for the current course-linked exercise completion flow. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorLessonRepository.kt` | Modified | Stored the new optional `creatorId` alongside nullable `courseId` in the SQLDelight cache. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorLessonRepositoryTest.kt` | Modified | Updated lesson cache inserts for the expanded SQLDelight insert signature. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorExerciseRepositoryTest.kt` | Modified | Updated lesson fixture inserts for the new nullable/ownership schema. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorUserRepositoryTest.kt` | Modified | Updated lesson fixture inserts for the new nullable/ownership schema. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ContentReadAccess.kt` | Modified | Added standalone lesson visibility rules and answer-redaction behavior for admin reads. |
| `server/src/main/kotlin/com/example/proyectofinal/service/LessonService.kt` | Modified | Added standalone-aware lesson access resolution, admin lesson CRUD helpers, audit-preserving patch semantics, and standalone theory updates. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ExerciseService.kt` | Modified | Added standalone ownership fallback for exercise auth plus admin list/create/update/delete helpers with foreign-key validation. |
| `server/src/main/kotlin/com/example/proyectofinal/service/CourseService.kt` | Modified | Added admin course create/update/delete operations while keeping creator ownership bound to the authenticated admin. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/adminRoutes.kt` | Modified | Added admin backend CRUD routes for courses, lessons, and exercises, including lesson patch parsing and filter handling. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/lessonRoutes.kt` | Modified | Enabled standalone lesson creation for admin/teacher flows and ensured audit `creatorId` persistence on public lesson creation. |
| `server/src/main/kotlin/com/example/proyectofinal/Main.kt` | Modified | Wired lesson and exercise services into the expanded admin route module. |
| `server/src/test/kotlin/com/example/proyectofinal/AdminIntegrationTest.kt` | Modified | Added admin CRUD integration tests plus public standalone lesson/exercise authorization coverage. |
| `server/src/test/kotlin/com/example/proyectofinal/ServiceLayerTest.kt` | Modified | Added standalone visibility, patch semantics, ownership fallback, and cascade-preservation tests. |
| `openspec/changes/admin-exercise-lesson-course-management/tasks.md` | Modified | Recorded the feature-branch-chain plan and marked both the completed foundational PR 1 tasks and the backend PR 2 tasks in the OpenSpec checklist. |
| `openspec/changes/admin-exercise-lesson-course-management/apply-progress.md` | Modified | Extended the cumulative apply-progress record with the completed backend slice and verification results. |
| `admin-web/src/App.tsx` | Modified | Added Lessons and Exercises navigation links plus protected routes for the new admin CRUD pages. |
| `admin-web/src/pages/Courses.tsx` | Modified | Added course create/edit/delete forms, mutation feedback banners, and destructive action handling. |
| `admin-web/src/pages/Lessons.tsx` | Created | Added lesson list/filter CRUD with standalone toggling, course assignment, and mutation-based refresh. |
| `admin-web/src/pages/Exercises.tsx` | Created | Added exercise list/filter CRUD with lesson assignment, type selection, and newline-based option editing. |
| `admin-web/src/lib/api.ts` | Modified | Added a shared unknown-error formatter used by the new admin-web mutation flows. |
| `admin-web/src/App.css` | Modified | Added shared panel/form/button styles for the admin content-management screens. |
| `openspec/changes/admin-exercise-lesson-course-management/tasks.md` | Modified | Marked the admin-web PR 3 tasks complete after the UI slice built successfully. |
| `openspec/changes/admin-exercise-lesson-course-management/apply-progress.md` | Modified | Extended the cumulative progress record with the admin-web slice, build verification, and manual-check notes. |
| `server/src/test/kotlin/com/example/proyectofinal/AdminIntegrationTest.kt` | Modified | Added runtime assertions for the 6 verify-blocked admin backend scenarios: missing course update, missing lesson/course validation, missing exercise required data, missing exercise reassignment target, and unfiltered admin exercise listing. |
| `openspec/changes/admin-exercise-lesson-course-management/specs/admin-lesson-crud/spec.md` | Modified | Reconciled standalone admin lesson creation with the implemented safer behavior that defaults omitted `creatorId` to the authenticated admin. |
| `openspec/changes/admin-exercise-lesson-course-management/specs/admin-course-crud/spec.md` | Modified | Reconciled the requirement wording with the established decision that course listing is unpaginated in this phase. |
| `openspec/changes/admin-exercise-lesson-course-management/design.md` | Modified | Updated the design decisions/open questions to match the persisted standalone creator default and deferred pagination decision. |
| `openspec/changes/admin-exercise-lesson-course-management/apply-progress.md` | Modified | Recorded the verification-blocker remediation batch, aligned artifacts, and fresh verification evidence. |

### Verification
| Command | Result |
|---------|--------|
| `./gradlew :server:test` | Passed (`BUILD SUCCESSFUL`). |
| `./gradlew :composeApp:assembleDebug` | Passed (`BUILD SUCCESSFUL`). |
| `npm run build` (in `admin-web/`) | Passed (`tsc -b && vite build`). |

### Deviations from Design
None — implementation matches the design after the verify-blocker spec/design alignment.

### Issues Found
- `ServiceMappers.kt` already had the `creatorId` lesson mapping from PR 1, so task 2.6 completed through validation and regression coverage rather than a new source diff in this slice.
- The admin course create/update endpoints respond with the shared `Course` model instead of the richer admin list DTO, so the UI uses invalidate/refetch after mutations instead of trying to patch the table cache locally.
- Browser-only checks still need a human pass: create/edit/delete for courses, linked-vs-standalone lesson reassignment, and exercise lesson reassignment/type changes.

### Remaining Tasks
- [x] None in this apply slice.

### Workload / PR Boundary
- Mode: remediation batch
- Current work unit: verify blocker closure
- Boundary: starts at the 6 missing backend scenario assertions plus spec/design wording reconciliation, and ends after rerunning `:server:test` and `:composeApp:assembleDebug`.
- Estimated review budget impact: Low; targeted backend-test and OpenSpec alignment changes only.

### Status
24/24 tasks complete. Verify blockers remediated and ready for re-verify, with browser walkthrough notes still captured for a human follow-up pass.
