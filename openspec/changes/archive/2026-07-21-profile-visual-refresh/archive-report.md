# Archive Report: profile-visual-refresh

**Date**: 2026-07-21
**Status**: archived
**Artifact store**: openspec

## Executive Summary

Profile screen visual refresh replacing gamified dashboard with hub layout, local sub-screen navigation, and stubbed sub-screens. All 15 tasks across 3 phases complete. Full JVM test suites pass (server 59/59, composeApp 106/106). The change is archived with documented nonblocking coverage warnings and a native-gate exception due to environment limitations.

## Scope Delivered

- **ViewModel fields**: added `email` and `role` to `ProfileUiState`; mapped from `AuthSession.user`
- **Three presentational primitives**: `ProfileNavigationCard`, `ProfileToggleRow`, `ProfileListRow` in `ui/primitives/`
- **Profile hub layout**: centered identity (avatar initials, name, email, role chip), four navigation cards, logout button, version caption
- **Local sub-screen navigation**: `ProfileSubScreen` enum (`HUB`, `ACCOUNT`, `PREFERENCES`, `HELP`, `ABOUT`) with `AnimatedContent` switcher; `BackHandler` for Android system back
- **Stubbed sub-screens**: Cuenta, Preferencias, Ayuda y soporte, Acerca de — all TODO no-ops
- **Tests**: `ProfileScreenTest` (hub rendering, nav, back, logout, initials fallback) + `ProfileViewModelTest` (email/role success + error fallback)

## Tasks

| Phase | Tasks | Status |
|-------|-------|--------|
| 1: Foundation — ViewModel & Primitives | 6 | ✅ 6/6 |
| 2: Core — ProfileScreen Hub & Sub-Screens | 6 | ✅ 6/6 |
| 3: Testing & Verification | 3 | ✅ 3/3 |
| **Total** | **15** | **✅ 15/15** |

## Verification Evidence

### Fresh Verification (2026-07-21)

| Command | Result | Notes |
|---------|--------|-------|
| `./gradlew :server:test` | 59/59 PASS (7 suites) | Backend unaffected; green baseline |
| `./gradlew :composeApp:jvmTest` | 106/106 PASS (23 suites) | Original verify had 100; growth from other changes archived today |
| `./gradlew :composeApp:assembleDebug` | BLOCKED (environment only) | Android SDK visible from WSL is the Windows install; no Linux aapt binaries. Not a code failure. Original verify-report recorded assembleDebug green from Windows at implementation time. |

### Original Verification (2026-07-13)

- `./gradlew :composeApp:jvmTest` — 100/100 PASS (22 suites)
- `./gradlew :composeApp:assembleDebug` — BUILD SUCCESSFUL (Android runtime evidence R2-001)

### Spec Compliance

| Domain | Requirement | Scenarios | Status |
|--------|-------------|-----------|--------|
| profile-hub-navigation | ProfileSubScreen Enum | 2 scenarios | ✅ COMPLIANT |
| profile-hub-navigation | Intra-Tab Sub-Screen Switching | 4 scenarios | ✅ COMPLIANT (3 runtime + 1 manual evidence) |
| profile-hub-navigation | Stubbed Sub-Screen Composables | 4 scenarios | ⚠️ PARTIAL — 1 runtime, 3 source-confirmed (coverage gap) |
| profile-hub-navigation | No Functional Wiring in Stubs | 1 scenario | ✅ COMPLIANT (source-only) |
| profile-screen | Profile Screen Layout (MODIFIED) | 4 scenarios | ✅ COMPLIANT |
| profile-screen | Hub Identity Data Fields (ADDED) | 2 scenarios | ✅ COMPLIANT |
| profile-screen | Loading and Error States Preserved (ADDED) | 2 scenarios | ⚠️ PARTIAL — source-confirmed, no runtime assertion |
| profile-screen | Bottom Nav Shell Preserved (ADDED) | 2 scenarios | ⚠️ PARTIAL — source-confirmed via diff scope |

