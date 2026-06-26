# Proposal: Lesson Read Access Control

## Intent

Retroactively formalize the existing lesson read access control behavior already implemented in the backend. The code already enforces visibility scopes, but OpenSpec specs and roadmap do not yet reflect this reality. This slice reconciles documentation with implementation, providing traceability rather than new functionality.

## Scope

### In Scope
- Reconcile `theory-management` spec with existing lesson read access rules.
- Document lesson visibility tiers: official (public), enrolled (private), course owner, admin.
- Capture existing backend integration and service tests as spec verification evidence.

### Out of Scope
- New backend implementation or route changes.
- Frontend behavior changes.
- Write access control (already covered by existing specs).

## Capabilities

### New Capabilities
- `lesson-read-access`: Role and enrollment-based visibility for GET `/lessons/{id}` and GET `/courses/{courseId}/lessons`.

### Modified Capabilities
- `theory-management`: Reconcile the "Inaccessible lesson is blocked" scenario with actual visibility rules (official / enrolled / owner / admin).

## Approach

Audit existing `LessonService` methods (`getLessonByIdForUser`, `getLessonsByCourseIdForUser`) and the integration test `lesson read route enforces visibility scopes`. Extract the four visibility tiers, map them to concrete Gherkin scenarios, and update specs accordingly. No code changes are required.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `openspec/specs/theory-management/spec.md` | Modified | Reconcile scenario with actual rules |
| `openspec/specs/lesson-read-access/spec.md` | New | Document read access tiers |
| `server/src/test/.../ServerIntegrationTest.kt` | Verified | Existing tests serve as evidence |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Spec over-specifies edge cases not covered by tests | Low | Base scenarios strictly on existing integration/service tests |
| Future code drift from documented behavior | Low | Link scenarios directly to existing test names |

## Rollback Plan

Delete the new `lesson-read-access` spec and revert `theory-management` to its previous revision. No deployment or code rollback needed.

## Dependencies

None — implementation already exists.

## Success Criteria

- [ ] `theory-management` spec accurately reflects current backend behavior.
- [ ] New `lesson-read-access` spec exists with scenarios matching integration test `lesson read route enforces visibility scopes`.
- [ ] No functional code changes required to pass verification.
