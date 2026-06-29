# Learning Paths Future-Feature Brief

`learning-paths` is the future default learner experience: a platform-curated progression layer that organizes existing lessons into guided, school-year-based paths without changing the underlying lesson/exercise completion model.

## Purpose

- Preserve the product decisions already made for `learning-paths` v1.
- Give the team a single scannable reference before any formal OpenSpec change is created.
- Clarify the boundary between platform learning paths and teacher-managed course features.

## Product Boundaries

### In scope for v1

- Platform-curated learning paths only.
- One default platform path per school year.
- School year is the primary organizational axis.
- Learning paths become the primary default learner experience.
- Learners can still browse and open other paths with low friction.

### Out of scope for v1

- Teacher-created private courses as part of the learning-paths model.
- Path authoring tools.
- Special migration flows when a learner switches paths.
- 100% completion rewards inside this slice.

### Domain separation

`learning-paths` and teacher-created private courses MUST remain distinct concepts. In v1, learning paths are platform-owned curriculum guidance; teacher courses remain a separate classroom/product track.

## V1 Behavior

### Assignment and recommendation

- During onboarding, the app auto-assigns or recommends the default path for the learner's selected school year.
- This onboarding behavior happens with no extra confirmation step.
- If the learner changes school year later, the product recalculates the recommended default path.
- A later school-year change MUST ask for confirmation before switching the learner away from the current path.

### Path access and switching

- The app remembers the last opened path.
- Learners MAY manually switch paths at any time.
- Access to other paths SHOULD stay lightweight and discoverable.
- Switching paths MUST NOT require any migration workflow.

### Progression model

- Progression is linear lesson-by-lesson.
- Future lessons are visible from the start and are not hard-locked.
- Learners can view the full ordered lesson list immediately.
- The next recommended lesson SHOULD be visually emphasized, even if the UI does not use explicit text such as "Recommended next lesson".
- Completed lessons remain open for review.

### Progress rules

- Path progress is percentage-based.
- Progress is derived from completed lessons.
- Lesson completion still derives from exercise completion.
- A lesson MAY belong to multiple paths.
- Completed lessons MUST reuse progress across every path that includes that lesson.
- Switching paths therefore does not require special reconciliation or migration logic.

### Path entry behavior

- Opening a path first shows a summary screen.
- Each path summary includes a primary CTA such as `Start`.
- That CTA opens the path view focused on the first incomplete lesson.
- The CTA MUST NOT deep-link directly into lesson content.

## Data / Model Implications

### Path metadata

Each learning path needs:

- `name`
- `description`
- `visibleObjective`
- `structuredObjective`

### Structured objective rules

- V1 supports only one structured objective type: `grade-level`.
- The structured objective exists to support consistent labeling and future filtering/grouping.

### Relationships and state

| Area | Implication |
|------|-------------|
| Path → lessons | Ordered one-to-many path composition, with lesson reuse across paths. |
| Lesson reuse | Path membership cannot own lesson completion; completion remains lesson-based. |
| Learner state | Product needs enough state to know the recommended path, current/selected path, and last opened path. |
| Progress | Path percentage is a derived view over completed lessons, not a separate source of truth. |
| Switching | No dedicated migration model is needed when moving between paths. |

### Architectural implication

This feature should extend the existing lesson/exercise progress system rather than replace it. The path layer is guidance and aggregation, not a second independent completion engine.

## UX Flow

### Onboarding

1. Learner selects school year.
2. Product resolves the default platform path for that year.
3. Product auto-assigns or recommends that path without an extra confirmation step.

### Returning learner

1. Learner reopens the app.
2. Product restores the last opened path as the easiest re-entry point.
3. UI emphasizes the next incomplete lesson inside that path.

### Opening a path

1. Learner opens a path.
2. Product shows the path summary first.
3. Learner taps the primary CTA.
4. Product opens the ordered path view positioned at the first incomplete lesson.

### Changing school year later

1. Learner updates school year.
2. Product recalculates the recommended default path.
3. Product asks for confirmation before switching away from the current path.

### Manual switching

1. Learner opens another path.
2. Shared completed lessons already count there if applicable.
3. No special migration or conversion step is shown.

## Explicit Non-Goals / Deferred Decisions

- No teacher-authored private paths in v1.
- No path hard-locking beyond soft progression guidance.
- No separate path-completion mechanic independent from lesson/exercise completion.
- No special switching migration flow.
- No reward design for 100% path completion here; that belongs to `gamification-rewards`.
- No expansion beyond the `grade-level` structured objective type in v1.
- No authoring workflow, analytics, or advanced curation tooling defined here yet.

## Open Questions To Resolve When Promoted

- How many non-default paths should be surfaced prominently versus tucked behind browse/discover UI?
- Where should path switching live in navigation so it stays low-friction without diluting the default experience?
- What exact learner state should persist locally versus server-side for current path, recommended path, and last opened path?
- How should platform curation operate operationally if content for a school year changes after learners already started a path?

## Status

This document is a future-feature brief only. It is intentionally not an implementation plan, not a formal change proposal, and not a replacement for a later OpenSpec change folder.
