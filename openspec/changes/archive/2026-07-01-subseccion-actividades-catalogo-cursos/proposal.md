# Proposal: Course Catalog in Activities Tab

## Intent

Replace the ACTIVITIES placeholder with a discoverable course catalog matching the reference UI. Add discovery metadata to the shared `Course` contract so cards can display topic, difficulty, duration, and XP. Keep the enrollment action visual-only in v1; the actual enrollment backend will come in a follow-up change.

## Scope

### In Scope
- Add `topic`, `difficulty`, `durationMinutes`, `xpReward` to `shared` `Course` and backend `Courses` table.
- Flyway migration for new columns.
- Update official courses endpoint to return new fields.
- Update SQLDelight schema and queries.
- Build `CourseCatalogScreen` with search bar, horizontal topic chips (Fracciones, Álgebra, Geometría), and course cards.
- Wire `MainTab.ACTIVITIES` to `CourseCatalogScreen`.
- Visual-only "Inscribirse" button on cards.

### Out of Scope
- Backend enrollment flow or `POST /courses/{id}/enroll`.
- Repurposing HOME to enrolled courses (depends on future enrollment implementation).
- Gamification or reward logic beyond displaying XP on cards.

## Capabilities

### New Capabilities
- `course-catalog-discovery`: Discover screen with search, topic filters, and rich course cards.

### Modified Capabilities
- `school-year-filtering`: Official course list now includes discovery metadata.
- `client-server-contract`: `Course` contract gains new display fields.

## Approach

Extend the canonical `Course` model with discovery fields rather than introducing a parallel DTO. Update backend, shared, and SQLDelight schemas in one slice, then build the Compose UI on top. Use client-side filtering for search and chips against the official course list.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/src/commonMain/.../models/Models.kt` | Modified | Add discovery fields to `Course`. |
| `server/src/main/kotlin/.../database/Tables.kt` | Modified | Add columns to `Courses`. |
| `server/src/main/kotlin/.../service/CourseService.kt` | Modified | Populate and return new fields. |
| `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq` | Modified | Update `CourseEntity` and queries. |
| `composeApp/src/commonMain/.../ui/AuthenticatedHomeScaffold.kt` | Modified | Swap ACTIVITIES placeholder for catalog screen. |
| `composeApp/src/commonMain/.../ui/catalog/` | New | `CourseCatalogScreen`, `CourseCatalogViewModel`, card composables. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Diff exceeds 400 lines | High | Chain PRs: backend+shared first, then app UI. |
| HOME/ACTIVITIES ownership confusion | Med | Document in code comments that HOME is reserved for enrolled courses. |

## Rollback Plan

Revert the PRs. The Flyway migration is additive-only (new nullable columns), so rollback requires no schema reversal. If needed, hide the catalog screen by restoring `PlaceholderScreen` in `AuthenticatedHomeScaffold`.

## Dependencies

- Future change: backend enrollment endpoint to make "Inscribirse" functional and populate HOME with enrolled courses.

## Success Criteria

- [ ] ACTIVITIES tab renders the catalog with search and topic chips.
- [ ] Course cards display real topic, difficulty, duration, and XP from the backend.
- [ ] "Inscribirse" button is present but does not call an enrollment endpoint in v1.
- [ ] Project builds and existing tests pass.
