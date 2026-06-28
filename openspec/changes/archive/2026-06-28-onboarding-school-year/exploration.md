# Exploration: Onboarding School Year

## Current State

The app currently has a minimal auth gate with no onboarding beyond account creation:

1. **Entry flow**: `App.kt` → `AuthGate()` → `resolveAuthView()` decides between `LOGIN`, `REGISTER`, or `COURSE`.
2. **Registration**: `RegisterScreen` collects `name`, `email`, `password`. On success, `KtorAuthRepository` emits an authenticated `AuthSession` and the UI immediately switches to `CourseScreen`.
3. **No onboarding screens exist**. There is no concept of "learner type", "school year selection", or "onboarding completion" in the current UI or state machines.
4. **User model** (`shared/Models.kt`): `User` only carries `id`, `name`, `email`, `role`. No `schoolYear` or `learnerType` fields.
5. **Server schema** (`server/database/Tables.kt`): `Users` table has `id`, `name`, `email`, `passwordHash`, `role`. No profile-extension columns.
6. **Local DB** (`AppDatabase.sq`): `UserEntity` mirrors the shared `User` shape. No local profile or onboarding-state tables.
7. **Course filtering by school year already works**: `KtorCourseRepository.getOfficialCourses(schoolYear)` exists, the backend supports `GET /courses/official?schoolYear={n}`, and `Course.schoolYear` is serialized and cached in SQLDelight.
8. **No persistent preferences layer**: `TokenStore` is an in-memory interface (`InMemoryTokenStore` on all platforms). There is no DataStore, NSUserDefaults, or similar key-value abstraction for lightweight flags.

## Affected Areas

| File / Module | Why affected |
|---|---|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Must gate between auth and onboarding before showing `CourseScreen`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthGateRouter.kt` | Needs new routing states (e.g. `ONBOARDING`) or a separate onboarding router. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterScreen.kt` + `RegisterViewModel.kt` | After successful registration, must navigate to onboarding instead of directly to courses. |
| `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq` | Needs a local table to store onboarding profile (`schoolYear`, `learnerType`, `onboardingComplete`) so the gate survives recomposition and ideally app restart. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | New onboarding ViewModel and repository need Koin wiring. |
| `composeApp/src/commonTest/...` | New tests for routing logic, ViewModel validation, and repository behavior. |
| `shared/src/commonMain/.../Models.kt` | **Potential** — only if we decide to sync profile fields to the backend in this slice. |
| `server/...` | **Potential** — only if we extend the `Users` table and `UpdateUserRequest` to persist profile server-side. |

## Approaches

### 1. Local-Only Onboarding (ComposeApp-only)

Keep all onboarding state locally in SQLDelight. After registration, show onboarding screens, write `schoolYear` + `learnerType` to a new local table, and use that value when calling `getOfficialCourses(schoolYear)`.

- **Pros**:
  - Stays entirely inside `composeApp` — matches the roadmap’s "Affected modules: `composeApp`".
  - No backend migration, no `shared` changes, no cross-module coordination.
  - Can reuse existing `CourseRepository.getOfficialCourses(Int?)` for the recommendation screen.
  - Fits the ~200–300 line review budget.
- **Cons**:
  - Onboarding profile is device-local and lost on reinstall.
  - Spec language says "profile is saved" — local save satisfies the letter but not the spirit of cross-device sync.
  - Future server-side recommendation logic will require a follow-up migration anyway.
- **Effort**: Low–Medium

### 2. Backend-Synced User Profile (Cross-module)

Extend the `User` model in `shared` with `schoolYear` and `learnerType`, add columns to the server `Users` table, update `UpdateUserRequest`, and call `PUT /users/{id}` during onboarding to persist the profile. The auth session is refreshed with the updated user.

- **Pros**:
  - Profile persists across devices and reinstalls.
  - Aligns cleanly with the spec’s implication that the system "saves" the profile.
  - Enables future server-side recommendation endpoints without another migration.
- **Cons**:
  - Touches `shared`, `server`, and `composeApp`.
  - Requires a Flyway migration for the new `Users` columns.
  - Exceeds the 200–300 line budget; realistically 400–500+ lines.
  - Needs backend tests (`:server:test`) in addition to app tests.
- **Effort**: Medium–High

## Recommendation

**Adopt Approach 1 (Local-Only) for this slice**, with a clear architectural note that Approach 2 should follow as a dedicated "user-profile-sync" slice once the learner track is validated.

Rationale:
- The roadmap explicitly scopes this slice to `composeApp` and targets `:composeApp:jvmTest` verification.
- The existing `getOfficialCourses(schoolYear)` API already provides everything needed for the "recommend initial curriculum path" requirement.
- Keeping the slice small preserves reviewer focus and avoids mixing UI flow work with backend schema changes.
- We can model the local profile table so that a future backend-sync slice only needs to:
  1. Add server columns,
  2. Extend `User`/`UpdateUserRequest` in `shared`,
  3. Replace the local write with an API call.

## Risks

1. **Local data loss**: If the user reinstalls the app, onboarding state disappears. Mitigation: document as known limitation; next slice adds backend sync.
2. **SQLDelight schema change**: Adding a new table is safe, but any mistake in the `.sq` file will break compilation on all platforms. Mitigation: validate with `./gradlew :composeApp:jvmTest` immediately after schema edits.
3. **Auth gate complexity**: `App.kt` currently has a simple ternary. Adding onboarding as a third gate state increases branching logic. Mitigation: keep the routing decision in a pure, testable class (extend `AuthGateRouter` or introduce an `AppRouter`) rather than inline in `@Composable`.
4. **No persistent token store**: Because `TokenStore` is in-memory, the user is already logged out on every restart. Onboarding would re-run each cold start unless we also persist the auth token. This is a **pre-existing issue** unrelated to this slice, but it means onboarding + auth persistence should be addressed together or acknowledged as out of scope.

## Ready for Proposal

**Yes.**

The orchestrator can proceed to `sdd-propose`. The proposal should:
- Define the exact learner-type enum values (e.g. `PRIMARY`, `SECONDARY` or `INDEPENDENT`, `SCHOOL_MANAGED`).
- Define valid school-year values (e.g. `1..6` for Argentina secondary, or `1..12` inclusive of primary).
- Explicitly call out that backend profile persistence is **deferred** to a follow-up slice.
- Include a rollback plan: remove the onboarding table + revert `App.kt` routing.
