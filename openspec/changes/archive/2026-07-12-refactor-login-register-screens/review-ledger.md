# Review Ledger: refactor-login-register-screens

## Judgment Day — Design Phase, Round 1

| id | lens | location | severity | status | evidence |
| --- | --- | --- | --- | --- | --- |
| JD-A-001 | judgment-day | `design.md:36` | CRITICAL | info | Only Judge A found the scaffold-layout detail insufficient; Judge B found no matching CRITICAL issue. Recorded as a suspect, not a confirmed finding. |
| JD-A-002 | judgment-day | `design.md:59,65-66` | CRITICAL | info | Only Judge A found `PasswordStrength` undefined; Judge B assessed it as WARNING. No two-judge convergence. |
| JD-A-003 | judgment-day | `design.md:60` | CRITICAL | info | Only Judge A found `RegisterField` undefined; Judge B assessed it as WARNING. No two-judge convergence. |
| JD-A-004 | judgment-day | `design.md:65-66` | WARNING | info | Password-strength thresholds are advisory design detail; they do not block progression. |
| JD-B-001 | judgment-day | `design.md:59-60,65` | WARNING | info | Enum values and per-step validation mapping should be made explicit during implementation/testing. |
| JD-B-002 | judgment-day | `design.md:38,48-54,65` | WARNING | info | Preserve the existing required-field error contract while adding email-format validation. |

## Verdict

- Confirmed BLOCKER/CRITICAL findings: 0
- Suspect findings: 3
- WARNING/SUGGESTION findings: 3 (informational only)
- Fix rounds: 0

**JUDGMENT: APPROVED**

## Pre-commit Reliability Review

| id | lens | location | severity | status | evidence |
| --- | --- | --- | --- | --- | --- |
| R3-001 | reliability | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterViewModel.kt:193-201` | CRITICAL | refuted | The agreed spec and design define password strength as display-only; they do not require a minimum-length submission rule. |
| R3-002 | reliability | `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/LoginViewModelTest.kt:1-171` | BLOCKER | refuted | The approved test strategy uses ViewModel/routing runtime tests plus build/manual evidence for visual behavior; missing screen tests are not a release blocker. |

**Review result: no open BLOCKER or CRITICAL findings.**

## Judgment Day — Apply Batch 3, Round 1

| id | lens | location | severity | status | evidence |
| --- | --- | --- | --- | --- | --- |
| JD-A-001 | judgment-day | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterViewModel.kt:163` | WARNING | info | The rare generic repository-error fallback remains English in an otherwise Spanish registration experience. |
| JD-A-002 | judgment-day | `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/LoginViewModelTest.kt:137-171` | WARNING | info | `FakeAuthRepository` is shared implicitly via package scope rather than a test utility file. |

## Verdict — Apply Batch 3

- Confirmed BLOCKER/CRITICAL findings: 0
- WARNING/SUGGESTION findings: 2 (informational only)
- Fix rounds: 0

**JUDGMENT: APPROVED**

## Judgment Day — Apply Batch 2, Round 1

| id | lens | location | severity | status | evidence |
| --- | --- | --- | --- | --- | --- |
| JD-A-001 | judgment-day | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginViewModel.kt:51` | WARNING | info | The pre-existing required-fields message remains English while the login UI is Spanish. |
| JD-A-002 | judgment-day | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginViewModel.kt:31-39,51` | WARNING | info | A previous login error remains visible while fields are edited until the next submission. This behavior pre-existed. |
| JD-B-001 | judgment-day | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginViewModel.kt:51,72` | WARNING | info | Required-fields and fallback errors remain English in an otherwise Spanish login experience. |
| JD-B-002 | judgment-day | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginScreen.kt:221-225` | WARNING | info | Visual-only social buttons are enabled and inert rather than disabled or labelled as forthcoming. |

## Verdict — Apply Batch 2

- Confirmed BLOCKER/CRITICAL findings: 0
- WARNING/SUGGESTION findings: 4 (informational only)
- Fix rounds: 0

**JUDGMENT: APPROVED**

## Judgment Day — Apply Batch 1, Round 1

| id | lens | location | severity | status | evidence |
| --- | --- | --- | --- | --- | --- |
| JD-B-001 | judgment-day | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardScreen.kt:250-255` | WARNING | info | Disabled filled-button rendering changes outside auth because opacity is now applied globally. Informational only; the spec calls for this disabled appearance. |
| JD-B-002 | judgment-day | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MTextField.kt:44,60-63` | WARNING | info | Focus state is retained by non-auth fields even though the glow is opt-in. No functional defect confirmed. |
| JD-B-003 | judgment-day | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MTextField.kt:45` | SUGGESTION | info | An explicit shape is ignored when `authStyle` is enabled. No current caller uses this combination. |
| JD-B-004 | judgment-day | `openspec/config.yaml:12,48,58` | WARNING | info | Test-count metadata changed outside this work unit. It originated during SDD initialization and does not affect the slice behavior. |
| JD-B-005 | judgment-day | `composeApp/src/commonMain/composeResources/drawable/apple_logo.xml:7,10` | SUGGESTION | info | The Apple asset uses a fixed dark fill. Dark mode is intentionally out of scope. |

## Verdict — Apply Batch 1

- Confirmed BLOCKER/CRITICAL findings: 0
- WARNING/SUGGESTION findings: 5 (informational only)
- Fix rounds: 0

**JUDGMENT: APPROVED**
