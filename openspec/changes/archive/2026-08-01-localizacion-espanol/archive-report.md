# Archive Report: localizacion-espanol

**Change**: `localizacion-espanol`
**Archived**: 2026-08-01
**Artifact Store**: hybrid (OpenSpec files + Engram observations)

## Engram Observation IDs Used (Traceability)

| Artifact | Engram Topic Key | Observation ID |
|----------|-----------------|----------------|
| Proposal | `sdd/localizacion-espanol/proposal` | #1215 |
| Spec | `sdd/localizacion-espanol/spec` | #1216 |
| Design | `sdd/localizacion-espanol/design` | #1217 |
| Tasks | `sdd/localizacion-espanol/tasks` | #1218 |
| Apply Progress | `sdd/localizacion-espanol/apply-progress` | #1219 |
| Verify Report | `sdd/localizacion-espanol/verify-report` | #1220 |

## Implementation Commit

| Commit Hash | Message |
|-------------|---------|
| `fdfa354` | feat(compose): redesign core learning screens |

## Task Completion Gate

All 5 phases (A-D + Backlog Sync) with 23 implementation tasks total are marked `[x]` in archived `tasks.md`:
- Phase 1 (Group A): 10/10 tasks complete
- Phase 2 (Group B): 8/8 tasks complete
- Phase 3 (Group C): 6/6 tasks complete
- Phase 4 (Group D): 14/14 complete (noting 4.5, 4.6 are N/A for render call sites that never existed)
- Phase 5 (Backlog Sync): 2/2 tasks complete

Gate passed — no stale-checkbox reconciliation needed.

## Verification Gate

