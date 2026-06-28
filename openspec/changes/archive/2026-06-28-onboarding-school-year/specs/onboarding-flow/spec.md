# onboarding-flow Specification

## Purpose

Define the mandatory multi-step onboarding flow shown after registration (and on first launch) before the user can access `CourseScreen`. The flow collects province, school year, and onboarding category to derive a curriculum-appropriate content list.

## Requirements

### Requirement: Mandatory Onboarding Gate

The system MUST display the onboarding flow after successful registration and MUST prevent access to `CourseScreen` until all onboarding steps are completed.

#### Scenario: Registration redirects to onboarding

- GIVEN the user completes registration successfully
- WHEN the auth session is established
- THEN the system SHALL navigate to the onboarding flow
- AND the system SHALL NOT show `CourseScreen`

#### Scenario: Incomplete onboarding blocks course access

- GIVEN the user has not completed onboarding
- WHEN the app attempts to resolve the post-auth view
- THEN the system SHALL display the onboarding flow
- AND the system SHALL NOT allow navigation to `CourseScreen`

### Requirement: Province Selection Step

The system MUST present province selection as the first onboarding step and MUST require the user to select exactly one Argentine province before proceeding.

#### Scenario: Province step is displayed first

- GIVEN the user enters the onboarding flow
- WHEN the first step renders
- THEN the system SHALL display a list of Argentine provinces
- AND the system SHALL NOT show school-year options yet

#### Scenario: Province selection enables next step

- GIVEN the province step is visible
- WHEN the user selects a valid province
- THEN the system SHALL advance to the school-year selection step
- AND the system SHALL retain the selected province in onboarding state

### Requirement: Province-Based School-Year Rules

The system MUST derive valid `schoolYear` values from the selected province's school structure. Validation SHALL use the following province mapping and year bands.

Primary-year mapping for this slice SHALL be:

| Primary years | Provinces |
|---|---|
| 6 | Buenos Aires, Catamarca, Chubut, Córdoba, Corrientes, Entre Ríos, Formosa, La Pampa, San Juan, San Luis, Tierra del Fuego, Tucumán |
| 7 | CABA, Chaco, Jujuy, La Rioja, Mendoza, Misiones, Neuquén, Río Negro, Salta, Santa Cruz, Santa Fe, Santiago del Estero |

Year-band rules for this slice SHALL be:

| Province structure | Primary | Secondary | Technical Secondary |
|---|---|---|---|
| 6-year primary | 1-6 | 7-12 | 7-13 |
| 7-year primary | 1-7 | 8-12 | 8-13 |

#### Scenario: Province defines the primary-to-secondary boundary

- GIVEN a province has been selected
- WHEN the school-year and category rules are evaluated
- THEN the system SHALL use the configured 6-year or 7-year province mapping
- AND the system SHALL place the first non-primary year at 7 for 6-year-primary provinces or 8 for 7-year-primary provinces

#### Scenario: School-year selection is required

- GIVEN the school-year step is visible
- WHEN no year option is selected
- THEN the system SHALL NOT allow proceeding to the onboarding-category step

### Requirement: Onboarding Category Classification

The system MUST present exactly four onboarding category options: `Primary`, `Secondary`, `Technical Secondary`, `Self-directed`. The user MUST select exactly one category before completing onboarding.

#### Scenario: Four onboarding categories are available

- GIVEN the onboarding-category step is visible
- WHEN the options are displayed
- THEN the system SHALL show exactly: Primary, Secondary, Technical Secondary, Self-directed
- AND no other options SHALL be available

#### Scenario: Category selection is required

- GIVEN the onboarding-category step is visible
- WHEN no category is selected
- THEN the system SHALL NOT allow completing onboarding

### Requirement: Category Semantics

The system MUST treat onboarding category as metadata that validates the selected year against the province-derived ranges, but course filtering in this slice SHALL continue to use only the selected numeric `schoolYear`.

#### Scenario: Technical secondary extends valid year availability by one year

- GIVEN the user selected a province with a resolved school-structure mapping
- WHEN the selected `schoolYear` is the extra upper year beyond standard secondary
- THEN the system SHALL accept that year only when the category is `Technical Secondary`
- AND the same year SHALL be invalid for `Secondary`

#### Scenario: Technical secondary does not change course selection semantics

- GIVEN two users select the same numeric `schoolYear`
- AND one category is `Secondary`
- AND the other category is `Technical Secondary`
- WHEN the app requests official courses
- THEN the system SHALL use the same `schoolYear` filter value for both
- AND any difference in this slice SHALL be limited to which year values are selectable

#### Scenario: Self-directed is an explicit category

- GIVEN the user is learning outside formal school
- WHEN the onboarding-category step is displayed
- THEN the system SHALL allow selecting `Self-directed`
- AND the user SHALL still complete province and school-year selection for content recommendation

### Requirement: Diagnostic Questions Deferred

The system MUST NOT ask mastery, level, or diagnostic questions in this onboarding slice.

#### Scenario: No diagnostic questions are shown

- GIVEN the user completes the onboarding flow
- WHEN all steps are rendered in this slice
- THEN the system SHALL only ask for province, school year, and onboarding category
- AND no mastery or level questionnaire SHALL be shown

### Requirement: Onboarding Completion and Navigation

The system MUST persist the onboarding outcome (province, school year, onboarding category) and MUST navigate to `CourseScreen` with the selected school-year value after all steps are completed.

#### Scenario: Complete onboarding navigates to courses

- GIVEN the user has selected province, school year, and onboarding category
- WHEN the user confirms completion
- THEN the system SHALL persist the onboarding profile locally
- AND the system SHALL navigate to `CourseScreen`
- AND the system SHALL use the selected school-year value for course filtering

#### Scenario: Onboarding state survives recomposition

- GIVEN the user is mid-onboarding (partial selections made)
- WHEN the composable recomposes
- THEN the system SHALL retain previously selected values
- AND the user SHALL NOT need to restart from the first step
