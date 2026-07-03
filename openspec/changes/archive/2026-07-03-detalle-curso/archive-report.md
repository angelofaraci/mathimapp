# Archive Report: detalle-curso — Course Detail Screen with Enrollment

**Archived**: 2026-07-03
**Source change**: `detalle-curso`
**Persistence mode**: hybrid (openspec filesystem + Engram)
**Verdict**: COMPLETED WITH CARRYOVER WARNINGS

## Task Completion

All 17/17 implementation tasks are complete and checked in the archived `tasks.md`.

| Phase | Tasks | Status |
|-------|-------|--------|
| 1 — Shared Contract & Backend | 1.1–1.6 (6 tasks) | ✅ All complete |
| 2 — App Repository, DI & Session Hydration | 2.1–2.4 (4 tasks) | ✅ All complete (2.2 deferred from slice 2, closed in slice 3) |
| 3 — Activities Router & Detail UI | 3.1–3.7 (7 tasks) | ✅ All complete |

## Verification Summary

| Slice | Result | Details |
|-------|--------|---------|
| Slice 1 (shared + server) | **PASS** | All server tests green; enrollment, exerciseCount, auth, idempotency covered |
| Slice 2 (repo + hydration) | **PASS** | All client repository + hydration tests green; backend composition fix verified |
| Slice 3 (router + UI) | **PASS WITH WARNINGS** | 27 tests green; VM derivation, catalog→detail nav, enroll state, AuthGate routing covered |

## Delta Specs Synced to Main Specs

| Domain | Action | Details |
|--------|--------|---------|
| `course-detail-screen` | Created (new) | Copied delta as full spec — 6 requirements, 16 scenarios |
| `course-enrollment` | Created (new) | Copied delta as full spec — 3 requirements, 11 scenarios |
| `lesson-display` | Created (new) | Copied delta as full spec — 3 requirements, 8 scenarios |
| `session-hydration` | Created (new) | Copied delta as full spec — 3 requirements, 7 scenarios |
| `course-catalog-discovery` | Merged (delta) | Replaced "Visual-Only Enrollment Button" with "Functional Enrollment Button" — 3 scenarios |

## Archive Contents

- `proposal.md` ✅ — Change intent, scope, approach
- `design.md` ✅ — Technical architecture, data flow, file changes
- `specs/` ✅ — 5 delta spec files (course-detail-screen, course-enrollment, lesson-display, session-hydration, course-catalog-discovery delta)
- `tasks.md` ✅ — 17/17 tasks complete
- `verify/` ✅ — Slice 1, 2, 3 and aggregate verify reports
- `exploration.md` ✅ — Exploration artifacts
- `archive-report.md` ✅ — This file

## Carryover Warnings & Follow-ups (Non-Blocking)

The following items were identified during verification as non-critical gaps. They do not block archive but should be visible for future work:

### Warnings (from slice-3 verify report)

| Warning | Description |
|---------|-------------|
| **W1** — Compose rendering test coverage gap | Header chips, progress-bar text/visibility, checkmark/arrow glyphs, "ejercicios" text, lesson-list ordering, inert-tap, loading indicator are covered by static source inspection only. No Compose UI test harness in `commonTest`. |
| **W2** — Non-enrolled progress bar branch | `isEnrolled=false` progress bar hide branch is not directly unit-tested; only structural via `if (isEnrolled)`. |
| **W3** — Lesson ordering not runtime-asserted | Lesson ordering relies on server `orderIndex` (design note); no VM test asserts the rendered sequence. |

### Suggestions (carried across slices)

| Suggestion | Source | Description |
|------------|--------|-------------|
| **S1** — Compose UI smoke test seam | slice-3 W1 | Introduce a minimal Compose UI test for `CourseDetailContent` with header strings, "X/Y lecciones" text, checkmark/arrow glyphs. Closes W1/W2. |
| **S2** — `MockCourseRepository` enrollment contract | slice-2 S1 | Add a small unit test asserting `MockCourseRepository.enroll()` adds to `enrolledCourseIds` and respects `isOfficial`. |
| **S3** — Lesson `exerciseCount` deserialization default test | slice-1 S1 / slice-2 S2 | One-line `commonTest` deserializing a `Lesson` JSON without `exerciseCount` to confirm backward-compatible default of `0`. |
| **S4** — Exception catch ordering | slice-2 S3 / slice-3 S2 | Maintain `UnauthorizedSessionException` before generic `Exception` in `hydrateSessionIfNeeded` — reordering silently changes invalid-token semantics. |
| **S5** — `localEnrolledCourseIds` refresh optimization | slice-3 S4 | `CourseDetailScreen` `LaunchedEffect` re-fetches progress when `courseId` enters `localEnrolledCourseIds`. Acceptable (idempotent), but a future optimization could pass the set directly into UI state and avoid extra network round-trips. |

### Engram Observation IDs (Traceability)

| Artifact | Observation ID |
|----------|---------------|
| `sdd/detalle-curso/proposal` | #657 |
| `sdd/detalle-curso/spec` | #658 |
| `sdd/detalle-curso/design` | #659 |
| `sdd/detalle-curso/tasks` | #663 |
| `sdd/detalle-curso/apply-progress` | #669 |
| `sdd/detalle-curso/archive-report` | (this observation) |

**Note**: The `sdd/detalle-curso/verify-report` was not found as a single Engram observation — verification artifacts are stored as filesystem files in `verify/verify-slice-1.md`, `verify-slice-2.md`, `verify-slice-3.md`, and `verify-report.md`.

## Source of Truth Updated

The following main specs now reflect the new behavior from this change:

- `openspec/specs/course-detail-screen/spec.md`
- `openspec/specs/course-enrollment/spec.md`
- `openspec/specs/lesson-display/spec.md`
- `openspec/specs/session-hydration/spec.md`
- `openspec/specs/course-catalog-discovery/spec.md` (delta merged)

## SDD Cycle Complete

The `detalle-curso` change has been fully planned, proposed, specified, designed, implemented (3 stacked slices), verified (3 slices, all passing), and archived. 17/17 tasks complete. All delta specs synced to source-of-truth main specs. Non-blocking carryover warnings are documented above for visibility.
