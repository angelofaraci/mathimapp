# home-dashboard Specification

## Purpose

Provide the authenticated landing screen on the HOME tab with greeting, progress summary, empty-state learning card, and navigation CTA. Frontend-only — no new backend contracts.

## Requirements

### Requirement: Dashboard Greeting

The system SHALL display a personalized greeting derived from the authenticated user's display name. The greeting text SHALL vary by time of day (morning, afternoon, evening).

#### Scenario: Greeting shows user name with time-based salutation

- GIVEN an authenticated user with display name "María"
- WHEN the dashboard renders at 10:00 local time
- THEN the greeting SHALL display "Buenos días, María" or equivalent morning salutation

#### Scenario: Missing display name uses fallback

- GIVEN the authenticated user model has no display name
- WHEN the dashboard renders
- THEN the greeting SHALL display a generic salutation without a name

### Requirement: Progress Summary Chip

The system SHALL render a compact progress summary showing the user's current level and a synthetic activity indicator derived from local completed-lesson data.

#### Scenario: Level and activity display from local data

- GIVEN the user has completed 5 lessons and level 2
- WHEN the progress chip renders
- THEN the chip SHALL show level 2 and an activity count of 5

#### Scenario: Zero progress shows empty state

- GIVEN the user has completed 0 lessons
- WHEN the progress chip renders
- THEN the chip SHALL show level 0 and activity count of 0

### Requirement: Empty-State Learning Card

The system SHALL render a "Continuar aprendiendo" card in an empty-state configuration when no in-progress lesson entity exists. The card SHALL display an illustration placeholder, a title, a brief description, and a primary CTA.

#### Scenario: Empty-state card renders when no in-progress lesson

- GIVEN no in-progress lesson entity exists locally
- WHEN the dashboard renders
- THEN the "Continuar aprendiendo" section SHALL display an empty-state card with illustration, title, description, and CTA

#### Scenario: CTA navigates to Activities tab

- GIVEN the empty-state card is visible
- WHEN the user taps the primary CTA
- THEN the system SHALL navigate to the Activities tab via the main router

### Requirement: Catalog CTA

The system SHALL provide a secondary CTA that navigates to the course catalog within the Activities tab.

#### Scenario: Catalog CTA navigates correctly

- GIVEN the dashboard is visible
- WHEN the user taps the catalog CTA
- THEN the system SHALL navigate to the Activities tab with the course catalog view

### Requirement: Synthetic Streak Display

The system SHALL display a synthetic activity count derived from `completedLessonIds.size`, capped at 7. The display SHALL NOT imply date-based streak continuity because no date tracking exists.

#### Scenario: Streak equals completed count when below cap

- GIVEN the user has 3 completed lessons
- WHEN the synthetic streak is computed
- THEN the system SHALL report 3

#### Scenario: Streak caps at 7

- GIVEN the user has 12 completed lessons
- WHEN the synthetic streak is computed
- THEN the system SHALL report 7

#### Scenario: Label avoids date-based streak semantics

- GIVEN the synthetic streak is displayed
- WHEN the UI renders the streak label
- THEN the label SHALL NOT use "días" or any date-continuity implication
