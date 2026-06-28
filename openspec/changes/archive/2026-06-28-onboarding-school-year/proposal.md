# Proposal: Onboarding School Year

## Why

After registration, users currently land directly on `CourseScreen` with no context about their school level. The app needs a mandatory onboarding flow that captures the learner's province, school year, and onboarding category so it can recommend the appropriate curriculum path via the existing `GET /courses/official?schoolYear={n}` endpoint.

## What Changes

### New Capabilities

#### onboarding-flow
A mandatory multi-step onboarding sequence shown after registration (and on first launch) before the user can access `CourseScreen`. The flow collects:
1. **Province** — user selects their Argentine province first.
2. **School Year** — available year validation adapts to the selected province's school structure: 6-year-primary provinces use a 6+6 base split, 7-year-primary provinces use a 7+5 base split, and `Technical Secondary` adds one extra upper year.
3. **Onboarding Category** — exactly one of: `Primary`, `Secondary`, `Technical Secondary`, `Self-directed`.

After completing all steps, the system uses the selected `schoolYear` to navigate to the course recommendation screen. `Technical Secondary` does not imply different content in this slice; it only extends valid year availability by one year beyond the non-technical secondary range. Mastery/diagnostic questions are deferred.

#### learner-profile
A local SQLDelight table that persists the onboarding outcome (`schoolYear`, `studentTrack`, `province`, `onboardingComplete`) so the app gate survives recomposition and app restart within the current device.

### Modified Capabilities

#### frontend-auth
The auth entry flow changes: after successful registration, the system navigates to the onboarding screen instead of directly to `CourseScreen`. The `AuthGateRouter` gains an `ONBOARDING` state between `REGISTER` and `COURSE`.

## Capabilities → Specs Mapping

| Capability | Spec File | Type |
|---|---|---|
| `onboarding-flow` | `openspec/changes/onboarding-school-year/specs/onboarding-flow/spec.md` | New |
| `learner-profile` | `openspec/changes/onboarding-school-year/specs/learner-profile/spec.md` | New |
| `frontend-auth` | `openspec/changes/onboarding-school-year/specs/frontend-auth/spec.md` | Delta (MODIFIED) |

## Affected Modules

| Module | Scope |
|---|---|
| `composeApp` | Onboarding screens, `AuthGateRouter` states, SQLDelight schema, Koin wiring, `commonTest` |
| `shared` | No changes — profile stays local-only for this slice |
| `server` | No changes — backend sync deferred |

## Rollback Plan

1. Revert `App.kt` and `AuthGateRouter.kt` to route directly from auth to `CourseScreen`.
2. Remove the onboarding SQLDelight table from `AppDatabase.sq` (safe — new table, no migration).
3. Delete onboarding UI screens and ViewModel.
4. No backend migration to revert — this slice is composeApp-only.

## Out of Scope

- Backend user profile persistence (follow-up slice).
- Token persistence across app restarts (pre-existing issue).
- Mastery/level diagnostic questions (future slice).
