# course-catalog-discovery Specification

## Purpose

Provide a discoverable course catalog in the ACTIVITIES tab with search, topic filtering, and rich course cards. This screen replaces the existing ACTIVITIES placeholder.

## Requirements

### Requirement: Course Catalog Screen Display

The system MUST render a course catalog screen when the user navigates to the ACTIVITIES tab. The screen SHALL display a search bar, horizontal topic chips, and a scrollable list of course cards.

#### Scenario: Catalog screen renders on ACTIVITIES tab

- GIVEN the user is authenticated and on the main scaffold
- WHEN the user selects the ACTIVITIES tab
- THEN the system SHALL display the course catalog screen with search bar, topic chips, and course cards

#### Scenario: Catalog screen replaces placeholder

- GIVEN the ACTIVITIES tab previously showed a placeholder
- WHEN this change is deployed
- THEN the placeholder SHALL no longer be visible; the catalog screen SHALL render instead

### Requirement: Topic Chip Filtering

The system MUST provide horizontal topic chips for `Fracciones`, `Álgebra`, and `Geometría`. Selecting a chip SHALL filter the displayed courses to only those matching the selected topic.

#### Scenario: Default state shows all courses

- GIVEN the catalog screen is loaded
- WHEN no topic chip is selected
- THEN the system SHALL display all available courses

#### Scenario: Selecting a topic chip filters courses

- GIVEN courses exist with topics `Fracciones` and `Álgebra`
- WHEN the user selects the `Fracciones` chip
- THEN the system SHALL display only courses with topic `Fracciones`

#### Scenario: Deselecting a chip restores all courses

- GIVEN the `Fracciones` chip is selected and filtering is active
- WHEN the user deselects the chip
- THEN the system SHALL display all available courses again

### Requirement: Course Card Display

The system MUST display each course as a card showing the course name, topic, difficulty, duration, and XP reward. Tapping the entire card SHALL navigate to the course detail screen.

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

### Requirement: Search Bar Filtering

The system MUST provide a search bar that filters courses by name using client-side text matching.

#### Scenario: Search matches course name

- GIVEN courses "Sumas básicas" and "Restas avanzadas" exist
- WHEN the user types "Sumas" in the search bar
- THEN the system SHALL display only "Sumas básicas"

#### Scenario: Empty search shows all filtered courses

- GIVEN a topic filter is active showing 3 courses
- WHEN the user clears the search bar
- THEN the system SHALL show all 3 courses matching the active topic filter
