## Exploration: Scalable Phase 1 Exercise Model for Admin and App

### Current State

**Shared Contract (`shared/src/commonMain/.../Models.kt`)**
- `Exercise` is a flat record: `id`, `lessonId`, `question`, `options: List<String>`, `correctAnswer: String`, `type: ExerciseType`.
- `ExerciseType` enum has exactly three values: `MULTIPLE_CHOICE`, `TRUE_FALSE`, `INPUT_VALUE`.
- The model assumes every exercise is a single question with a list of textual options and one textual correct answer.

**Database (`server/.../Tables.kt`)**
- `Exercises` table stores `options` as a comma-separated `varchar(500)` and `correctAnswer` as `varchar(255)`.
- No column for exercise-type-specific configuration, media, or structured answer shapes.
- The 500-char `options` limit will break for any exercise type that needs more than a handful of short text labels.

**Backend (`server/.../ExerciseService.kt`, `adminRoutes.kt`)**
- `ExerciseService` creates and updates exercises by joining options with commas and splitting them on read.
- `AdminExerciseResponse` mirrors the flat `Exercise` model exactly.
- `CreateAdminExerciseRequest` / `UpdateAdminExerciseRequest` expose the same flat fields.
- No validation of type-specific invariants (e.g., `TRUE_FALSE` should only have two options, `INPUT_VALUE` should ignore options).

**Admin Panel (`admin-web/src/pages/Exercises.tsx`)**
- The form is a generic question/options/correctAnswer builder with a type dropdown.
- `optionsText` is a newline-separated textarea that gets split into a list on submit.
- The UI does not adapt per type: even `TRUE_FALSE` still shows the options textarea.
- Exercise types are hardcoded in the TypeScript union: `'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'INPUT_VALUE'`.

**Compose App (`composeApp/.../LessonMapScreen.kt`, `LessonMapViewModel.kt`)**
- `LessonMapScreen` renders every exercise as a vertical list of radio-button cards (`AnswerOptionCard`).
- There is NO branching on `ExerciseType` in the UI layer. `TRUE_FALSE` and `INPUT_VALUE` are treated identically to `MULTIPLE_CHOICE`.
- `LessonMapViewModel.submitAnswer()` simply compares `selectedAnswer` to `exercise.correctAnswer`. This logic cannot support multi-select, drag-drop ordering, or numeric ranges.

**Reference Inventory (`docs/ui/excercises/`)**
- 13 PNG mockups show exercise types far beyond the current enum:
  - Drag-and-drop equations and terms
  - Number line placement (integers, absolute value)
  - Ordering steps (e.g., solving an equation)
  - Multi-select (combining like terms)
  - Fill-in-the-blank / complete text
  - Angle measurement multiple-choice
  - Statistical context completion
- None of these are implemented. They are design references for what the platform should support.

### Affected Areas

- `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` — `Exercise` and `ExerciseType` need expansion or replacement.
- `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` — `Exercises.options` and `Exercises.correctAnswer` columns are insufficient for typed payloads.
- `server/src/main/kotlin/com/example/proyectofinal/service/ExerciseService.kt` — Flat CRUD logic must be replaced or extended with type-specific creation/update rules.
- `server/src/main/kotlin/com/example/proyectofinal/routes/adminRoutes.kt` — `AdminExerciseResponse` and request DTOs must evolve.
- `server/src/main/kotlin/com/example/proyectofinal/models/AdminDtos.kt` — Admin exercise DTOs are flat and need restructuring.
- `admin-web/src/pages/Exercises.tsx` — The form must become type-aware with conditional fields.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/activities/LessonMapScreen.kt` — Exercise rendering must branch by type.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/activities/LessonMapViewModel.kt` — Answer validation must branch by type.
- Database migrations — Any schema change to `Exercises` requires a Flyway migration.

### Approaches

1. **Extend the Primitive Flat Contract**
   - Keep the current `Exercise` shape (`question`, `options`, `correctAnswer`, `type`) but add more enum values.
   - Abuse the existing string fields with type-specific conventions (e.g., `options` encodes drag-drop pairs as `"label|target,label|target"`, `correctAnswer` encodes order as `"B,A,C"`).
   - Minimal database change: only the `type` column widens slightly.
   - **Pros:**
     - Very low immediate churn.
     - No migration beyond adding enum values.
     - Admin-web form can stay mostly the same with minor hints.
   - **Cons:**
     - Unmaintainable as types grow. Each new type adds another string-parsing convention.
     - `options` varchar(500) is a hard ceiling for any moderately complex exercise.
     - Frontend and backend must secretly agree on parsing rules for every type.
     - Type validation is string-based and error-prone.
     - Cannot support media, rich text, or structured math expressions natively.
   - **Effort:** Low

