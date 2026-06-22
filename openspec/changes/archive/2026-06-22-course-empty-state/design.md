# Design: Course List Empty State

## Technical Approach

Implement the empty state directly inside `CourseList(courses: List<Course>)` in `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt`. `CourseUiState.Success(emptyList())` remains the app state contract; only the successful content rendering changes. This matches the spec requirement that an empty list renders a centered message while non-empty lists keep the existing course rows.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Empty-state ownership | Keep the branch in `CourseList` | Add `CourseUiState.Empty` or change repository/ViewModel behavior | The current ViewModel already models successful loading with `Success(courses)`. The requested behavior is presentation-only, so changing state contracts would expand scope unnecessarily. |
| Dependency footprint | Use existing Compose Material3/layout primitives only | Add a material-icons dependency for `Icons.Default.Menu` | The version catalog has no icon dependency today. The spec requires a centered message; the proposal marks the icon as optional. Avoiding a dependency keeps the change UI-only and localized. |
| Preview placement | Add a second `@Preview` in `App.kt` near `AppPreview` | Create a separate preview file/package | Existing preview code lives in `App.kt`, and the change is small enough that colocating the preview keeps the pattern consistent. |

## Data Flow

No data contract changes.

    CourseViewModel ── Success(courses) ──→ CourseContent ──→ CourseList
                                                              │
                                                              ├─ emptyList() → empty message
                                                              └─ non-empty   → existing LazyColumn cards

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modify | Add an `if (courses.isEmpty())` branch inside `CourseList`; keep the existing `LazyColumn` branch unchanged for populated lists. Add an empty-list preview. |

## Interfaces / Contracts

No public API, DTO, repository, backend, SQLDelight, or shared-module contract changes.

UI contract for `CourseList`:

- Input: `List<Course>`
- Empty input: show the title plus a centered text message: `No courses available yet.`
- Non-empty input: show the title plus existing spaced `LazyColumn` of `CourseCard` rows.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | ViewModel state behavior | No new unit test required; `CourseUiState.Success(emptyList())` remains valid and no ViewModel logic changes. |
| UI/Preview | Empty course list visual state | Add `@Preview` rendering `CourseList(emptyList())`; inspect that no cards are shown and the empty message is centered in the content area. |
| Build | KMP common UI compile | Run the smallest relevant compose app verification, preferably `:composeApp:jvmTest` or a compose app compile task available in the environment. |

## Migration / Rollout

No migration required. Rollout is a localized UI rendering change in `composeApp`.

## Open Questions

None.