Verify report (#1220) shows **PASS** (after voseo fix re-verify):
- **CRITICAL**: voseo violation was identified and fixed. All 7 voseo strings in `values/strings.xml` were corrected to use "tú" forms. Whole-file sweep confirms zero voseo instances anywhere in the file.
- **WARNING**: 2 non-blocking process notes (PR-staging for HomeDashboardViewModel entanglement with onboarding-profile-scoped-to-user, and pre-existing `%` escape in Groups B/C). Neither blocks archive.
- **Verdict**: PASS — archive proceeds.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| ui-localization | Created (new capability) | Copied delta spec as full spec to `openspec/specs/ui-localization/spec.md` — 5 requirements, 13 scenarios |
| onboarding-flow | Modified (existing capability) | ADDED Requirement "Onboarding Copy Renders in Spanish" (3 scenarios) to existing main spec |

## Archive Contents

- `proposal.md` ✅
- `specs/ui-localization/spec.md` ✅ (new capability)
- `specs/onboarding-flow/spec.md` ✅ (delta/ADDED)
- `design.md` ✅
- `tasks.md` ✅ (23/23 tasks complete)
- `archive-report.md` ✅ (this file)

## Source of Truth Updated

- `openspec/specs/ui-localization/spec.md` — new canonical spec for composable string resource indirection and Spanish localization behavior
- `openspec/specs/onboarding-flow/spec.md` — merged with new requirement for Spanish-language rendering

## Backlog Updated

Per Phase 5, Task 5.1:
- "App is not localized to Spanish" (item #1 of "Onboarding and navigation bug fixes" in `openspec/backlog.md`) has been marked as resolved, referencing `localizacion-espanol`.

Per Phase 5, Task 5.2:
- New backlog entry added: "Localize ViewModel error messages via sealed error types" — covers `errorMessage: String` fields, raw `Throwable.message` propagation, and the already-Spanish-but-hardcoded `salutation()`/`greetingFor()` in `HomeDashboardViewModel.kt`.

## Verification

- [x] Main spec created correctly at `openspec/specs/ui-localization/spec.md`
- [x] Existing spec merged correctly at `openspec/specs/onboarding-flow/spec.md`
- [x] Change folder moved to `openspec/changes/archive/2026-08-01-localizacion-espanol/`
- [x] Archive contains all artifacts (proposal, specs, design, tasks)
- [x] Archived `tasks.md` has 0 unchecked implementation tasks (23/23 complete)
- [x] Active changes directory no longer has `localizacion-espanol`

## Notable Decisions

- **New capability**: `ui-localization` is a new cross-cutting capability spanning all UI screens, distinct from domain models. The delta spec became a full spec (no merge needed) because no prior `openspec/specs/ui-localization/spec.md` existed.
- **Voseo fix**: A CRITICAL voseo content-quality issue was identified during verification (obs #1220), fixed in apply-progress (obs #1219), and re-verified. The change was still fully approved because: (a) all 7 affected strings were corrected; (b) whole-file sweep confirmed no other voseo instances; (c) build tests passed; (d) the fix itself is low-risk (XML string content only, no Kotlin changes).
- **State-shape refactor**: The `schoolYearLabel` string-interpolation in ViewModels was converted to structured fields (`schoolYear: Int?`, `studentTrack: StudentTrack?`) to enable proper localization. This was not explicitly enumerated in the proposal but was discovered as a scope refinement during design (not an `errorMessage` exclusion, so it fell under the localization scope). Tests were updated per TDD in Group D.
- **Onboarding copy requirement**: The delta spec for `onboarding-flow` introduced one new ADDED requirement (not a MODIFIED one) because the existing main spec had no prior requirement about copy language/localization — this is genuinely new behavior, not a change to existing specs.

## Implementation Details

### Code Changes Summary
Across 4 chained PRs (A-D):
- 2 new XML files: `composeResources/values/strings.xml` (Spanish, ~110 keys) and `composeResources/values-en/strings.xml` (English fallback, same key set)
- 1 new Kotlin file: `ui/StudentTrackLabels.kt` (UI-layer extension)
- ~12 modified UI screen files (OnboardingScreen, LessonMapScreen, TheorySheet, PlaceholderScreen, LessonMapNode, RegisterScreen, HomeDashboardScreen, LoginScreen, ProfileScreen, AuthScreenScaffold, AuthenticatedHomeScaffold)
- 2 modified ViewModel files (ProfileViewModel, HomeDashboardViewModel) for state-shape refactor
- 5 modified test files (SqlDelightLearnerProfileRepositoryTest, ProfileViewModelTest, HomeDashboardViewModelTest, and render tests)
- 1 modified backlog file (`openspec/backlog.md`)

Total: ~670 changed lines across 4 PRs, matching the high-risk forecast.

### Key Architecture Decisions
1. **Spanish default**: `values/strings.xml` is the unqualified folder (not `values-es/`), so unmatched device locales resolve to Spanish (the Argentina-targeted audience default).
2. **`StudentTrack.displayName` untouched**: Persistence contract protected. UI-layer `localizedLabel()` extension added instead.
3. **Numbered positional placeholders**: `%1$s`, `%1$d` used consistently for translation reorderability.
4. **ViewModel error text excluded**: `errorMessage: String` fields and raw `Throwable.message` remain unlocalized (deferred to backlog item).

## Intentional Archive Notes

- **Standard clean archive**: No overrides, partial-archive approvals, or stale-checkbox reconciliations were needed. All tasks are complete, and the voseo CRITICAL issue was fixed and re-verified before archive.
- **Hybrid persistence**: All OpenSpec files were written to the filesystem AND Engram observations were captured for future reference and traceability.
- **Backlog alignment**: The backlog was explicitly updated per design and tasks, with resolved item #1 marked and the new error-message localization item added.

## What Shipped

Users (Argentina primary/secondary) now see:
- **Spanish-language onboarding flow** with 4 complete steps (province, school year, category, confirmation), all in neutral Latin American Spanish (tú forms, no voseo).
- **Spanish localization across all major screens**: lesson map progress, theory sheet, placeholder screens, registration flow, home dashboard, profile screen, auth flows (login/register) all render Spanish copy from centralized string resources.
- **English fallback**: Devices set to English locale will see English equivalents from `values-en/strings.xml`, but no in-app switcher was added (device-level locale only).
- **Preserved data integrity**: `StudentTrack.displayName` and persistence contracts remain unchanged, ensuring no data loss or orphaning.

## Session & Metadata

- **Archive phase**: completed by `sdd-archive` executor
- **Mode**: hybrid (openspec filesystem + engram observations)
- **Date**: 2026-08-01
