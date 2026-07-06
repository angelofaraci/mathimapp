# Archive Report: Admin Exercise / Lesson / Course Management

**Change**: admin-exercise-lesson-course-management
**Archived at**: 2026-07-06
**Archive path**: `openspec/changes/archive/2026-07-06-admin-exercise-lesson-course-management/`
**Artifact store mode**: openspec
**Executor**: sdd-archive

## Task Completion Gate

All 24 implementation tasks across all 4 phases are checked `[x]` in the persisted `tasks.md`. Confirmed by reading the tasks artifact: 24/24 tasks complete. `apply-progress.md` and `verify-report.md` corroborate full completion.

No stale unchecked implementation tasks. No exceptional stale-checkbox reconciliation was needed.

## Verification Gate

- **Verdict**: PASS WITH WARNINGS (re-verify)
- **CRITICAL issues**: None — all 3 previously reported CRITICAL blockers are resolved in the re-verify.
- **Warnings**: 4 known items (manual admin-web browser walkthrough not executed, partial cascade assertion, partial auth-coverage gaps, service FK-violation path not explicitly asserted). None block archive per project config (`admin_web` runner null, non-critical partial coverage).

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| admin-course-crud | Created | Full spec copied to `openspec/specs/admin-course-crud/spec.md` (5 requirements, 8 scenarios) |
| admin-exercise-crud | Created | Full spec copied to `openspec/specs/admin-exercise-crud/spec.md` (5 requirements, 8 scenarios) |
| admin-lesson-crud | Created | Full spec copied to `openspec/specs/admin-lesson-crud/spec.md` (4 requirements, 10 scenarios) |
| backend-auth-security | Updated | Merged 2 ADDED requirements: Standalone Lesson Mutation Authorization (4 scenarios) + Exercise Mutation Ownership via Lesson (3 scenarios) |
| client-server-contract | Updated | Merged 2 ADDED requirements: Nullable Lesson Course ID (3 scenarios) + Lesson Creator ID Field (2 scenarios) |
| database-integrity | Updated | Merged 3 ADDED requirements: Nullable Course ID Foreign Key (3 scenarios) + Lesson Creator ID Column (1 scenario) + Cascade Behavior for Unassigned Lessons (2 scenarios) |
| lesson-read-access | Updated | Merged 2 ADDED requirements: Standalone Lesson Read Visibility (3 scenarios) + Standalone Lesson List Access (2 scenarios) |

## Scope Delivered

All in-scope capabilities from the proposal are implemented:

| Capability | Status |
|------------|--------|
| `admin-course-crud` | ✅ Full CRUD with name/description/isOfficial/schoolYear, listing, cascade delete |
| `admin-lesson-crud` | ✅ Full CRUD with nullable courseId, standalone toggle, creator defaulting, course reassignment/unassignment |
| `admin-exercise-crud` | ✅ Full CRUD with lesson assignment, type/content management, reassignment, listing with filters |
| `client-server-contract` | ✅ `Lesson.courseId` nullable, `creatorId` field added, both serializable |
| `lesson-read-access` | ✅ Standalone visibility (admin always, creator can read own, others denied), admin-only listing |
| `backend-auth-security` | ✅ Standalone lesson mutation auth, exercise mutation ownership via lesson, admin route guard |
| `database-integrity` | ✅ Nullable course_id FK, creator_id column with CHECK constraint, no cascading for standalone lessons |

### Out of Scope (confirmed excluded)
- Exercise-type-specific gameplay implementations (MULTIPLE_CHOICE, TRUE_FALSE, INPUT_VALUE)
- ComposeApp lesson/exercise gameplay screens
- Bulk import/export of content
- Admin course listing pagination (explicitly deferred)

## Manual-QA Note

Admin-web CRUD flows (course/lesson/exercise create, edit, delete in browser) were not executed in the CLI environment. `admin-web` builds successfully (`tsc -b && vite build`), and all backend API endpoints are tested. A human browser walkthrough is needed before production release per the proposal's acknowledged risk: "Admin-web has no tests." See `verify-report.md` Warning #1 for details.

## Follow-Up Items (outside this change)

1. Close the open design question: should admin-created course-linked lessons persist `creatorId` for audit consistency? (design.md open question #1)
2. Consider splitting high-assertion-density `AdminIntegrationTest` test methods into narrower scenario-named tests for clearer failure attribution.
3. Non-admin `/admin/lessons` 403 assertion is not explicitly tested (covered by shared `requireAdmin` guard).
4. Add explicit DB FK-violation test for bad `course_id` insertion (currently covered by service-level 400 pre-check).

## Source of Truth Updated

The following main specs now reflect the new behavior:
- `openspec/specs/admin-course-crud/spec.md`
- `openspec/specs/admin-exercise-crud/spec.md`
- `openspec/specs/admin-lesson-crud/spec.md`
- `openspec/specs/backend-auth-security/spec.md`
- `openspec/specs/client-server-contract/spec.md`
- `openspec/specs/database-integrity/spec.md`
- `openspec/specs/lesson-read-access/spec.md`

## Archive Contents

| Artifact | Status |
|----------|--------|
| proposal.md | ✅ |
| specs/ (7 domains) | ✅ |
| design.md | ✅ |
| tasks.md | ✅ (24/24 tasks complete) |
| apply-progress.md | ✅ |
| verify-report.md | ✅ (PASS WITH WARNINGS) |
| archive-report.md | ✅ (this file) |
| exploration.md | ✅ (present) |

## Change Archived

The SDD cycle is complete. The change has been fully planned, proposed, specified, designed, implemented, verified, and archived. Ready for the next change.
