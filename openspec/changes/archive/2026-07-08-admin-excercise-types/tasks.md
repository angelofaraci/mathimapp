# Tasks: Typed Exercise Payloads (Phase 1)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 550–700 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: shared+server → PR 2: admin-web → PR 3: composeApp |
| Delivery strategy | ask-always |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Shared contract + server (schema, migration, service, routes, answer stripping) | PR 1 | base: main; includes server tests; foundation for all others |
| 2 | Admin-web type-aware exercise editor | PR 2 | base: PR 1 branch; manual verification (no test runner) |
| 3 | Compose app type-specific player, retry logic, local payload storage | PR 3 | base: PR 1 branch; includes VM + SQLDelight tests |

## Phase 1: Shared Contract & Server Foundation

- [x] 1.1 Modify `shared/.../Models.kt`: replace flat Exercise with base + sealed ExercisePayload, ChoiceOption, typed attempt DTOs
- [x] 1.2 Modify `server/.../database/Tables.kt`: add `payload` TEXT column; widen type; keep old columns for rollback
- [x] 1.3 Create `server/.../db/migration/V5__exercise_payload_json.sql`: add nullable payload, backfill JSON, map TRUE_FALSE→MULTIPLE_CHOICE, set NOT NULL
- [x] 1.4 Modify `server/.../service/ExerciseService.kt`: add payload serialize/deserialize and per-type validation rules
- [x] 1.5 Modify `server/.../service/ServiceMappers.kt`: decode payload; map question→title; strip answers for student role
- [x] 1.6 Modify `server/.../routes/adminRoutes.kt`: admin CRUD consumes/returns typed payloads; update DTOs
- [x] 1.7 Modify `server/.../routes/userRoutes.kt` + `service/UserService.kt`: add attempt validation endpoint (wrong/correct evaluation)

## Phase 2: Admin Web Type-Aware Editor

- [x] 2.1 Modify `admin-web/src/pages/Exercises.tsx`: convert form to discriminated union with per-type fields (option list, text input, checkbox set)

## Phase 3: Compose App Player & ViewModel

- [x] 3.1 Modify `composeApp/.../ui/activities/LessonMapViewModel.kt`: replace `selectedAnswer` with typed draft/attempt/retry state machine
- [x] 3.2 Modify `composeApp/.../ui/activities/LessonMapScreen.kt`: dispatch to type-specific composables (radio cards, text input, checkboxes) and feedback states
- [x] 3.3 Modify `composeApp/.../data/UserApi.kt` (or ExerciseApi.kt): send typed attempt requests to new endpoint
- [x] 3.4 Modify `composeApp/.../sqldelight/.../AppDatabase.sq`: store payload JSON; drop cached flat answer columns

## Phase 4: Testing

- [x] 4.1 Server unit: payload serializer round-trip, per-type validation rules, attempt evaluation per spec scenarios
- [x] 4.2 Server integration: admin CRUD with typed payloads, migration backfill correctness, attempt endpoint wrong/correct flows
- [x] 4.3 ViewModel unit: retry/advance logic, wrong-answer feedback state transitions, payload caching
- [x] 4.4 SQLDelight test: payload JSON round-trip through local DB storage
