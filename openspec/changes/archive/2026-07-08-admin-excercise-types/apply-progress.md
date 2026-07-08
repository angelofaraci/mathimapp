## Implementation Progress

**Change**: admin-excercise-types
**Mode**: Standard

### Completed Tasks
- [x] 1.1 `shared/.../Models.kt` — replaced the flat exercise contract with typed payloads, compatibility accessors, and typed attempt DTOs
- [x] 1.2 `server/.../database/Tables.kt` — added persisted payload support in the Exposed schema and widened the type column
- [x] 1.3 `server/.../db/migration/V5__exercise_payload_json.sql` — added the payload backfill migration and normalized legacy `TRUE_FALSE` rows
- [x] 1.4 `server/.../service/ExerciseService.kt` — centralized payload serialization, deserialization, validation, stripping, and attempt evaluation rules
- [x] 1.5 `server/.../service/ServiceMappers.kt` — mapped `question` to shared `title` and stripped typed answers for student lesson reads
- [x] 1.6 `server/.../routes/adminRoutes.kt` + DTOs — moved admin exercise CRUD to typed payload requests/responses
- [x] 1.7 `server/.../routes/userRoutes.kt` + `service/UserService.kt` — added `/exercises/{id}/attempt` with wrong/correct evaluation and progress updates
- [x] 2.1 `admin-web/src/pages/Exercises.tsx` — rebuilt the editor around a discriminated union form with typed payload submission, per-type controls, and typed listing/edit flows
- [x] 4.1 Server unit — covered payload compatibility, per-type validation, and typed attempt evaluation in service-layer tests
- [x] 4.2 Server integration — covered typed admin/public exercise flows, migration backfill behavior, answer stripping, and attempt endpoint wrong/correct responses
- [x] 3.1 `composeApp/.../LessonMapViewModel.kt` — replaced the flat answer field with typed draft/attempt/retry state, backend attempt submission, and wrong-answer retry handling
- [x] 3.2 `composeApp/.../LessonMapScreen.kt` — dispatched typed player UI for multiple choice, input value, and multi-select payloads with feedback-aware submit states
- [x] 3.3 `composeApp/.../data/UserApi.kt` + `domain/UserRepository.kt` — sent typed attempt submissions to `/exercises/{id}/attempt` and synced returned progress locally
- [x] 3.4 `composeApp/.../sqldelight/.../AppDatabase.sq` — replaced cached flat exercise fields with `title` + payload JSON and added local schema repair for legacy rows
- [x] 4.3 Compose ViewModel unit — covered retry/advance behavior, wrong-answer feedback transitions, and client-side blank input rejection
- [x] 4.4 SQLDelight/local DB tests — covered payload JSON persistence round-trip and legacy cached exercise row repair into the new schema

