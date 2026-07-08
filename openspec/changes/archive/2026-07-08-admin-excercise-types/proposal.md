# Proposal: Typed Exercise Payloads (Phase 1)

## Intent

Replace the flat `Exercise` model with polymorphic typed payloads so the platform can support MultipleChoice, InputValue, and MultiSelect exercises in a scalable way. This unblocks the production-ready admin workflow and enables type-aware rendering and validation in the app.

## Scope

### In Scope
- Refactor shared `Exercise` to base metadata + sealed `ExercisePayload` (MultipleChoice, InputValue, MultiSelect).
- Add `payload` JSON column to `Exercises` table via Flyway migration.
- Update backend service, admin routes, and DTOs to serialize/validate payloads.
- Rebuild admin-web exercise form with dynamic fields per type.
- Update Compose app `LessonMapScreen` and `LessonMapViewModel` to dispatch type-specific player composables and validators.
- Implement wrong-answer immediate retry with feedback in the player.
- Update SQLDelight `ExerciseEntity` schema and queries to store payload JSON.

### Out of Scope
- Additional payload types (DragDrop, OrderSteps, NumberLine, etc.) — Phase 2+.
- Offline exercise creation or student-side editing.
- Backend attempt-history tracking beyond current completion model.

## Capabilities

### New Capabilities
- `exercise-type-player`: Type-specific player rendering and answer validation in the Compose app for MultipleChoice, InputValue, and MultiSelect, with immediate retry and feedback for wrong answers.

### Modified Capabilities
- `admin-exercise-crud`: Admin exercise creation/update requirements change from flat `question/options/correctAnswer` fields to typed `ExercisePayload` with per-type validation.
- `client-server-contract`: Shared `Exercise` contract changes to base metadata + polymorphic `payload`; serialization contracts updated.
- `lesson-read-access`: Exercise answer-hiding logic updated to strip correct answers from typed payloads for student-facing responses.

## Approach

Adopt Approach 2 from exploration: sealed-class payloads with `kotlinx.serialization`. Store payloads as JSON in a new `TEXT` column. Keep old flat columns in DB temporarily (unused) and drop them in a follow-up migration after Phase 1 is verified. Deploy backend and app together as a breaking, coordinated release.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/.../Models.kt` | Modified | `Exercise` becomes base + sealed `ExercisePayload` |
| `server/.../Tables.kt` | Modified | Add `payload` TEXT column; keep old columns temporarily |
| `server/.../ExerciseService.kt` | Modified | Serialize/deserialize payloads; validate per type |
| `server/.../adminRoutes.kt` | Modified | DTOs use payload shape |
| `server/.../AdminDtos.kt` | Modified | Admin request/response DTOs updated |
| `admin-web/src/pages/Exercises.tsx` | Modified | Dynamic form layout per exercise type |
| `composeApp/.../LessonMapScreen.kt` | Modified | Dispatch to type-specific player composables |
| `composeApp/.../LessonMapViewModel.kt` | Modified | Type-specific answer validation and retry logic |
| `composeApp/.../AppDatabase.sq` | Modified | Store payload JSON instead of flat fields |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Cross-module compilation breakage | High | Update shared, server, and composeApp in same branch; verify with `./gradlew build` |
| PR exceeds 400-line review budget | Med | Slice into chained PRs: shared+server schema, admin-web, composeApp player |
| Admin-web has no tests | High | Manual verification checklist; defer minimal test runner to follow-up |
| JSON payload validation bloat | Med | Centralize per-type validators in `ExerciseService` |

## Rollback Plan

Revert the Flyway migration (drop `payload` column). Restore previous `Exercise` model and flat DTOs from Git. Re-deploy backend and app together. Old flat columns remain in DB until follow-up cleanup, so rollback is non-destructive.

## Dependencies

- Coordinated deploy of backend and app (no dual-format compatibility).
- Flyway migration must run before backend startup.

## Success Criteria

- [ ] Admin can create and update MultipleChoice, InputValue, and MultiSelect exercises with type-specific fields.
- [ ] App renders each type correctly and validates answers with immediate retry feedback.
- [ ] `./gradlew build` passes across all modules.
- [ ] Existing exercise data remains readable after migration.
