# Archive Report

**Change**: subseccion-actividades-catalogo-cursos
**Archived at**: 2026-07-01
**Archive path**: `openspec/changes/archive/2026-07-01-subseccion-actividades-catalogo-cursos/`
**Artifact store mode**: openspec
**Archived by**: sdd-archive sub-agent

## Change Summary

Course catalog screen in the ACTIVITIES tab with search, topic filtering, and rich course cards. Extended the shared `Course` contract with discovery metadata fields (`topic`, `difficulty`, `durationMinutes`, `xpReward`) across backend (Flyway, Exposed, service, seed) and client (SQLDelight, repository, catalog UI).

## Override / Reconciliation Record

### Native Dispatcher False-Negative Override

The native dispatcher reported `verify-report.md is not clearly passing` as a gate warning. This was a **false negative** — the verify report was already updated and clearly states **PASS** with **READY FOR ARCHIVE (content)**. The report contains:

- No CRITICAL issues
- 12/16 COMPLIANT scenarios, 4/16 PARTIAL (all deferred by design)
- 0 FAILING or UNTESTED required scenarios
- Both `:composeApp:jvmTest` and `:server:test` fresh full passes (`BUILD SUCCESSFUL`)

**Reason for override**: The user/orchestrator explicitly approved continuing with archive despite this native false-negative gate, confirming the verify report's actual content is authoritative. This archive report records the override for full audit traceability.

### Stale Checkbox Reconciliation

Not applicable — all 16 tasks were already checked in `tasks.md` with no stale unchecked implementation tasks.

## Task Completion Gate Validation

- `tasks.md`: 16/16 tasks checked `[x]` ✅
- `apply-progress.md`: "16/16 total tasks complete" ✅
- `verify-report.md`: Tasks complete confirmed ✅

## Specs Synced to Main Specs

| Domain | Action | Details |
|--------|--------|---------|
| `client-server-contract` | Updated | Added 1 requirement ("Shared Course Discovery Fields") with 3 scenarios to existing main spec. Existing 6 requirements preserved. |
| `school-year-filtering` | Updated | Added 1 requirement ("Official Courses Include Discovery Metadata") with 2 scenarios to existing main spec. Existing requirement preserved. |
| `course-catalog-discovery` | Already in place | Full spec was created directly in main specs during the change. No delta to merge. |

Total: 2 requirements added across 2 deltas; 0 modified, 0 removed.

## Archive Contents

- `proposal.md` ✅
- `exploration.md` ✅
- `design.md` ✅
- `specs/client-server-contract/spec.md` ✅ (delta)
- `specs/school-year-filtering/spec.md` ✅ (delta)
- `specs/course-catalog-discovery/` ✅ (empty dir — spec was created directly in main specs)
- `tasks.md` ✅ (16/16 tasks complete)
- `apply-progress.md` ✅
- `verify-report.md` ✅
- `archive-report.md` ✅ (this file)

## Source of Truth Updated

The following main specs now reflect the new behavior permanently:

- `openspec/specs/client-server-contract/spec.md` — Added "Shared Course Discovery Fields" requirement
- `openspec/specs/school-year-filtering/spec.md` — Added "Official Courses Include Discovery Metadata" requirement
- `openspec/specs/course-catalog-discovery/spec.md` — Already complete (full spec)

## Verification Status at Archive Time

**Verdict**: PASS — READY FOR ARCHIVE
**Build evidence**: Both `:composeApp:jvmTest` and `:server:test` fresh full passes (`BUILD SUCCESSFUL`) post local-DB remediation

## Risks / Caveats Carried Forward

- W4 (non-blocking): No explicit null-discovery-field assertion test. Source handles nulls; recommended as future enhancement.
- Design open question: Topic chip matching is case-insensitive but accent-sensitive (e.g. "algebra" won't match "Álgebra"). Not a spec violation.

## SDD Cycle Complete

The change has been fully planned, explored, designed, implemented (3 PR slices), verified, and archived. Ready for the next change.
