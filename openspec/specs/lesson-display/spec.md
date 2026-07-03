# lesson-display Specification

## Purpose

Define the shared `Lesson.exerciseCount` field and its computation in the course response, enabling lesson cards to display exercise counts without N+1 API calls.

## Requirements

### Requirement: Lesson Exercise Count Field

The shared `Lesson` model MUST include an `exerciseCount: Int = 0` field. The field SHALL be serializable and deserializable by both client and server using the existing `@Serializable` mechanism.

#### Scenario: Lesson carries exercise count

- GIVEN the server builds a lesson response
- WHEN the lesson is serialized
- THEN the JSON payload SHALL contain `exerciseCount` with the correct integer value

#### Scenario: Client deserializes exercise count

- GIVEN the client receives a Lesson JSON payload with `exerciseCount`
- WHEN the client deserializes the response
- THEN the `Lesson` object SHALL retain the `exerciseCount` value

#### Scenario: Default value for backward compatibility

- GIVEN a client receives a Lesson JSON without `exerciseCount`
- WHEN the client deserializes the response
- THEN the `Lesson` object SHALL have `exerciseCount = 0` without error

### Requirement: Exercise Count Computation

The system MUST compute `exerciseCount` as `COUNT(exercises)` per lesson when building the course response in `CourseService.getCourseById()`. The computation SHALL use a single SQL aggregation, not N+1 queries.

#### Scenario: Course response includes computed exercise counts

- GIVEN a course with 3 lessons having 5, 3, and 7 exercises respectively
- WHEN `GET /courses/{id}` is called
- THEN the response SHALL include lessons with `exerciseCount` values 5, 3, and 7

#### Scenario: Lesson with no exercises has count zero

- GIVEN a lesson with no associated exercises
- WHEN the course response is built
- THEN the lesson's `exerciseCount` SHALL be 0

### Requirement: Exercise Count Display on Lesson Card

The system SHALL display "{exerciseCount} ejercicios" on each lesson card in the course detail screen.

#### Scenario: Lesson card shows exercise count

- GIVEN a lesson with `exerciseCount = 5`
- WHEN the lesson card is rendered
- THEN the system SHALL display "5 ejercicios" on the card

#### Scenario: Zero exercise count displays correctly

- GIVEN a lesson with `exerciseCount = 0`
- WHEN the lesson card is rendered
- THEN the system SHALL display "0 ejercicios" on the card
