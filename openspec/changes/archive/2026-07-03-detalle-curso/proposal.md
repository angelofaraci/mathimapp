# Proposal: Course Detail Screen with Enrollment

## Intent

The ACTIVITIES tab currently shows a course catalog with a no-op "Inscribirse" button. Learners cannot view course contents, see lesson progress, or enroll. This change turns the catalog into a real course-detail flow with enrollment and lesson visibility.

## Scope

### In Scope
- `ActivitiesTabRouter` local to ACTIVITIES tab (catalog → detail)
- `CourseDetailScreen` with header, progress bar, and ordered lesson list
- `CourseDetailViewModel` fetching course + user progress
- `POST /courses/{id}/enroll` backend endpoint for official courses
- `exerciseCount` added to shared `Lesson` model
- Lesson status indicators (completed checkmark vs arrow) with no locking in V1
- Session hydration: restore current-user info alongside token on app restart

### Out of Scope
- Lesson locking / sequential unlocking
- Navigating into an individual lesson screen on lesson tap
- Student count aggregation in header
- Deep-linking to detail from HOME tab

## Capabilities

### New Capabilities
- `course-detail-screen`: Local tab router, screen, viewmodel, and data fetching for course detail
- `course-enrollment`: Backend endpoint and client repository method for enrolling in official courses
- `session-hydration`: Recover current-user information on session restore so screens like course detail/home/profile load correctly without forcing re-login

### Modified Capabilities
- `frontend-auth`: Session restoration flow must hydrate the current user alongside the token (previously: token-only restoration)

## Approach

1. **Navigation**: Add a small sealed-class `ActivitiesTabRouter` inside the ACTIVITIES tab scope (`Catalog` / `Detail(courseId)`). Keeps the bottom bar visible and avoids touching the global scaffold.
2. **Data**: Add `exerciseCount: Int = 0` to `Lesson` in `shared`. Compute it in `CourseService.getCourseById()` via `COUNT(exercises)`.
3. **Enrollment**: Create `POST /courses/{id}/enroll` restricted to authenticated users and official/accessible courses.
4. **UI**: `CourseDetailScreen` shows course metadata, derived progress (`completedLessonIds / totalLessons`), and a lesson list. Tapping a lesson does nothing in V1.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/ui/catalog/` | Modified | Catalog becomes router-driven; add `CourseDetailScreen` |
| `composeApp/ui/AuthenticatedHomeScaffold.kt` | Modified | ACTIVITIES tab hosts local router |
| `composeApp/di/AppModule.kt` | Modified | Register `CourseDetailViewModel` |
| `composeApp/domain/auth/` | Modified | Session restore flow hydrates current user alongside token |
| `shared/models/Models.kt` | Modified | Add `exerciseCount` to `Lesson` |
| `server/routes/courseRoutes.kt` | Modified | Add `POST /courses/{id}/enroll` |
| `server/service/CourseService.kt` | Modified | Add enrollment logic; count exercises in course response |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| New local router pattern is unfamiliar | Med | Document the pattern in code comments; future tabs can copy it |
| Cross-module change spans 3 modules | Med | Define shared contract first, then backend, then client |
| Enrollment endpoint missed in slice | Low | Include backend route + service + minimal test in this slice |
| Real metrics look sparse in header | Low | Accept minimal V1 header; defer student count and duration |

## Rollback Plan

1. Remove `ActivitiesTabRouter` and `CourseDetailScreen`.
2. Revert ACTIVITIES tab to render `CourseCatalogScreen` directly.
3. Backend endpoint is additive and safe to leave; disable route registration if needed.
4. `exerciseCount` field has a default value and is backward-compatible.

## Dependencies

- `course-catalog-discovery` must be in place (already implemented).
- `lesson-read-access` and `lesson-progress-derivation` are already implemented; they inform the progress bar logic.

## Success Criteria

- [ ] Tapping a course card in the catalog navigates to the detail screen.
- [ ] Detail screen shows course metadata and an ordered lesson list.
- [ ] "Inscribirse" button calls the enrollment endpoint and updates local state.
- [ ] Each lesson shows a completion checkmark if its ID is in `completedLessonIds`.
- [ ] No lesson locking in V1; all lessons are visible.

## Proposal Question Round

The following assumptions need validation before moving to specs:

1. **Post-enrollment CTA**: After enrolling, should the main button change to "Continuar" or remain "Inscribirse"?
2. **Already-enrolled state**: If the user is already enrolled when opening the detail, should the button show "Ya inscripto" or similar?
3. **Lesson tap in V1**: Should tapping a lesson show a placeholder / toast, or be completely inert?
4. **Exercise progress per lesson**: Should lesson cards show "X/Y ejercicios" or only a binary completed/available status?

Please confirm, correct, or expand these assumptions.
