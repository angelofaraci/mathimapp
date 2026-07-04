# Design: Course Detail with Enrollment

## Technical Approach

Implement the change entirely in `composeApp/commonMain`, keeping the bottom navigation intact and reusing the existing `/courses/{id}` and `/courses/join` client APIs. The ACTIVITIES tab will switch between catalog and detail using local UI state, while a new `CourseDetailViewModel` owns course loading, enrollment eligibility, CTA state, and simple error messaging.

## Architecture Decisions

| Decision | Options | Choice | Rationale |
|---|---|---|---|
| Activities navigation | Global router, new tab router, local selected id | Local `selectedCourseId` in ACTIVITIES scope | Matches the spec, keeps the change isolated, and preserves the existing bottom-bar scaffold. |
| Current user source | `UserRepository.getCurrentUser()`, `AuthRepository.session` | `AuthRepository.session.value.user` | Existing app screens already depend on session user; `getCurrentUser()` currently uses a placeholder id and is not reliable for this flow. |
| CTA resolution | String-only branching in UI, explicit UI enum | Explicit CTA mode in view state (`Continue` / `Enroll` / `Start`) | Keeps business rules out of composables and makes state transitions unit-testable. |
| Enrollment refresh | Optimistic local toggle, refetch progress | Call `joinCourseByCode`, then `getUserProgress` | Reuses the existing source of truth for `enrolledCourseIds` and avoids inventing client-only enrollment state. |

## Data Flow

```text
Course card tap
  -> ACTIVITIES selectedCourseId = course.id
  -> CourseDetailViewModel.load(courseId)
  -> CourseRepository.getCourseById(courseId)
  -> AuthRepository.session.value.user
  -> UserRepository.getUserProgress(user.id)
  -> UI derives CTA

Enroll tap
  -> if joinCode != null and CTA == Enroll
  -> CourseRepository.joinCourseByCode(user.id, joinCode)
  -> UserRepository.getUserProgress(user.id)
  -> CTA becomes Continue
  -> on failure, set simple error string
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` | Modify | Own `rememberSaveable` ACTIVITIES `selectedCourseId` state and switch between catalog/detail views. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseCatalogScreen.kt` | Modify | Make the full card tappable, remove the visual-only button, and emit `onCourseSelected(courseId)`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseDetailScreen.kt` | Create | Render metadata, back action, primary CTA, loading state, and simple error surface. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/catalog/CourseDetailViewModel.kt` | Create | Load course/progress, resolve CTA mode, handle enroll action, expose dismissible error state. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modify | Register `CourseDetailViewModel` in Koin. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/catalog/CourseDetailViewModelTest.kt` | Create | Cover load, CTA derivation, join success refresh, and join failure. |

## Interfaces / Contracts

```kotlin
data class CourseDetailUiState(
    val isLoading: Boolean = true,
    val course: Course? = null,
    val cta: CourseDetailCta = CourseDetailCta.Start,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

enum class CourseDetailCta { Continue, Enroll, Start }
```

`CourseDetailViewModel` depends on `AuthRepository`, `CourseRepository`, and `UserRepository`. `load(courseId)` fetches the course and user progress; `onPrimaryAction()` only calls `joinCourseByCode` when CTA is `Enroll` and `course.joinCode` is present.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | CTA derivation and state transitions | `commonTest` for enrolled, unenrolled, and missing-joinCode cases. |
| Unit | Enrollment success/error | Fake repositories verifying refresh after join and stable screen after failure. |
| Integration | Existing join client behavior | Reuse current `KtorCourseRepositoryTest` coverage for `/courses/join`; no new backend test needed. |
| E2E | Not planned | No Compose E2E harness exists in this repo. |

## Migration / Rollout

No migration required. This is an additive app-side flow that reuses existing backend capabilities.

## Open Questions

- [ ] If the app restores only a token and `session.user` is null after restart, should the detail screen show an auth error or block the CTA until session hydration exists?
