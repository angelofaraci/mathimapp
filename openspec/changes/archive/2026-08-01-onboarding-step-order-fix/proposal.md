# Proposal: Fix Onboarding Step Order (Category Before School Year)

## Intent

Onboarding asks Province → School year → Category (`StudentTrack`). This inverts the domain rule: the education level determines which school years are valid (primary 1-6/7, secondary 7/8-12, technical adds year 13), not the reverse. Learners must guess a year before stating their level, then discover their category is disabled — `selectSchoolYear()` gates `trackOptions` by the chosen year (`OnboardingViewModel.kt:68-85`), so a wrong year silently blocks the correct category and forces a back-and-forth. Backlog item #2 (`openspec/backlog.md:68`).

## Scope

### In Scope
- Reorder onboarding to Province → Category → School year → Confirmation (`OnboardingStep`, `nextStep()`, `goBack()` and their reset side effects).
- Add a track-filtered year derivation to `ProvinceSchoolCatalog` (province + track → `List<SchoolYearOption>`); `selectTrack()` populates `availableSchoolYears`, `selectSchoolYear()` no longer rebuilds `trackOptions`.
- After province selection all four categories are enabled (track availability does not vary by province).
- `SELF_DIRECTED` intentionally keeps the full primary+secondary range (1-12) — it is a non-narrowing category, not a bug. Year 13 stays technical-only.
- Reorder `OnboardingScreen.kt` step branches, `hasCurrentStepSelection()`, and move step2/step3 copy so titles/numbers follow the step, not the enum name.
- Update `OnboardingViewModelTest` (8 tests) and `OnboardingScreenTest`.

### Out of Scope
- Moving Province off the first position, adding/removing steps, or restyling onboarding.
- Backlog item #3 (logout button during onboarding) and any other backlog item.
- Course filtering semantics, `LearnerProfile`, persistence, and `completeOnboarding()` validation (order-independent, unchanged).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `onboarding-flow`: "Province-Based School-Year Rules" (year list derived from province + selected track, step now third); "Province Selection Step" (advances to category); "Onboarding Category Classification" (second step, all four options enabled); "Category Semantics" (self-directed range).

## Approach

Keep `SchoolYearOption.allowedTracks` as the single source of truth and filter it: `schoolYearOptionsFor(province).filter { track in it.allowedTracks }`. No new year-band table, so province boundary rules stay in one place and `isValidSelection()` remains consistent with what the UI offers.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `ui/OnboardingViewModel.kt` | Modified | Enum order, `select*`, `nextStep`, `goBack`. |
| `data/ProvinceSchoolCatalog.kt` | Modified | Track-filtered overload. |
| `ui/OnboardingScreen.kt` | Modified | Branch order, step copy mapping. |
| `composeResources/values*/strings.xml` | Modified | Swap step2/step3 copy. |
| `commonTest`, `jvmTest` | Modified | Sequence and assertion updates. |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Step numbering desyncs from copy | Med | Assert rendered title per step in `OnboardingScreenTest`. |
| Stale year survives a back-then-change-track path | Med | Reset `selectedSchoolYear` on `selectTrack()` and on back from SCHOOL_YEAR. |

## Rollback Plan

Revert the single commit. No schema, contract, or persisted-state change; existing profiles are unaffected.

## Dependencies

None.

## Success Criteria

- [ ] Flow renders Province → Category → School year → Confirmation with matching step numbers/copy.
- [ ] Year list shown reflects the selected track (self-directed shows 1-12; technical includes 13; secondary excludes 13).
- [ ] Changing the track after picking a year clears the stale year.
- [ ] `./gradlew :composeApp:jvmTest` passes.
