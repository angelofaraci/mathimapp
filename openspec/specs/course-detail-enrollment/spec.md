# course-detail-enrollment Specification

## Purpose

Provide a course detail screen reachable from the catalog, with metadata display and an enrollment CTA that calls the existing backend join endpoint.

## Requirements

### Requirement: Detail Screen Display

The system MUST render a `CourseDetailScreen` showing course name, topic, difficulty, duration, XP reward, and full description. The screen SHALL be reachable by tapping any course card in the catalog.

#### Scenario: Detail screen renders with all metadata

- GIVEN a course with name, topic, difficulty, duration, XP, and description
- WHEN the user taps the course card in the catalog
- THEN the system SHALL display the detail screen with all fields visible

#### Scenario: Detail screen handles missing optional fields

- GIVEN a course with name and topic but no duration or XP
- WHEN the detail screen renders
- THEN the system SHALL display available fields and omit or show defaults for missing ones

### Requirement: Card Tap Navigation

The system MUST navigate from `CourseCatalogScreen` to `CourseDetailScreen` when the user taps a course card. Navigation SHALL use a local `selectedCourseId` state; clearing it returns to the catalog list. A back button SHALL clear `selectedCourseId`.

#### Scenario: Tapping a card opens detail

- GIVEN the catalog screen displays course cards
- WHEN the user taps any card
- THEN the system SHALL set `selectedCourseId` and render the detail screen

#### Scenario: Back button returns to catalog

- GIVEN the detail screen is visible
- WHEN the user taps the back button
- THEN the system SHALL clear `selectedCourseId` and render the catalog list

### Requirement: CTA State — Enrolled vs Not Enrolled

The system SHALL display a primary CTA on the detail screen. If the user is already enrolled in the course, the CTA text MUST be "Continue". If the user is NOT enrolled, the CTA text MUST be "Enroll".

#### Scenario: Enrolled user sees Continue CTA

- GIVEN the user is enrolled in the displayed course
- WHEN the detail screen renders
- THEN the CTA SHALL display the text "Continue"

#### Scenario: Unenrolled user sees Enroll CTA

- GIVEN the user is NOT enrolled in the displayed course
- WHEN the detail screen renders
- THEN the CTA SHALL display the text "Enroll"

#### Scenario: CTA state updates after successful enrollment

- GIVEN the user is NOT enrolled and sees "Enroll"
- WHEN the user taps "Enroll" and the backend call succeeds
- THEN the CTA SHALL update to "Continue"

### Requirement: Enrollment Action

The system MUST call `CourseRepository.joinCourseByCode` when the user taps "Enroll" on a course that has a `joinCode`. On success, the system SHALL refresh `UserProgress` and update the CTA state.

#### Scenario: Enroll calls backend join endpoint

- GIVEN the user is NOT enrolled and the course has a `joinCode`
- WHEN the user taps "Enroll"
- THEN the system SHALL call `joinCourseByCode` with the course's join code

#### Scenario: Enrollment success refreshes state

- GIVEN the `joinCourseByCode` call succeeds
- WHEN the response is received
- THEN the system SHALL refresh `UserProgress` and switch the CTA to "Continue"

#### Scenario: Course without joinCode shows Start CTA

- GIVEN the user is NOT enrolled and the course has NO `joinCode`
- WHEN the detail screen renders
- THEN the CTA SHALL display "Start" and SHALL NOT call `joinCourseByCode`

### Requirement: Enrollment Error Handling

The system MUST surface enrollment failures as a simple error message in the UI state. The error SHALL NOT crash the screen or leave it in an inconsistent state.

#### Scenario: Enrollment failure shows error message

- GIVEN the `joinCourseByCode` call fails (network error, invalid code, etc.)
- WHEN the failure is caught
- THEN the system SHALL display a simple error string on the detail screen

#### Scenario: Screen remains usable after error

- GIVEN an enrollment error is displayed
- WHEN the user dismisses or ignores the error
- THEN the detail screen SHALL remain interactive and the CTA SHALL still be tappable
