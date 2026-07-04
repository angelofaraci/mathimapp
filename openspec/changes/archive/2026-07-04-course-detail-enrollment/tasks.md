# Tasks: Course Detail with Enrollment

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~250-350 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Full detail + enrollment feature | PR 1 | single PR, within 400-line budget |

## Phase 1: Foundation — ViewModel & State

- [x] 1.1 Create `CourseDetailViewModel.kt` with `CourseDetailUiState`, `CourseDetailCta` enum, and ViewModel class, depending on `AuthRepository`, `CourseRepository`, `UserRepository`
- [x] 1.2 Implement `load(courseId)` — fetch course via `CourseRepository.getCourseById`, session user via `AuthRepository.session`, and enrolled IDs via `UserRepository.getUserProgress`
- [x] 1.3 Derive CTA: `Continue` (enrolled), `Enroll` (not enrolled + has joinCode), `Start` (not enrolled + no joinCode); handle null `session.user`
- [x] 1.4 Implement `onPrimaryAction()` — call `joinCourseByCode` when CTA is `Enroll`, refresh progress on success, set `errorMessage` on failure

## Phase 2: Core UI — Detail Screen

- [x] 2.1 Create `CourseDetailScreen.kt` composable showing title, topic, difficulty, duration, XP, full description, with loading spinner and missing-field handling
- [x] 2.2 Render primary CTA button with text from CTA state, disabled while `isSubmitting`; show dismissible `errorMessage` snackbar/inline text

## Phase 3: Integration — Navigation & DI

- [x] 3.1 Modify `CourseCatalogScreen.kt`: add `onCourseSelected: (String) -> Unit` lambda, make `CourseCatalogCard` tappable, remove visual-only `Button("Inscribirse")`
- [x] 3.2 Modify `AuthenticatedHomeScaffold.kt`: own `rememberSaveable selectedCourseId`, switch between `CourseCatalogScreen` and `CourseDetailScreen`; back button clears selection
- [x] 3.3 Register `CourseDetailViewModel` via `viewModelOf(::CourseDetailViewModel)` in `AppModule.kt`

## Phase 4: Testing — ViewModel Unit Tests

- [x] 4.1 Create `CourseDetailViewModelTest.kt` with fake repositories; test CTA derivation: enrolled → `Continue`, unenrolled → `Enroll`, no joinCode → `Start`
- [x] 4.2 Test enrollment success: CTA flips to `Continue`, progress refresh called, no error set
- [x] 4.3 Test enrollment failure: `errorMessage` populated, CTA remains tappable, `isLoading` resets correctly
