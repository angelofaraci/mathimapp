# Review Ledger: profile-visual-refresh

## Judgment Day — Design Phase

**Outcome:** RESOLVED — the user supplied decisions for the two user-impacting findings. Planning artifacts now incorporate them; the remaining findings are informational.

| id | lens | location | severity | status | evidence |
| --- | --- | --- | --- | --- | --- |
| JD-A-001 | readability | `specs/profile-hub-navigation/spec.md`; `design.md` | WARNING | verified | Reconciled to idiomatic English Kotlin enum identifiers: `HUB`, `ACCOUNT`, `PREFERENCES`, `HELP`, and `ABOUT`; rendered labels remain localized. |
| JD-A-002 | risk | `design.md:65`; design handoff | WARNING | info | The navigation-card contract does not explicitly name the required per-card icon/color inputs. |
| JD-A-003 | resilience | `specs/profile-screen/spec.md`; `design.md` | WARNING | verified | The spec and design now explicitly preserve loading and error branches before hub content renders. |
| JD-B-001 | reliability | `design.md`; `specs/profile-hub-navigation/spec.md` | CRITICAL | verified | User decision: Android system back from any local profile sub-screen MUST return to the Profile hub. Both scoped re-judges verified the enabled-only-outside-hub `BackHandler` contract. |
| JD-B-002 | risk | `design.md`; `specs/profile-screen/spec.md` | CRITICAL | verified | User decision: do NOT render a streak chip. Both scoped re-judges verified its consistent removal because existing `streak` is not consecutive days. |
| JD-B-003 | judgment-day | `specs/profile-hub-navigation/spec.md`; `design.md` | WARNING | verified | Reconciled to the same idiomatic English Kotlin enum identifiers in both artifacts. |
| JD-B-004 | judgment-day | `specs/profile-screen/spec.md`; `design.md` | WARNING | verified | The streak chip is omitted consistently from the spec and design. |
| JD-B-005 | reliability | `design.md:56`; `composeApp/build.gradle.kts` | WARNING | info | The proposed JVM Compose test setup may need runner compatibility confirmation. |
| JD-B-006 | reliability | `design.md:33` | WARNING | info | Unsaved local destination resets to the hub after configuration change. |
| JD-B-007 | readability | `design.md:44`; design handoff | SUGGESTION | info | Existing button/tokens may not exactly represent handoff styles. |

## Gatekeeper Result

The automatic design gate passed with warnings after its required retry. It independently confirmed the source paths and scope. It also found the enum-name drift, conditional streak-chip discrepancy, and implicit avatar/role fallback details.

## Findings Status

- BLOCKER: 0
- CRITICAL: 0 open; JD-B-001 and JD-B-002 are verified by scoped re-judgment.
- WARNING/SUGGESTION: informational only; they do not trigger a fix loop.

## Task 3.2 — Android System-Back Runtime Evidence

**Outcome:** VERIFIED — user-provided manual Android runtime evidence closed the final blocked verification task on 2026-07-13.

| id | lens | location | severity | status | evidence |
| --- | --- | --- | --- | --- | --- |
| R2-001 | reliability | Android Profile local navigation | CRITICAL | verified | The user manually validated that, from a Profile sub-screen, the Android system back button/gesture returns to the Profile hub and does not close the app. This satisfies the system-back scenario in `specs/profile-hub-navigation/spec.md`. |

## Judgment Day — Work Unit 1

**Outcome:** APPROVED. The foundation slice passed both the fresh gate and blind review. No BLOCKER or CRITICAL findings were found.

| id | lens | location | severity | status | evidence |
| --- | --- | --- | --- | --- | --- |
| R1-001 | reliability | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileNavigationCard.kt:28-32` | WARNING | info | The clickable modifier should be clipped to the rounded `MCard` shape so its ripple does not draw outside card corners. Address while wiring the card in work unit 2. |
| R1-002 | reliability | `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileListRow.kt:24-28` | WARNING | info | The clickable modifier should be clipped to the rounded `MCard` shape so its ripple does not draw outside card corners. Address while wiring the row in work unit 2. |

## Pre-Commit Review

| id | lens | location | severity | status | evidence |
| --- | --- | --- | --- | --- | --- |
| R3-001 | reliability | `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/ProfileScreenTest.kt:53-70` | BLOCKER | refuted | All cards use the same tested `AnimatedContent` destination mechanism; missing duplicate assertions are coverage work, not a demonstrated blocker. |
| R3-002 | reliability | `composeApp/src/androidMain/kotlin/com/example/proyectofinal/ui/BackHandler.android.kt:7-9` | BLOCKER | refuted | Android delegates to Compose `BackHandler`; recorded manual device validation confirms system back returns to the hub. |
| R3-003 | reliability | `server/src/main/resources/db/migration/V5__exercise_payload_json.sql:5` | WARNING | verified | Real PostgreSQL/Flyway startup validated the current checksum and records V5 as successful with `ALTER COLUMN type TYPE VARCHAR(50)`. |
