# Delta for course-catalog-discovery

## MODIFIED Requirements

### Requirement: Course Card Display

The system MUST display each course as a card showing the course name, topic, difficulty, duration, and XP reward. Tapping the entire card SHALL navigate to the course detail screen.
(Previously: Card displayed metadata with no tap navigation behavior specified.)

#### Scenario: Card displays all discovery metadata

- GIVEN a course with name "Sumas básicas", topic "Fracciones", difficulty "Fácil", duration 15min, and XP 50
- WHEN the card is rendered
- THEN the system SHALL display all five fields on the card

#### Scenario: Card renders for courses with missing optional fields

- GIVEN a course with name and topic but no duration or XP
- WHEN the card is rendered
- THEN the system SHALL display available fields and omit or show defaults for missing ones

#### Scenario: Tapping card navigates to detail

- GIVEN the catalog displays course cards
- WHEN the user taps any course card
- THEN the system SHALL navigate to the course detail screen for that course

## REMOVED Requirements

### Requirement: Visual-Only Enrollment Button

(Reason: Superseded by the detail-screen CTA in `course-detail-enrollment` spec. Enrollment action now lives on the detail screen, not on catalog cards.)
(Migration: Remove the "Inscribirse" button from `CourseCard` composable. The entire card becomes the tap target for navigation.)
