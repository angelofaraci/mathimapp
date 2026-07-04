## Implementation Progress

**Change**: course-detail-enrollment
**Mode**: Standard

### Completed Tasks
- [x] 1.1 Create `CourseDetailViewModel.kt` with `CourseDetailUiState`, `CourseDetailCta` enum, and ViewModel class, depending on `AuthRepository`, `CourseRepository`, `UserRepository`
- [x] 1.2 Implement `load(courseId)` — fetch course via `CourseRepository.getCourseById`, session user via `AuthRepository.session`, and enrolled IDs via `UserRepository.getUserProgress`
- [x] 1.3 Derive CTA: `Continue` (enrolled), `Enroll` (not enrolled + has joinCode), `Start` (not enrolled + no joinCode); handle null `session.user`
- [x] 1.4 Implement `onPrimaryAction()` — call `joinCourseByCode` when CTA is `Enroll`, refresh progress on success, set `errorMessage` on failure
- [x] 2.1 Create `CourseDetailScreen.kt` composable showing title, topic, difficulty, duration, XP, full description, with loading spinner and missing-field handling
- [x] 2.2 Render primary CTA button with text from CTA state, disabled while `isSubmitting`; show dismissible `errorMessage` snackbar/inline text
- [x] 3.1 Modify `CourseCatalogScreen.kt`: add `onCourseSelected: (String) -> Unit` lambda, make `CourseCatalogCard` tappable, remove visual-only `Button("Inscribirse")`
- [x] 3.2 Modify `AuthenticatedHomeScaffold.kt`: own `rememberSaveable selectedCourseId`, switch between `CourseCatalogScreen` and `CourseDetailScreen`; back button clears selection
- [x] 3.3 Register `CourseDetailViewModel` via `viewModelOf(::CourseDetailViewModel)` in `AppModule.kt`
- [x] 4.1 Create `CourseDetailViewModelTest.kt` with fake repositories; test CTA derivation: enrolled → `Continue`, unenrolled → `Enroll`, no joinCode → `Start`
- [x] 4.2 Test enrollment success: CTA flips to `Continue`, progress refresh called, no error set
- [x] 4.3 Test enrollment failure: `errorMessage` populated, CTA remains tappable, `isLoading` resets correctly

### Files Changed
| File | Action | What Was Done |
|------|--------|---------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseDetailViewModel.kt` | Created | Added detail state, CTA derivation, safe null-session handling, and enrollment refresh/error orchestration. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseDetailScreen.kt` | Created | Added the detail UI with metadata, back button, CTA rendering, loading state, and dismissible inline errors. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseCatalogScreen.kt` | Modified | Made the full course card tappable, emitted `onCourseSelected`, and removed the visual-only enrollment button. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` | Modified | Added saveable ACTIVITIES detail navigation state and switched between catalog and detail screens. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modified | Registered `CourseDetailViewModel` in Koin. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/catalog/CourseDetailViewModelTest.kt` | Created | Added targeted tests for CTA derivation, token-only restore safety, enrollment success, and enrollment failure. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ComposeAppCommonTest.kt` | Modified | Extended the DI smoke test to resolve `CourseDetailViewModel`. |
| `openspec/changes/course-detail-enrollment/tasks.md` | Modified | Marked every planned task complete after implementation. |
| `openspec/changes/course-detail-enrollment/apply-progress.md` | Created | Recorded the cumulative apply status, verification, and review boundary for this single work unit. |

### Verification
| Command | Result |
|---------|--------|
| `./gradlew :composeApp:jvmTest --tests "com.example.proyectofinal.ui.catalog.CourseDetailViewModelTest" --tests "com.example.proyectofinal.AppModuleTest"` | Failed initially because `CourseCatalogScreen.kt` still referenced `Button` for the retry state after removing the card CTA import, and the new success test expected only one progress fetch even though load + post-join refresh correctly call `getUserProgress` twice. |
| `./gradlew :composeApp:jvmTest --tests "com.example.proyectofinal.ui.catalog.CourseDetailViewModelTest" --tests "com.example.proyectofinal.AppModuleTest"` | Passed after restoring the missing Material3 `Button` import and fixing the test expectation (`BUILD SUCCESSFUL`). |

### Deviations from Design
None — implementation matches design.

### Issues Found
- The first compile run failed because `CourseCatalogScreen` still used a retry `Button` after the catalog card CTA removal refactor dropped the import.
- The first enrollment-success assertion expected a single `getUserProgress` call, but the design correctly requires one lookup during load and another after a successful join refresh.

### Remaining Tasks
- None — all tasks for this change are complete.

### Workload / PR Boundary
- Mode: single PR
- Current work unit: Full detail + enrollment feature
- Boundary: starts from the existing ACTIVITIES course catalog and ends with tappable catalog cards, local detail navigation, the new detail screen/view model, DI wiring, and focused JVM verification for detail behavior.
- Estimated review budget impact: Low risk and within the approved 400-line review budget for a single reviewable unit.

### Status
12/12 tasks complete. Ready for verify.
