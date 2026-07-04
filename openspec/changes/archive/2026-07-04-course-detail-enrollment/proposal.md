# Proposal: Course Detail with Enrollment

## Intent

The course catalog currently shows a visual-only "Inscribirse" button that does not complete the enrollment flow. We will add a course detail screen reachable by tapping any catalog card, and wire the enrollment CTA to the existing backend join endpoint so the flow actually completes.

## Scope

### In Scope
- `CourseDetailScreen` with metadata, description, and CTA.
- Navigation from `CourseCatalogScreen` card tap to detail (local tab-level state machine).
- CTA state: "Continue" if already enrolled, "Enroll" if not.
- Enrollment action via existing `POST /courses/join` (reuses `CourseRepository.joinCourseByCode`).
- Simple error surface (snackbar / inline text) on enrollment failure.
- Update catalog card to remove the visual-only button or repurpose it as a detail link.

### Out of Scope
- Lesson or theory content access.
- Progress tracking updates beyond existing `UserProgress` sync.
- Backend changes (new endpoints, schema changes).
- Cross-tab deep linking.

## Capabilities

### New Capabilities
- `course-detail-enrollment`: Detail screen UI, ViewModel, and enrollment action orchestration.

### Modified Capabilities
- `course-catalog-discovery`: Card tap now navigates to detail. The visual-only enrollment button requirement is superseded by the detail-screen CTA.

## Approach

Add a `CourseDetailScreen` and `CourseDetailViewModel` inside `composeApp/src/commonMain/.../ui/catalog/`.  
The `CourseCatalogScreen` will manage a local `selectedCourseId` state; when non-null it renders the detail screen instead of the list.  
The ViewModel loads the course via `CourseRepository.getCourseById` and checks enrollment via `UserRepository.getUserProgress.enrolledCourseIds`.  
If not enrolled and the course has a `joinCode`, tapping "Enroll" calls `CourseRepository.joinCourseByCode`; on success the UI refreshes progress and switches the CTA to "Continue".  
Errors are surfaced as a simple string in the UI state (no complex form-level validation).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/.../ui/catalog/CourseCatalogScreen.kt` | Modified | Card tap navigates to detail; visual-only button removed or redirected. |
| `composeApp/.../ui/catalog/CourseDetailScreen.kt` | New | Detail UI with metadata and CTA. |
| `composeApp/.../ui/catalog/CourseDetailViewModel.kt` | New | State management and enrollment call. |
| `composeApp/.../domain/CourseRepository` | None | Existing methods reused. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Tab-level navigation has no back stack | Low | Use local `selectedCourseId` state with a back button that clears it. |
| Backend only supports join-by-code | Med | For courses without a `joinCode`, show "Start" CTA and defer direct-enroll endpoint. |
| Enrollment state desync after app restart | Low | `UserProgress` is fetched from server on screen load; local DB is fallback. |

## Rollback Plan

Delete the two new screen/ViewModel files and revert the card tap behavior in `CourseCatalogScreen.kt` to the previous no-op. No backend or schema changes are required.

## Dependencies

- Existing `CourseRepository` and `UserRepository` interfaces in `composeApp`.
- Existing `POST /courses/join` backend endpoint.

## Success Criteria

- [ ] Tapping a course card opens the detail screen.
- [ ] Enrolled users see a "Continue" CTA.
- [ ] Unenrolled users see an "Enroll" CTA that calls the backend and updates state on success.
- [ ] Enrollment failures show a simple error message without crashing the screen.
- [ ] `composeApp:jvmTest` passes after changes.
