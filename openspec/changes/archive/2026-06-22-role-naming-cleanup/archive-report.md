# Archive Report: role-naming-cleanup

**Change**: role-naming-cleanup
**Archived at**: 2026-06-22
**Mode**: openspec
**Store**: filesystem (`openspec/changes/archive/2026-06-22-role-naming-cleanup/`)

## Task Completion Gate

- [x] All 23 implementation tasks checked `[x]` in `tasks.md`
- [x] No CRITICAL or WARNING issues in `verify-report.md`
- [x] Verdict: PASS — 65 tests passing, 0 failures

## Delta Spec Sync

**backend-auth-security**: Delta already merged during apply (task 6.1/6.2). Main spec at `openspec/specs/backend-auth-security/spec.md` reflects updated role terminology (`STUDENT` throughout) and includes a `Compatibility` section documenting legacy `LEARNER` equivalence. Delta spec directory was empty at archive time — no additional merge action needed.

**lesson-progress-derivation**: No delta spec existed for this domain in this change, but the main spec at `openspec/specs/lesson-progress-derivation/spec.md` was updated during archive-readiness reconciliation to replace remaining learner-role natural language with student terminology for consistency with the archived change scope.

## Archive Contents

| Artifact | Present |
|----------|---------|
| `proposal.md` | ✅ |
| `design.md` | ✅ |
| `specs/` | ✅ (directory present, delta removed per task 6.2) |
| `tasks.md` | ✅ (23/23 tasks complete) |
| `verify-report.md` | ✅ |
| `exploration.md` | ✅ (present from explore phase) |
| `archive-report.md` | ✅ (this file) |

## Verification Summary

- **Tasks**: 23/23 completed
- **Tests**: 65 passing (29 server + 36 compose app), 0 failures, 0 skipped
- **Builds**: `generateSqlDelightInterface` succeeded; `assembleDebug` (not rerun, unchanged since apply)
- **Spec compliance**: 18/18 formal scenarios COMPLIANT; direct Compose app local-cache compatibility coverage is now explicit in tests
- **Issues**: 0 CRITICAL, 0 WARNING, 2 SUGGESTIONS (non-blocking)

Post-review reconciliation added a direct Compose app persistence-boundary regression test proving that a raw local-cache `LEARNER` row decodes as `UserRole.STUDENT` through the production SQLDelight adapter. `./gradlew :composeApp:jvmTest` passed with the added coverage.

## Rollback / Recovery Note

Canonical new writes intentionally emit `STUDENT`. If rollback is needed after any server rows, local cache rows, or JWT claims have already been written with `STUDENT`, a plain revert to pre-change code is unsafe because older code does not understand `STUDENT`.

Concrete recovery path:

1. Deploy a compatibility hotfix (or revert target) that restores parsing/adapter support for both `LEARNER` and `STUDENT`.
2. If a strict legacy rollback is still required, run a migration-aware recovery to rewrite persisted `STUDENT` values back to `LEARNER`, clear or migrate local SQLDelight caches, and invalidate/rotate JWTs minted with `STUDENT` claims.
3. Only then complete the full rollback to the older code line.

Do not stop emitting `STUDENT` in the verified implementation solely for rollback convenience; canonical new writes are part of the accepted behavior.

## Roadmap Update

- `role-naming-cleanup` moved from "Next Slices" → "Completed Slices" in `openspec/roadmap.md`
- "Recommended First Slice" section updated to "Recommended Next Slice" — now points to `versioned-db-migrations` (with `lesson-read-access-control` as alternative)
- Dependency graph updated to show `role-naming-cleanup` as archived
- Remaining Phase 1 slices renumbered: `versioned-db-migrations` (1), `lesson-read-access-control` (2)

## SDD Cycle Complete

The change has been fully planned, designed, implemented, verified, and archived.
Ready for the next change slice.
