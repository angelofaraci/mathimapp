# Archive Report: auth-registration-flow-stability

**Date**: 2026-07-21
**Status**: archived
**Artifact store**: openspec

## Executive Summary

Three critical defects blocking authentication and onboarding completion were fixed: Koin DI context destruction on configuration change (causing `AuthGate` and child ViewModels to observe divergent `AuthRepository` instances), onboarding province step pushing Continue off-screen, and rotation resetting the auth gate from register to login. All 15 tasks across 4 phases are complete. Full JVM test suites pass (server 59/59, composeApp 106/106). The change is archived with documented warnings for device-rotation manual-smoke scenarios and a design-table scope drift.

## Scope Delivered

- **Hoist Koin startup** to Android `MainActivity` and iOS `MainViewController`; remove `KoinApplication` from `App.kt`
- **Convert `AuthGateRouter` to `AuthGateViewModel`** and `MainRouter` to `MainRouterViewModel` — both now survive configuration changes via platform `ViewModelStore`
- **Fix `OnboardingScreen` layout** — step content wrapped in `Modifier.weight(1f)` container so Continue/Back remain visible
- **Register new ViewModels** in `AppModule` via `viewModelOf`
- **Delete `PlatformModule`** files (expect/actual pattern superseded by entry-point modules)
- **Add `KoinInitializer`** with guarded `getKoinOrNull()` idempotent startup
- **In-wizard Back action** for Register steps 2-3 (JD-B-004 CRITICAL fix from review-ledger)

## Tasks

| Phase | Tasks | Status |
|-------|-------|--------|
| 1: Koin Infrastructure | 5 | ✅ 5/5 |
| 2: ViewModel Conversion | 5 | ✅ 5/5 |
| 3: Onboarding Layout Fix | 1 | ✅ 1/1 |
| 4: Testing | 4 | ✅ 4/4 |
| **Total** | **15** | **✅ 15/15** |

## Verification Evidence

### Fresh Verification (2026-07-21)

| Command | Result | Notes |
|---------|--------|-------|
| `./gradlew :server:test` | 59/59 PASS (7 suites) | Backend unaffected; green baseline |
| `./gradlew :composeApp:jvmTest` | 106/106 PASS (23 suites) | Updated from 105 → 106 (new OnboardingScreenTest confirmed) |
| `./gradlew :composeApp:assembleDebug` | BLOCKED (environment only) | Android SDK visible from WSL is the Windows install; no Linux aapt binaries. Not a code failure. Original verify-report recorded assembleDebug green from Windows at implementation time (2026-07-17). |

### Original Verification (2026-07-17)

- `./gradlew :composeApp:jvmTest` — 105/105 PASS (23 suites, including 34.5s OnboardingScreenTest)
- `./gradlew :composeApp:assembleDebug` — BUILD SUCCESSFUL (69 tasks, 1m 24s)

### Spec Compliance

| Domain | Requirement | Scenarios | Status |
|--------|-------------|-----------|--------|
| frontend-auth | Auth Gate Survives Configuration Changes (ADDED) | 4 scenarios | ⚠️ PARTIAL — ViewModel-retention logic covered by tests; rotation-recomposition event requires manual device smoke |
| onboarding-flow | Action Buttons Always Reachable (ADDED) | 2 scenarios | ✅ COMPLIANT |
| onboarding-flow | Onboarding Completion and Navigation (MODIFIED) | 3 scenarios | ⚠️ PARTIAL — recomposition covered; rotation scenario requires manual device smoke |

**Compliance**: 5/9 scenarios COMPLIANT, 4/9 PARTIAL (device-rotation scenarios cannot execute in Linux/no-device environment).

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
| JD-A-001 | judgment-day | BLOCKER | `ConfirmationStep` vertical Column — verified fixed |
| JD-B-004 | judgment-day | CRITICAL | In-wizard Back action for Register steps 2-3 — verified fixed, approved in Scoped Re-Review |

### Scoped Re-Review

**Round 1**: JUDGMENT: APPROVED — Both repaired findings independently verified.

### Native-Gate Exception (User-Approved 2026-07-21)

Archive proceeds WITHOUT `reviewGate.result: allow`. Cause: environment limitation — the gentle-ai review facade cannot reliably operate its git-CAS store on this DrvFs Windows mount (git subprocess timeouts from I/O latency; native privacy checks impossible on 0777 directories). This is an environment constraint, not a quality waiver: the reliability lens ran, its only blocker was adjudicated and fixed by maintainer action, and full test suites are green.

## Canonical Specs Modified

| Spec File | Changes Applied |
|-----------|-----------------|
| `openspec/specs/frontend-auth/spec.md` | ADDED: "Auth Gate Survives Configuration Changes" (1 requirement, 4 scenarios) |
| `openspec/specs/onboarding-flow/spec.md` | ADDED: "Action Buttons Always Reachable" (1 requirement, 2 scenarios); MODIFIED: "Onboarding Completion and Navigation" (expanded description + 1 new scenario: device rotation) |

## Config Changes

| File | Change |
|------|--------|
| `openspec/config.yaml` | `testing.runners.composeApp.tests`: 92 → 106 (drift corrected per verify-report) |

## Carried Acknowledgments

1. **Device-rotation spec scenarios remain PARTIAL** — manual smoke on a physical device is still pending and cannot run in this Linux/no-device environment.
2. **Design-table deviation**: `RegisterScreen.kt`/`RegisterViewModel.kt` were implemented but not listed in the design's file-changes table. Reviewed and approved in Scoped Re-Review Round 1.
3. **Open informational review-ledger items**: JD-A-002 (Koin default-constructor device smoke), JD-B-003 (category validation text), JD-B-005 (retained credentials after logout) — all WARNING/info, none blocking.

## Archived Folder

`openspec/changes/archive/2026-07-21-auth-registration-flow-stability/`

### Archive Contents

- proposal.md ✅
- exploration.md ✅
- design.md ✅
- tasks.md ✅ (15/15 tasks complete)
- verify-report.md ✅
- review-ledger.md ✅
- specs/frontend-auth/spec.md ✅ (delta)
- specs/onboarding-flow/spec.md ✅ (delta)

## SDD Cycle Complete

The change has been fully planned, implemented, verified, and archived.
Ready for the next change.