### Files Changed
| File | Action | What Was Done |
|------|--------|---------------|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modified | Added typed payload contracts, typed attempt DTOs, and backward-compatible exercise accessors/helpers. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` | Modified | Added `payload` persistence and widened the stored exercise type column. |
| `server/src/main/resources/db/migration/V5__exercise_payload_json.sql` | Created | Backfilled JSON payloads from legacy flat rows and normalized `TRUE_FALSE` to `MULTIPLE_CHOICE`. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ExerciseService.kt` | Rewritten | Added payload codec/validation helpers plus typed admin/public mutation support. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ServiceMappers.kt` | Modified | Decoded payload JSON and stripped answer fields for students only. |
| `server/src/main/kotlin/com/example/proyectofinal/service/UserService.kt` | Modified | Added typed attempt evaluation and shared completion bookkeeping. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/userRoutes.kt` | Modified | Exposed the new typed attempt endpoint. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/adminRoutes.kt` | Modified | Returned typed payloads from admin CRUD endpoints. |
| `server/src/main/kotlin/com/example/proyectofinal/models/{AdminDtos,ExerciseDto}.kt` | Modified | Updated backend request/response DTOs to the typed payload shape. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ContentReadAccess.kt` | Modified | Kept full answers for admin/creator reads and hid typed answers for students only. |
| `server/src/main/kotlin/com/example/proyectofinal/seed/SeedData.kt` | Modified | Seeded legacy exercise rows with payload JSON so fresh databases match the new schema. |
| `server/src/test/kotlin/com/example/proyectofinal/{ServiceLayerTest,AdminIntegrationTest,ServerIntegrationTest}.kt` | Modified | Added typed payload, migration, answer-stripping, attempt endpoint coverage, and admin validation regression coverage for the verify blockers. |
| `admin-web/src/pages/Exercises.tsx` | Rewritten | Converted the admin editor from flat text fields to a type-aware form and list backed by typed exercise payloads. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/activities/{LessonMapViewModel,LessonMapScreen,LessonMapUiState}.kt` | Modified | Added typed answer draft state, backend-driven retry flow, and type-dispatched exercise player UI. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/{UserApi,KtorUserRepository,KtorExerciseRepository,ExercisePayloadJson}.kt` | Modified | Added typed attempt submission, local progress syncing, and payload JSON encoding for cached exercises. |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modified | Replaced local flat exercise cache columns with `title` and typed payload JSON. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/LocalDatabaseSchemaFixes.kt` | Modified | Repaired legacy cached exercise rows into the new payload-based schema before opening SQLDelight. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/{ComposeAppCommonTest,data/KtorExerciseRepositoryTest,data/KtorUserRepositoryTest,ui/activities/LessonMapViewModelTest,ui/ProfileViewModelTest,ui/home/HomeDashboardViewModelTest}.kt` | Modified | Updated repository fakes/tests for typed attempts and added compose/local DB coverage for the new player slice. |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/di/LocalDatabaseSchemaFixesTest.kt` | Modified | Added regression coverage for legacy exercise cache repair into payload JSON rows. |
| `openspec/changes/admin-excercise-types/tasks.md` | Modified | Recorded the resolved feature-branch-chain strategy and marked the completed PR 1, PR 2, and PR 3 tasks. |
| `openspec/changes/admin-excercise-types/apply-progress.md` | Modified | Recorded the verify-blocker remediation batch and the rerun of `./gradlew :server:test`. |

### Verification
| Command | Result |
|---------|--------|
| `./gradlew :server:test` | Passed (rerun after post-archive blocker remediation) |
| `./gradlew :composeApp:jvmTest` | Passed (rerun after closing the completion bypass and removing the dead client contract) |
| `npm run build` (in `admin-web/`) | Passed |

### Remediation Updates
- Added runtime admin integration coverage for the three verify blockers: MultipleChoice single-option rejection, InputValue blank `correctValue` rejection, and MultiSelect invalid `correctOptionIds` rejection.
- Confirmed the current server behavior already matches the documented spec messages, so no product code changes were required.
- Closed the legacy student `/exercises/{id}/complete` path with `410 Gone`, removed the dead Compose `completeExercise` API/repository contract, and rerouted regression coverage to the typed `/attempt` flow only.
- Fixed Flyway V5 payload backfill so legacy comma-delimited options are converted into typed JSON with runtime-consistent trimming, and added regression coverage for both multi-option and spaced legacy rows.
- Removed `points` and `difficulty` from the public exercise contracts/DTOs because they were never persisted and could not round-trip safely.
- Synced the archived OpenSpec design/spec/verification artifacts to the final shipped `Exercise(id, lessonId, title, type, payload)` contract and remediation evidence.

### Deviations from Design
- The admin web editor exposes the `InputValuePayload.placeholder` field as optional metadata so editing an existing typed payload does not silently discard placeholder content.
- The remediation slice removed `points` and `difficulty` from the public exercise contract instead of persisting them, because they were never backed by database columns and were not used by the shipped clients.

### Issues Found
- This foundation slice landed well above the 400-line review budget because the shared contract, migration, server serialization, compatibility shims, and regression coverage all had to move together.
- Backward compatibility for existing app/test code required shared `Exercise` compatibility helpers instead of a pure clean-break model in this slice.
- The admin-web slice also exceeds the 400-line review target because `Exercises.tsx` needed a full type-aware rewrite rather than an additive patch over the legacy flat editor.
- The Compose slice also needed a local SQLDelight repair step for legacy cached `ExerciseEntity` rows; without that repair, upgrading existing local databases would break when the payload-based schema opened.
- Even scoped to the compose work unit, the player UI rewrite, local schema repair, and regression coverage still push this slice above the 400-line review budget.

### Remaining Tasks
- [ ] None.

### Workload / PR Boundary
- Mode: chained PR slice
- Current work unit: post-archive blocker remediation slice
- Boundary: starts from the already completed archived implementation and ends after the completion bypass is closed, the V5 backfill is made multi-option safe, and the non-persisted exercise metadata fields are removed from the public contract
- Estimated review budget impact: Medium; the slice stays surgical but spans shared, server, Compose repository contracts, and regression tests

### Status
16/16 tasks complete. Post-archive blocker remediation applied and `:server:test` + `:composeApp:jvmTest` rerun; ready for verify/archive handoff.
