# Proposal: Course List Empty State

## Intent

The `CourseList` composable in `App.kt` renders a `LazyColumn` unconditionally. When no courses are available, the user sees only the "Welcome to MathApp!" title and a blank area. This creates a confusing experience because it is unclear whether data failed to load or simply does not exist. We will add a clear empty-state message when the course list is empty.

## Scope

### In Scope
- Add an empty-state branch inside `CourseList` when `courses.isEmpty()`.
- Display a centered message and optional icon in the empty state.
- Add a `@Preview` for the empty `CourseList` state.

### Out of Scope
- New `Empty` sealed class state in `CourseUiState`; `Success(emptyList())` remains valid.
- Backend changes, API contract changes, or new dependencies.
- Navigation, pull-to-refresh, or retry actions from the empty state.

## Capabilities

### New Capabilities
None

### Modified Capabilities
None

## Approach

Implement Option 1 from exploration: handle the empty state directly inside `CourseList`.

- Check `courses.isEmpty()` before rendering the `LazyColumn`.
- If empty, render a centered `Column` with:
  - An `Icon` (`Icons.Default.Menu` or similar) at 48dp.
  - A `Text` with message: "No courses available yet."
- Otherwise, render the existing `LazyColumn`.
- Add a `@Preview` composable for `CourseList(emptyList())` to verify the empty state in Android Studio / Fleet.

No ViewModel or backend changes are required.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modified | Add empty-list branch in `CourseList` and new preview. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Empty-state message localization not yet available | Low | Use a hardcoded English string consistent with current UI copy. |

## Rollback Plan

Revert the single commit that modifies `App.kt`. No database migrations, API contracts, or shared module changes are involved, so rollback is trivial.

## Dependencies

None

## Success Criteria

- [ ] Empty course list shows a centered message and icon instead of a blank area.
- [ ] Non-empty course list continues to render correctly.
- [ ] A `@Preview` for the empty state exists and renders in the IDE.
