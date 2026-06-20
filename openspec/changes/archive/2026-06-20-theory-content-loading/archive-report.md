# Archive Report: theory-content-loading

**Change**: theory-content-loading
**Archived**: 2026-06-20
**Store**: openspec
**Verdict**: PASS WITH WARNINGS — intentional partial archive with tracked caveats
**Archive type**: intentional-with-warnings (user explicitly approved: "lo que haya que hacer a futuro anotalo, segui con el archive")

## Task Completion Gate

- **Tasks**: 20/20 complete — all implementation tasks checked `[x]`
- **Gate**: PASS — no stale unchecked implementation tasks

## Verification Report Assessment

- **Verdict**: PASS WITH WARNINGS
- **CRITICAL issues**: None
- **Warnings** (carried forward, not blocking):
  1. `theory-management` read-access scenario "Inaccessible lesson is blocked" remains unmet — `GET /lessons/{id}` performs no read-access/enrollment check. Explicitly scoped out of this change by design.
  2. Database migration for `courses.school_year` not provided — persistent environments need explicit migration, not just `SchemaUtils.create`.
  3. Minor doc drift: proposal/design reference `LessonDto.kt` while actual change affected `CourseDto.kt`. Documentation-only, no code defect.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| `backend-auth-security` | Updated (delta merged) | Added 1 requirement: "Theory Mutation Authorization" with 3 scenarios |
| `client-server-contract` | Updated (delta merged) | Added 2 requirements: "Shared Course School Year Field" (2 scenarios), "Shared Theory Update Request" (2 scenarios) |
| `school-year-filtering` | No change needed | Already correct in main specs |
| `theory-management` | No change needed | Update-scope requirement already in main spec; read-access "Inaccessible lesson is blocked" scenario preserved as-is (unmet) — NOT marked satisfied |

### Merge Details

**backend-auth-security**: Delta had `## ADDED Requirements` → appended "Theory Mutation Authorization" requirement to main spec. Preserved all 5 pre-existing requirements unchanged.

**client-server-contract**: Delta had `## ADDED Requirements` → appended "Shared Course School Year Field" and "Shared Theory Update Request" requirements to main spec. Preserved all 3 pre-existing requirements unchanged.

**theory-management**: The read-access "Inaccessible lesson is blocked" scenario (warning #1) remains in main spec as unmet. NOT marked satisfied during archive. The update-scope requirement scenarios are satisfied and were already present in main spec — no merge needed.

## Archive Contents

- `proposal.md` — present
- `specs/backend-auth-security/spec.md` — present (delta)
- `specs/client-server-contract/spec.md` — present (delta)
- `design.md` — present
- `tasks.md` — present (20/20 complete)
- `apply-progress.md` — present
- `verify-report.md` — present
- `archive-report.md` — present (this file)
- `exploration.md` — present

## Source of Truth Updated

The following main specs now permanently reflect the new behavior:
- `openspec/specs/backend-auth-security/spec.md`
- `openspec/specs/client-server-contract/spec.md`

## Remaining Follow-ups

These items were intentionally NOT resolved during this change and MUST be tracked as future SDD work:

1. **theory-management read-access enforcement**: `GET /lessons/{id}` needs ownership/enrollment/role gating so "Inaccessible lesson is blocked" scenario is satisfied. Requires a new SDD change (e.g., `lesson-read-access`).
2. **Database migration for `courses.school_year`**: Persistent deployments need an explicit Flyway/Liquibase/Exposed migration script. Currently relies on `SchemaUtils.create` which only works for fresh databases.
3. **Minor doc drift correction**: Proposal and design file tables reference `server/.../models/LessonDto.kt` but the actual server DTO change was `CourseDto.kt`. `TheoryUpdateRequest` correctly lives in `shared`. Documentation-only; no code impact.
4. **Optional Android validation**: `:composeApp:androidUnitTest` not run (no Android SDK in CI). JVM SQLDelight path validates `schoolYear` adapter already. Verify on Android SDK environment if available.
5. **Explicit `theoryContent` assertion**: `ServerIntegrationTest.learner content hides correct answers` asserts exercise masking but not the `theoryContent` value. Adding `assertEquals("Theory", lesson.theoryContent)` would tighten coverage.

## Risks

- **None for archived change**: All delta spec scenarios are satisfied with passing test coverage (48 tests, 0 failures).
- **For follow-up**: The theory-management read-access gap means any authenticated user can read any lesson's theory. Mitigation requires a dedicated SDD change.

## SDD Cycle Complete

The `theory-content-loading` change has been fully planned, implemented, verified, and archived. All satisfied delta specs are merged into the source-of-truth main specs. Unresolved items are tracked as explicit follow-ups above.
