# Exploration: course-empty-state

## Current State
`CourseContent` in `App.kt` renders three states (`Loading`, `Success`, `Error`). In `Success`, it delegates to `CourseList(state.courses)`. `CourseList` unconditionally shows a `LazyColumn`; when `courses` is empty, the user sees only the "Welcome to MathApp!" title and a blank area below it. There is no empty-state messaging or visual cue.

## Affected Areas
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` — `CourseList` composable lacks empty-list handling.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/CourseViewModel.kt` — no changes required; `Success(emptyList())` is a valid state.
- No existing Compose UI tests; repository tests in `commonTest` are unaffected.

## Approaches

1. **Handle empty state inside `CourseList`** — Add an `if (courses.isEmpty())` branch that renders a centered message (and optionally an icon) instead of the `LazyColumn`.
   - Pros: Minimal change, no state architecture touched, localized to one composable, easy to preview.
   - Cons: None significant for this scope.
   - Effort: Low

2. **Introduce `CourseUiState.Empty`** — Extend the sealed interface and have the ViewModel emit `Empty` when the fetched list is empty.
   - Pros: More explicit state machine.
   - Cons: Overkill for a simple list empty state; increases coupling between ViewModel and a purely presentational concern; changes more files.
   - Effort: Medium

3. **Handle it in `CourseContent`** — Check `state.courses.isEmpty()` before calling `CourseList` and render a separate empty-screen composable.
   - Pros: Keeps `CourseList` focused on rendering a non-empty list.
   - Cons: Splits the success visual path; slightly more indirection for a small change.
   - Effort: Low

## Recommendation
**Option 1** — handle the empty state directly inside `CourseList`. It is the lowest-risk, most localized change and follows Compose conventions (the UI decides how to present data). `Success(emptyList)` remains a valid success state.

## Risks
- Very low risk. No new dependencies. No state machine changes. No backend impact.
- No existing UI tests to update, but a new preview for the empty state is recommended.

## Ready for Proposal
Yes. This is a straightforward, low-risk UI enhancement. The next phase (`sdd-propose`) can define the exact message text and whether to include an icon.
