# Tasks: Fix Onboarding Step Order (Category Before School Year)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~180-260 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Full reorder (catalog + view model + screen + strings + tests) | PR 1 | Single contained bug fix; splitting would break intermediate compilation |

## Phase 1: Catalog — Track-Filtered School Years

- [x] 1.1 In `ProvinceSchoolCatalog.kt`, add `schoolYearOptionsFor(province: String, track: StudentTrack): List<SchoolYearOption>` that filters `schoolYearOptionsFor(province)` by `track in it.allowedTracks` (spec: Province-Based School-Year Rules).
- [x] 1.2 Verify `SELF_DIRECTED` filtering naturally yields years 1-12 unfiltered by primary/secondary boundary and excludes 13, using existing `allowedTracks` sets — no new year-band table (spec scenario: "Self-directed shows the full unfiltered year range").

## Phase 2: ViewModel — Step Order and Reset Semantics

- [x] 2.1 In `OnboardingViewModel.kt:16-21`, reorder `OnboardingStep` enum to `PROVINCE, CATEGORY, SCHOOL_YEAR, CONFIRMATION`.
- [x] 2.2 Rewrite `selectProvince()` (`:51-66`) to populate `trackOptions` with all four enabled (`buildTrackOptions` with all `StudentTrack.entries`) instead of `defaultTrackOptions()` (all-disabled); clear `selectedTrack`/`selectedSchoolYear`/`availableSchoolYears` (spec: "All categories are enabled regardless of selected province").
- [x] 2.3 Rewrite `selectTrack()` (`:87-104`) to no longer read from `trackOptions.enabled` gating; instead validate track is one of `StudentTrack.entries`, set `selectedTrack`, populate `availableSchoolYears = ProvinceSchoolCatalog.schoolYearOptionsFor(province, track)`, and clear `selectedSchoolYear`.
- [x] 2.4 Rewrite `selectSchoolYear()` (`:68-85`) to validate against `state.availableSchoolYears` only (no `trackOptions` rebuild); set `selectedSchoolYear`.
- [x] 2.5 Reorder `nextStep()` (`:106-152`): `PROVINCE` validates `selectedProvince != null` and advances to `CATEGORY`; `CATEGORY` validates `selectedTrack != null` and advances to `SCHOOL_YEAR`; `SCHOOL_YEAR` validates `selectedSchoolYear` is in `availableSchoolYears` and advances to `CONFIRMATION`.
- [x] 2.6 Reorder `goBack()` (`:154-174`) per spec "Onboarding Back-Navigation Reset Semantics": from `CATEGORY` → clear `selectedTrack`, return to `PROVINCE`; from `SCHOOL_YEAR` → clear `selectedSchoolYear`, return to `CATEGORY` (keep `selectedTrack`); from `CONFIRMATION` → return to `SCHOOL_YEAR` (no clear).
- [x] 2.7 Confirm `completeOnboarding()` (`:176-230`) needs no changes — it reads final state fields, order-independent (per proposal Out of Scope).

## Phase 3: Screen — Branch Order and Copy Mapping

- [x] 3.1 In `OnboardingScreen.kt:126-152`, reorder the `when (state.currentStep)` branches to `PROVINCE → ProvinceStep`, `CATEGORY → CategoryStep`, `SCHOOL_YEAR → SchoolYearStep`, `CONFIRMATION → ConfirmationStep`.
- [x] 3.2 Update `hasCurrentStepSelection()` (`:178-184`) to match: `CATEGORY -> selectedTrack != null`, `SCHOOL_YEAR -> selectedSchoolYear != null`.
- [x] 3.3 Swap step copy resource references so numbering follows the new order: `CategoryStep` (`:294-296`) uses `onboarding_step2_title`/`_description`; `SchoolYearStep` (`:261-263`) uses `onboarding_step3_title`/`_description`.
- [x] 3.4 In `composeResources/values/strings.xml:14-17` and `values-en/strings.xml:14-17`, swap the text content of `onboarding_step2_*` (now "Elige tu categoría" / "Choose your category") and `onboarding_step3_*` (now "Elige tu año escolar" / "Choose your school year") to match the new step order, keeping resource key names stable (only referenced-by mapping changes per 3.3).

## Phase 4: Tests

- [x] 4.1 Update `OnboardingViewModelTest.kt` test 1 (`:43-56`, province step) to assert advancing lands on `OnboardingStep.CATEGORY`, not `SCHOOL_YEAR`.
- [x] 4.2 Update test 2 (`:58-83`, "Continue requires a valid selection...") to reorder the walk: province → nextStep → category error; select track → nextStep → school-year error (previously school-year-then-category).
- [x] 4.3 Update test 3 (`:85-102`, "category step keeps four track options...") — rewrite as a school-year-step assertion: after province → category (`selectTrack`) → nextStep, assert `availableSchoolYears` reflects the selected track's filtered years (no more `trackOptions` enabled/disabled check at this step).
- [x] 4.4 Update test 4 (`:104-129`, province boundary rules) to select track before school year and assert `availableSchoolYears` content varies by province/track combination instead of `trackOptions` enabled flags.
- [x] 4.5 Update tests 5, 6, 8, 9 (`:131-157`, `:203-227`, `:229-247`, happy-path completion flows) to call `selectTrack()` before `selectSchoolYear()` in the new step order.
- [x] 4.6 Update test 7 (`:177-201`, "selecting a disabled category is rejected...") — this scenario no longer applies as written (all categories are enabled post-province-selection); replace with a scenario for an out-of-range school year being rejected at the `SCHOOL_YEAR` step instead.
- [x] 4.7 Add new test: `SELF_DIRECTED` category yields the full unfiltered 1-12 year range (no province primary/secondary boundary applied), per spec scenario "Self-directed shows the full unfiltered year range".
- [x] 4.8 Add new test: back-navigation from `SCHOOL_YEAR` clears `selectedSchoolYear`, returns to `CATEGORY`, and keeps `selectedTrack` (spec scenario "Back from school-year clears...").
- [x] 4.9 Add new test: back-navigation from `CATEGORY` clears `selectedTrack`, returns to `PROVINCE`, and keeps `selectedProvince` (spec scenario "Back from category clears...").
- [x] 4.10 Add new test: selecting a different track after already picking a school year clears the stale `selectedSchoolYear` (spec scenario "Changing category after selecting a year clears the stale year").
- [x] 4.11 Review `OnboardingScreenTest.kt` — both existing tests construct explicit `OnboardingUiState` and don't depend on step order beyond the `currentStep` field; no changes needed, but re-run after Phase 3 to confirm no compile/text regressions from the step-copy swap.

## Phase 5: Verification

- [x] 5.1 Run `./gradlew :composeApp:jvmTest` and confirm all `OnboardingViewModelTest` and `OnboardingScreenTest` cases pass.
- [x] 5.2 Manually trace one full flow (Province → Category → School year → Confirmation) mentally against spec scenarios to confirm rendered step numbers/titles match the fixed order.
