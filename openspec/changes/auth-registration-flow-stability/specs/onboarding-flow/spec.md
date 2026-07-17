# Delta for onboarding-flow

## ADDED Requirements

### Requirement: Action Buttons Always Reachable

The system MUST ensure all step action buttons (Continue, Back, Complete) remain visible and reachable on screen at all times during the onboarding flow, regardless of content length or screen size. Scrollable step content SHALL occupy remaining space without pushing action buttons off-screen.

#### Scenario: Province list with Continue button visible

- GIVEN the user is on the province selection step
- WHEN the province list is rendered on any screen size
- THEN the Continue button SHALL remain visible at the bottom of the screen
- AND the province list SHALL scroll within the remaining space above the button

#### Scenario: Long content does not hide buttons

- GIVEN any onboarding step with scrollable content exceeding screen height
- WHEN the user scrolls to the bottom of the content
- THEN all action buttons SHALL remain visible and tappable
- AND no button SHALL be positioned below the visible viewport

## MODIFIED Requirements

### Requirement: Onboarding state survives recomposition

The system MUST persist the onboarding outcome (province, school year, onboarding category) and MUST navigate to `CourseScreen` with the selected school-year value after all steps are completed. Onboarding state SHALL survive both recomposition and device configuration changes (rotation, locale change) without losing partial selections or resetting to the first step. The onboarding state holder SHALL be backed by a `ViewModel` instance.
(Previously: State survived recomposition only; no explicit coverage of device configuration changes.)

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

#### Scenario: Onboarding state survives device rotation

- GIVEN the user is mid-onboarding with partial selections (e.g., province selected, school-year pending)
- WHEN the device rotates
- THEN the system SHALL remain on the current step
- AND all previously selected values SHALL be preserved
- AND the user SHALL NOT need to restart from the first step
