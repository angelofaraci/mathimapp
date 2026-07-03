# Delta for course-catalog-discovery

## MODIFIED Requirements

### Requirement: Functional Enrollment Button

The system MUST display an "Inscribirse" button on each course card. When tapped, the button SHALL call `POST /courses/{id}/enroll` for official courses, update local progress state, and navigate to the course detail screen on success.
(Previously: Button was visual-only and did not trigger any backend enrollment call)

#### Scenario: Button is visible on every card

- GIVEN the catalog displays course cards
- WHEN a card is rendered
- THEN the system SHALL show an "Inscribirse" button on that card

#### Scenario: Button tap triggers enrollment

- GIVEN the user taps "Inscribirse" on an official course card
- WHEN the tap is processed
- THEN the system SHALL call the enrollment endpoint and navigate to the course detail screen on success

#### Scenario: Enrollment failure shows error

- GIVEN the enrollment endpoint returns an error
- WHEN the user taps "Inscribirse"
- THEN the system SHALL display an error message and remain on the catalog screen
