# Delta for profile-screen

## MODIFIED Requirements

### Requirement: Inicio Tab Hosts HomeDashboardScreen

The system SHALL render the `HomeDashboardScreen` as Inicio tab content, replacing the legacy `CourseScreen`.
(Previously: Inicio tab hosted `CourseScreen` with its existing behavior.)

#### Scenario: Inicio displays dashboard content

- GIVEN the user selects the Inicio tab
- WHEN the tab content renders
- THEN the system SHALL display the `HomeDashboardScreen` with greeting, progress summary, and empty-state card

#### Scenario: Dashboard navigation to Activities works

- GIVEN the dashboard is visible on the Inicio tab
- WHEN the user taps a CTA that targets the Activities tab
- THEN the system SHALL switch to the Activities tab and display the course catalog
