# Review Ledger: auth-registration-flow-stability

## Judgment Day — Design

**Round:** 1  
**Verdict:** APPROVED

| id | lens | location | severity | status | evidence |
|---|---|---|---|---|---|
| JD-B-001 | judgment-day | openspec/changes/auth-registration-flow-stability/specs/onboarding-flow/spec.md:30-36 | SUGGESTION | info | Pre-existing `CourseScreen` naming is stale; it is not user-impacting for this change. |
| JD-B-002 | judgment-day | openspec/changes/auth-registration-flow-stability/design.md:38 | SUGGESTION | info | `HomeDashboardScreen.kt` appears in the design but not the proposal table; this is documentation-only scope drift. |

## Gate Review Notes

- `sdd-design` gate: PASS WITH WARNINGS.
- Automated recreation coverage for all auth-rotation scenarios should be planned where feasible; platform smoke testing remains required.

## Judgment Day — Applied Implementation

**Round:** 1  
**Verdict:** ESCALATED — independent judges reported different critical candidates; neither is confirmed for automatic remediation.

| id | lens | location | severity | status | evidence |
|---|---|---|---|---|---|
| JD-A-001 | judgment-day | composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/OnboardingScreen.kt:289-325 | BLOCKER | verified | `ConfirmationStep` now uses a vertical `Column` within the weighted step region, keeping the confirmation details and completion action separate and reachable. |
| JD-A-002 | judgment-day | composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt:63 | WARNING | info | Koin resolution of the ViewModel default constructor argument needs device smoke confirmation. |
| JD-B-003 | judgment-day | composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/OnboardingViewModel.kt:133-140 | WARNING | info | Category validation text is misleading when no category was selected. |
| JD-B-004 | judgment-day | composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterScreen.kt:46-104; composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterViewModel.kt:108-119 | CRITICAL | verified | Steps after the first now render an in-wizard Back action wired to `RegisterViewModel.goBack`, which returns to the preceding step while preserving entered values. |
| JD-B-005 | judgment-day | composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterViewModel.kt:108-154 | WARNING | info | A retained registration ViewModel may preserve submitted credentials after a later logout. |

Only BLOCKER/CRITICAL candidates require resolution. Warnings remain informational and do not drive a fix round.

## Scoped Re-Review — Round 1

**Judges:** A and B  
**Verdict:** JUDGMENT: APPROVED

Both repaired findings were independently verified. The English `Back` label noted on a fix-touched line is informational only.

## Pre-Push Reliability Review

| id | lens | location | severity | status | evidence |
|---|---|---|---|---|---|
| R3-001 | reliability | composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt:68-74 | CRITICAL | verified | Logout resets the retained main router to Home before clearing the authentication session; focused JVM coverage verifies ordering and retained state. |
