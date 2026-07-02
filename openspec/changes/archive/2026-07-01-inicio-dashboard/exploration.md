## Exploration: inicio-dashboard

### Current State

The app already has a 4-tab scaffold (`AuthenticatedHomeScaffold`) with bottom navigation matching the reference image: **Inicio / Actividades / Progreso / Perfil**.

However, the **HOME** tab currently renders `CourseScreen` (from `App.kt`), which is a raw course list with a "Welcome to MathApp!" heading and a Logout button. It does NOT act as an orientation/progress-summary dashboard. The `CourseScreen` and its `CourseViewModel` are essentially a legacy placeholder that was wired as the authenticated landing view.

Existing features that can feed a dashboard:
- **Profile data**: `ProfileViewModel` already fetches `User`, `UserProgress`, and `LearnerProfile`. It computes level, XP, streak, completed lessons, and achievements.
- **Catalog**: `CourseCatalogScreen` + `CourseCatalogViewModel` fetch official courses and support search/filter.
- **Progress tracking**: `UserProgress` model exists with `completedLessonIds`, `completedExerciseIds`, `totalScore`, `enrolledCourseIds`. The local DB syncs this.
- **Streak**: Currently a synthetic value (`min(completedLessons, 7)`); no date-based streak tracking exists yet.
- **Enrollment**: `enrolledCourseIds` exists but there is no "last accessed / in-progress lesson" concept yet.

### Affected Areas

- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` — HOME tab target changes from `CourseScreen` to the new dashboard.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` — `CourseScreen` is defined here; it may be removed or repurposed. Previews tied to it should move or be deleted.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/` — New `HomeDashboardScreen.kt` and `HomeDashboardViewModel.kt` to be added.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` — New ViewModel needs Koin registration.
- `composeApp/src/commonMain/composeResources/drawable/` — Empty-state illustration asset likely needed (currently only tab icons and an achievement placeholder exist).

### Approaches

1. **Replace `CourseScreen` with a dedicated `HomeDashboardScreen`**
   - Create `HomeDashboardScreen.kt` + `HomeDashboardViewModel.kt` following the existing screen/VM pattern.
   - Wire `AuthenticatedHomeScaffold` HOME tab to the new screen.
   - Use existing repositories (`UserRepository`, `LearnerProfileRepository`, `CourseRepository`) to populate greeting, streak chip, empty state, and recommendations.
   - CTAs navigate to the ACTIVITIES tab by calling `MainRouter.showActivities()`.
   - Pros:
     - Clean separation of concerns; each tab has its own focused screen.
     - Follows the existing architecture pattern used by Profile and Catalog.
     - Easy to extend later with populated states (enrolled courses, resume lesson).
   - Cons:
     - Requires creating 2–3 new files and a small scaffold change.
   - Effort: Low

2. **Refactor `CourseScreen` in place into a dashboard**
   - Keep `CourseScreen` name but redesign its UI to match the reference.
   - Reuse `CourseViewModel` or extend it.
   - Pros:
     - Fewer file moves.
   - Cons:
     - The name `CourseScreen` becomes misleading and confusing.
     - `CourseViewModel` is currently focused on loading `List<Course>`; extending it for dashboard state makes it a god object.
     - Harder to maintain as the dashboard grows (progress, recommendations, announcements).
   - Effort: Low-Medium

3. **Add a dashboard wrapper that keeps the course list below it**
   - Build a dashboard header on top and still show `CourseList` underneath.
   - Pros:
     - Reuses existing course list UI.
   - Cons:
     - Does NOT match the reference design, which is centered on an empty-state card + recommendations, not a scrollable list of all courses.
     - Creates a fragmented experience (dashboard summary + raw catalog in the same tab).
   - Effort: Medium

### Recommendation

**Approach 1: Replace with `HomeDashboardScreen`.**

Rationale: The HOME tab should have a single, coherent purpose — orientation + progress summary + prompting action. The current `CourseScreen` is a generic list that belongs more naturally in the ACTIVITIES tab (where `CourseCatalogScreen` already lives). Creating a dedicated screen/VM keeps the architecture consistent and makes the intent obvious.

### Risks

- **Streak data is synthetic**: The "0 días" chip cannot show a real consecutive-days streak because the backend/local DB does not store activity dates. The dashboard should use the existing capped-lesson-count streak (`min(completedLessons, 7)`) or simply show a neutral greeting chip.
- **No real "continue learning" entity**: Because there is no "last accessed lesson / in-progress course" tracking, the "Continuar aprendiendo" section should render the empty-state card as shown in the reference. When that tracking is added later, the same section can be extended to show a resume card.
- **Illustration asset needed**: The empty-state card requires an illustration. If the team does not have one ready, a placeholder drawable or an icon-based empty state should be used so the screen is not blocked on design assets.

### Ready for Proposal

Yes. The orchestrator can tell the user:

> The exploration confirms the app already has the correct 4-tab scaffold and the data sources needed for a dashboard. The recommended next step is to replace the legacy `CourseScreen` on the HOME tab with a new `HomeDashboardScreen` that shows: a personalized greeting + a status chip, a "Continuar aprendiendo" section with an empty-state card (since in-progress tracking is not yet implemented), and a lightweight "Actividades recomendadas" card that links to the catalog. This is a small, well-scoped change that follows existing patterns and does not force features that do not yet exist.