2. **Introduce Typed Exercise Payloads from Phase 1**
   - Replace the flat `Exercise` model with a polymorphic/typed contract where each exercise type carries its own structured payload.
   - Example:
     - `MultipleChoicePayload { options: List<ChoiceOption>, correctOptionId: String }`
     - `DragDropPayload { items: List<DraggableItem>, targets: List<DropTarget>, correctMapping: Map<String, String> }`
     - `OrderStepsPayload { steps: List<Step>, correctOrder: List<String> }`
     - `NumberLinePayload { min: Int, max: Int, step: Int, correctValue: Int, hints: List<String> }`
     - `FillBlankPayload { segments: List<TextSegment>, blanks: List<Blank> }`
   - Store the payload as serialized JSON in a new `Exercises.payload` text column.
   - Keep a small common base (`id`, `lessonId`, `type`, `title/question`) and attach the typed payload.
   - **Pros:**
     - Scales indefinitely. New types only need a new payload class and a new renderer/validator.
     - Type-safe serialization with kotlinx.serialization.
     - Database schema stays stable after the initial JSON column addition.
     - Admin panel can render a dynamic form based on the selected type.
     - Frontend can switch composables by type cleanly.
   - **Cons:**
     - Larger initial refactor across `shared`, `server`, `admin-web`, and `composeApp`.
     - Loses simple SQL querying inside `options` or `correctAnswer` (must parse JSON in the app or use PostgreSQL JSONB operators on the backend).
     - Requires careful design of the payload hierarchy to avoid circular dependencies between `shared` and platform code.
   - **Effort:** High

3. **Hybrid — Preserve Flat Base + Add Optional JSON Payload Column**
   - Keep the current `Exercise` model as-is for backward compatibility.
   - Add a new `ExerciseV2` / `RichExercise` model alongside it, used only by the admin panel and new app screens.
   - Migrate exercises lazily: old exercises remain flat, new ones use payloads.
   - **Pros:**
     - Zero breakage of existing endpoints and screens.
     - Can introduce new types incrementally.
   - **Cons:**
     - Two parallel code paths in every layer (service, routes, UI, repository).
     - Admin panel must support editing both formats, increasing complexity.
     - Technical debt accumulates quickly. The flat path will eventually need a migration anyway.
   - **Effort:** Medium

### Recommendation

**Approach 2 (Typed Exercise Payloads from Phase 1)** is recommended.

Rationale:
- The flat contract is already at its breaking point. The reference inventory shows 10+ distinct exercise shapes, and the current `varchar(500)` options field cannot encode any of them cleanly.
- A typed payload model with a JSON column is the industry-standard pattern for configurable content in LMS platforms. It separates the stable base metadata from the variable type-specific data.
- Kotlin's ` kotlinx.serialization` + sealed classes make polymorphic payloads straightforward and type-safe across `shared`, `server`, and `composeApp`.
- Doing this NOW, before the app has dozens of legacy exercises in production, avoids a painful data migration later.
- The project is in pre-release (no production data). This is the safest moment to make the schema change.

Phase 1 Scope Lock:
- Define the base `Exercise` model with `id`, `lessonId`, `type`, `title`, plus a `payload: ExercisePayload` sealed-class property.
- Implement only **3 initial payload types** in Phase 1 to prove the pipeline:
  1. `MultipleChoicePayload` (replaces current `MULTIPLE_CHOICE` / `TRUE_FALSE`)
  2. `InputValuePayload` (replaces current `INPUT_VALUE`)
  3. `MultiSelectPayload` (first multi-answer type, validates the polymorphic UI path)
- Add a `payload` `TEXT` column to the `Exercises` table with a Flyway migration.
- Update `ExerciseService` to serialize/deserialize payloads and validate type-specific rules.
- Update `admin-web/Exercises.tsx` with a type selector that swaps the form layout dynamically.
- Update `LessonMapScreen` to dispatch to type-specific player composables.
- Keep the old flat fields (`options`, `correctAnswer`) physically in the DB during Phase 1 but stop using them for new exercises; a follow-up migration can drop them after Phase 1 is verified.

### Risks

- **Cross-module ripple:** `shared` model changes immediately break compilation in both `server` and `composeApp`. Both modules must be updated in the same change window.
- **Review budget:** This touches `shared`, `server`, `admin-web`, and `composeApp`. Even with only 3 payload types, the PR may approach or exceed the 400-line budget. Consider a chained-PR strategy:
  1. PR #1: `shared` model + `server` schema + backend serialization + Flyway migration.
  2. PR #2: `admin-web` type-aware editor.
  3. PR #3: `composeApp` type-aware player + answer validation.
- **Admin-web test gap:** `openspec/config.yaml` confirms `admin_web` has 0 tests. The dynamic form must be verified manually or with a quick Playwright/Cypress addition.
- **JSON payload validation burden:** The backend must validate payload shape per type. Without a schema registry, this logic lives in `ExerciseService` and can grow. Consider centralizing payload validators.
- **Backward compatibility during deploy:** If the backend deploys before the app, existing `MULTIPLE_CHOICE` exercises in the app must still render correctly. The Phase 1 payload for `MultipleChoicePayload` should be a strict superset of the current flat behavior so the old app can still understand basic exercises if needed, OR the change is treated as a breaking deploy requiring coordinated update.
- **SQLDelight local persistence:** If `composeApp` caches exercises locally, the SQLDelight schema must also store the payload JSON. This adds `composeApp/src/commonMain/sqldelight/**` to the affected list.

### Ready for Proposal

**Yes**, with the following items the orchestrator should confirm with the user before `sdd-propose`:

1. **Phase 1 type selection:** Resolved in implementation as `MultipleChoice`, `InputValue`, and `MultiSelect`.
2. **Deploy coordination:** Is this treated as a breaking change requiring app + backend to ship together, or should the backend temporarily support both flat and payload responses?
3. **Admin-web test investment:** Should a minimal test runner (e.g., Vitest + React Testing Library) be added to `admin-web` as part of this change, or is manual verification acceptable for Phase 1?
4. **SQLDelight caching:** Does the app currently cache exercises locally? If yes, the SQLDelight schema update must be included in the plan.