**Acceptance-critical requirements**: 8/8 PASSED (see verify-report Acceptance-Critical Requirements Summary).

## Review Evidence

**Lineage**: review-854efe9712307f28 (bounded review)

### Review Receipt

- **Receipt path**: `.git/gentle-ai/review-transactions/v2/review-854efe9712307f28/review-receipt.json`
- **Terminal state**: `escalated`
- **Risk**: medium
- **Lens**: review-reliability

### Key Findings

| ID | Lens | Severity | Resolution |
|----|------|----------|------------|
| R3-001 | reliability | CRITICAL | Stale Gradle 4.4.1 wrapper copies under `scripts/` — 4 duplicate files deleted by maintainer. Correction (265 lines) exceeded ordinary budget (144), triggering escalation. |
| JD-B-001 | reliability | CRITICAL | Android system back from profile sub-screen MUST return to hub — verified via manual runtime evidence R2-001 |
| JD-B-002 | risk | CRITICAL | Streak chip omitted (capped streak not consecutive days) — verified consistent removal |

### Scoped Re-Review

**Round 1**: JUDGMENT: APPROVED — Both CRITICAL findings independently verified.

### Native-Gate Exception (User-Approved 2026-07-21)

Archive proceeds WITHOUT `reviewGate.result: allow`. Cause: environment limitation — the gentle-ai review facade cannot reliably operate its git-CAS store on this DrvFs Windows mount (git subprocess timeouts from I/O latency; native privacy checks impossible on 0777 directories). This is an environment constraint, not a quality waiver: the reliability lens ran, its only blocker was adjudicated and fixed by maintainer action, and full test suites are green.

## Canonical Specs Modified

| Spec File | Changes Applied |
|-----------|-----------------|
| `openspec/specs/profile-screen/spec.md` | MODIFIED: "Profile Screen Layout" (replaced gamified dashboard with hub layout, added 2 new scenarios); ADDED: "Hub Identity Data Fields" (1 requirement, 2 scenarios), "Loading and Error States Preserved" (1 requirement, 2 scenarios), "Bottom Nav Shell Preserved" (1 requirement, 2 scenarios) |
| `openspec/specs/profile-hub-navigation/spec.md` | **Created** — new canonical spec with Purpose + 4 Requirements (ProfileSubScreen Enum, Intra-Tab Sub-Screen Switching, Stubbed Sub-Screen Composables, No Functional Wiring in Stubs) |

## Carried Acknowledgments

1. **Nonblocking coverage warnings (W1–W8)**: scenario-level coverage gaps (missing dedicated runtime assertions on runtime-exercised shared scaffold paths and source-confirmed branches). All 8 acceptance-critical requirements PASSED; gaps are coverage, not correctness.
2. **W9 (stray modified `V5__exercise_payload_json.sql`)**: MOOT — proven pure CRLF line-ending noise (`git diff --ignore-cr-at-eol` shows zero content change); working tree normalized via repo-root `.gitattributes` (`* text=auto eol=lf`); phantom diff no longer appears.
3. **Android system-back scenario**: closed by recorded manual runtime evidence (R2-001) per review-ledger.
4. **TODO-comment scenarios**: structurally untestable at runtime (source comment is not runtime-observable); source inspection is the only valid verification method.

## Archived Folder

`openspec/changes/archive/2026-07-21-profile-visual-refresh/`

### Archive Contents

- proposal.md ✅
- exploration.md ✅
- design.md ✅
- tasks.md ✅ (15/15 tasks complete)
- verify-report.md ✅
- review-ledger.md ✅
- specs/profile-screen/spec.md ✅ (delta)
- specs/profile-hub-navigation/spec.md ✅ (delta → new canonical)

## SDD Cycle Complete

The change has been fully planned, implemented, verified, and archived.
Ready for the next change.