# Delta for onboarding-flow

## MODIFIED Requirements

### Requirement: Province Selection Step

The system MUST present province selection as the first onboarding step and MUST require the user to select exactly one Argentine province before proceeding.

#### Scenario: Province step is displayed first

- GIVEN the user enters the onboarding flow
- WHEN the first step renders
- THEN the system SHALL display a list of Argentine provinces
- AND the system SHALL NOT show onboarding-category or school-year options yet

#### Scenario: Province selection enables next step

- GIVEN the province step is visible
- WHEN the user selects a valid province
- THEN the system SHALL advance to the onboarding-category selection step
- AND the system SHALL retain the selected province in onboarding state
(Previously: province selection advanced to the school-year step.)

### Requirement: Onboarding Category Classification

The system MUST present exactly four onboarding category options: `Primary`, `Secondary`, `Technical Secondary`, `Self-directed`. The onboarding-category step MUST be the second step, presented immediately after province selection and before school-year selection. All four category options MUST be enabled once a province is selected, since track availability does not vary by province. The user MUST select exactly one category before proceeding to school-year selection.

#### Scenario: Four onboarding categories are available

- GIVEN the onboarding-category step is visible
- WHEN the options are displayed
- THEN the system SHALL show exactly: Primary, Secondary, Technical Secondary, Self-directed
- AND no other options SHALL be available

#### Scenario: Category selection is required

- GIVEN the onboarding-category step is visible
- WHEN no category is selected
- THEN the system SHALL NOT allow proceeding to the school-year step

#### Scenario: All categories are enabled regardless of selected province

- GIVEN a province has been selected
- WHEN the onboarding-category step renders
- THEN all four category options SHALL be selectable
- AND no category SHALL be disabled based on the selected province
(Previously: category was the third step, gated by a previously selected school year, with only categories matching that year enabled.)

### Requirement: Province-Based School-Year Rules

The system MUST derive valid `schoolYear` values from the selected province's school structure filtered by the selected onboarding category (`StudentTrack`). The school-year step MUST be the third step, presented after category selection. Validation SHALL use the following province mapping and year bands, further filtered to only the years whose `allowedTracks` include the selected category.

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

`Self-directed` is a non-narrowing category: the system MUST offer the full 1-12 range for `Self-directed`, unfiltered by the primary/secondary boundary, and MUST NOT include year 13 (technical-only) for `Self-directed`.

#### Scenario: Province defines the primary-to-secondary boundary

- GIVEN a province has been selected
- WHEN the school-year and category rules are evaluated
- THEN the system SHALL use the configured 6-year or 7-year province mapping
- AND the system SHALL place the first non-primary year at 7 for 6-year-primary provinces or 8 for 7-year-primary provinces

#### Scenario: School-year selection is required

- GIVEN the school-year step is visible
- WHEN no year option is selected
- THEN the system SHALL NOT allow proceeding to the confirmation step

#### Scenario: School-year list reflects the selected category

- GIVEN a province and a category have been selected
- WHEN the school-year step renders
- THEN the system SHALL display only the year options whose `allowedTracks` include the selected category
- AND the system SHALL NOT display years that are invalid for that category

#### Scenario: Self-directed shows the full unfiltered year range

- GIVEN a province has been selected
- AND the selected category is `Self-directed`
- WHEN the school-year step renders
- THEN the system SHALL display years 1 through 12
- AND the system SHALL NOT display year 13
- AND this range SHALL NOT be narrowed by the province's primary/secondary boundary
(Previously: the school-year step was second, presented directly after province selection, and the year list was derived from province alone with no category filter; category selection followed as the third step, gated by the chosen year.)

## ADDED Requirements

### Requirement: Onboarding Step Order

The system MUST present onboarding steps in this order: Province, Category (`StudentTrack`), School year, Confirmation. Each step MUST reflect its position in this order in its rendered step number and title, independent of the underlying `OnboardingStep` enum ordering.

#### Scenario: Steps render in the fixed order

- GIVEN the user progresses through onboarding from the start
- WHEN each step is completed and the next step renders
- THEN the steps SHALL appear in this order: Province, Category, School year, Confirmation

### Requirement: Onboarding Back-Navigation Reset Semantics

When the user navigates backward from a step, the system MUST clear the selection made on the step being left and MUST return to the immediately preceding step in the fixed order (Province, Category, School year, Confirmation).

#### Scenario: Back from school-year clears the selected year and returns to category

- GIVEN the user is on the school-year step, having already selected a category
- WHEN the user navigates back
- THEN the system SHALL clear the selected school year
- AND the system SHALL return to the category step
- AND the previously selected category SHALL remain selected

#### Scenario: Back from category clears the selected track and returns to province

- GIVEN the user is on the category step, having already selected a province
- WHEN the user navigates back
- THEN the system SHALL clear the selected category
- AND the system SHALL return to the province step
- AND the previously selected province SHALL remain selected

#### Scenario: Changing category after selecting a year clears the stale year

- GIVEN the user has selected a province, a category, and a school year
- WHEN the user navigates back to the category step and selects a different category
- THEN the system SHALL clear the previously selected school year
- AND the school-year step SHALL show the year list derived from the new category on re-entry
