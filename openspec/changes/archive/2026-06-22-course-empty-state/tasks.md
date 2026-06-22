# Tasks: Course List Empty State

## Review Workload Forecast

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

| Field | Value |
|-------|-------|
| Estimated changed lines | ~20 additions, 0 deletions |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Delivery strategy | auto-chain |
| Suggested split | Single PR (no split needed) |

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Add empty-state UI + preview in `App.kt` | PR 1 | Single self-contained change; base = main |

## Phase 1: Core Implementation

- [x] 1.1 Inside `CourseList(courses)` in `App.kt`, add an `if (courses.isEmpty())` branch that renders a centered `Text("No courses available yet.", ...)` inside a `Box(Modifier.fillMaxSize(), Alignment.Center)` — replace the `LazyColumn` branch when the list is empty
- [x] 1.2 Keep the existing `LazyColumn` branch unchanged in an `else` block so non-empty lists render identically to before
- [x] 1.3 Ensure the "Welcome to MathApp!" title renders above both branches (empty and non-empty)

## Phase 2: Preview & Verification

- [x] 2.1 Add a `@Preview` composable `CourseListEmptyPreview` in `App.kt` that renders `CourseList(emptyList())` inside `MaterialTheme`
- [x] 2.2 Run `:composeApp:jvmTest` to confirm compilation and that existing tests still pass
- [x] 2.3 Verify no new icon dependencies are introduced — empty state uses only existing Compose Material3/layout primitives (`Box`, `Text`, `Alignment`, `Modifier.fillMaxSize`)
