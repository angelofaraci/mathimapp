# Design: Onboarding School Year

## Technical Approach

Implement this slice entirely in `composeApp`. Code evidence shows the current gate is local to `App.kt` + `AuthGateRouter`, `CourseViewModel` still calls `getOfficialCourses()` without a year, and SQLDelight state lives in `composeApp`. The change adds a local onboarding flow that collects province, `schoolYear`, and `StudentTrack`, persists one profile row, and routes authenticated users through `ONBOARDING` before `COURSE`. Province metadata resolves the year bands: 6-year-primary provinces use `Primary 1-6`, `Secondary 7-12`, `Technical Secondary 7-13`; 7-year-primary provinces use `Primary 1-7`, `Secondary 8-12`, `Technical Secondary 8-13`. Content selection remains numeric-year based.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Persistence scope | Keep onboarding profile local to `composeApp` | Extend `shared` DTOs or add server persistence now | Current code keeps auth gating and SQLDelight storage inside `composeApp`, and the slice explicitly defers backend sync. |
| Technical naming | Use `StudentTrack` in code/storage instead of `LearnerType` | Keep `LearnerType` | `UserRole.STUDENT` already exists; `StudentTrack` better describes onboarding classification and avoids another overloaded “type” term. |
| Province catalog | Encode the 24-jurisdiction catalog plus boundary metadata in immutable Kotlin data | JSON resource or remote config | Existing app patterns are code-first, and this mapping is static, local, and easily unit-tested. |
| Track semantics | Validate `StudentTrack` against province-derived year bands; `Technical Secondary` unlocks only the extra top year | Split course queries by track or persist a different content key | `CourseRepository.getOfficialCourses(schoolYear)` only accepts a number today, so track-specific content would be false precision in this slice. |

## Data Flow

```text
User -> App.kt/AuthGate: session observed
App.kt/AuthGate -> LearnerProfileRepository: isOnboardingComplete()
LearnerProfileRepository -> App.kt/AuthGate: true/false
App.kt/AuthGate -> AuthGateRouter: resolveAuthView(session, onboardingComplete, target)

authenticated + incomplete -> OnboardingScreen
OnboardingScreen -> OnboardingViewModel: select province/year/track
OnboardingViewModel -> ProvinceSchoolCatalog: province boundary + year validation rules
OnboardingViewModel -> LearnerProfileRepository: upsertProfile(province, schoolYear, studentTrack)
OnboardingViewModel -> CourseViewModel: continue
CourseViewModel -> LearnerProfileRepository: getProfile()
CourseViewModel -> CourseRepository: getOfficialCourses(profile.schoolYear)
```

## File Changes

| File | Action | Description |
|---|---|---|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modify | Gate authenticated users through onboarding before `CourseScreen`; inject learner-profile lookup. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthGateRouter.kt` | Modify | Add `AuthView.ONBOARDING` and make resolution depend on onboarding completion. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/OnboardingScreen.kt` | Create | Multi-step Compose UI for province, year, and onboarding category. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/OnboardingViewModel.kt` | Create | Holds step state, validates progression, and saves the completed profile. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/StudentTrack.kt` | Create | Enum for `PRIMARY`, `SECONDARY`, `TECHNICAL_SECONDARY`, `SELF_DIRECTED`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/LearnerProfileRepository.kt` | Create | Local-only contract for completion check, profile read, and upsert. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/SqlDelightLearnerProfileRepository.kt` | Create | SQLDelight-backed repository using generated `appDatabaseQueries`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/ProvinceSchoolCatalog.kt` | Create | Province list plus year-band metadata for the 6+6, 7+5, and technical-extra-year validation rules. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/CourseViewModel.kt` | Modify | Read stored profile and call `getOfficialCourses(profile.schoolYear)`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modify | Register learner-profile repository, `StudentTrack` adapter, and onboarding ViewModel. |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modify | Add `LearnerProfileEntity` table and single-row queries (`selectProfile`, `upsertProfile`, `clearProfile`). |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/AuthGateRoutingTest.kt` | Modify | Cover authenticated/incomplete -> onboarding and authenticated/complete -> course. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/OnboardingViewModelTest.kt` | Create | Verify step validation, 4-track availability, and completion save. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/SqlDelightLearnerProfileRepositoryTest.kt` | Create | Verify single-row upsert and profile retrieval. |

## Interfaces / Contracts

```kotlin
enum class StudentTrack { PRIMARY, SECONDARY, TECHNICAL_SECONDARY, SELF_DIRECTED }

data class SchoolYearOption(
    val label: String,
    val schoolYear: Int,
    val allowedTracks: Set<StudentTrack>
)

data class LearnerProfile(
    val province: String,
    val schoolYear: Int,
    val studentTrack: StudentTrack,
    val onboardingComplete: Boolean
)

interface LearnerProfileRepository {
    suspend fun getProfile(): LearnerProfile?
    suspend fun isOnboardingComplete(): Boolean
    suspend fun upsertProfile(profile: LearnerProfile)
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Pure auth/onboarding routing | Extend `AuthGateRoutingTest` for `ONBOARDING` cases. |
| Unit | Onboarding state progression, province mapping, and 4-track compatibility | New `OnboardingViewModelTest` with fake repository/catalog. |
| Integration | SQLDelight single-row persistence and read-back | New repository test using existing test driver pattern. |
| Integration | Course filtering uses stored year | Update `CourseViewModel` tests with fake learner-profile repository. |

## Migration / Rollout

No backend migration required. This is a composeApp-only local schema addition; verify SQLDelight generation/build with `./gradlew :composeApp:jvmTest` before apply/verify.

## Open Questions

None.
