# admin-exercise-crud Specification

## Purpose

Define admin-only CRUD operations for exercises and their assignment to lessons. Exercise-type-specific gameplay behavior is out of scope; this spec covers assignment and basic content management only.

## Requirements

### Requirement: Admin Exercise Creation

The system MUST allow ADMIN users to create exercises with a `lessonId`, `type`, `question`, `options`, and `correctAnswer`.

#### Scenario: Admin creates an exercise for a lesson

- GIVEN an existing lesson
- WHEN an ADMIN submits a valid exercise with type, question, options, and correct answer
- THEN the system SHALL create the exercise linked to that lesson

#### Scenario: Exercise for non-existent lesson is rejected

- GIVEN an exercise creation request with a `lessonId` that does not exist
- WHEN the request is processed
- THEN the system SHALL reject with 400

#### Scenario: Missing required exercise fields are rejected

- GIVEN an ADMIN submits an exercise without `type` or `question`
- WHEN the request is processed
- THEN the system SHALL reject with 400 and indicate missing fields

### Requirement: Admin Exercise Update

The system MUST allow ADMIN users to update exercise fields: `type`, `question`, `options`, `correctAnswer`, and `lessonId` (reassignment).

#### Scenario: Admin updates exercise content

- GIVEN an existing exercise
- WHEN an ADMIN updates the exercise question, options, or correct answer
- THEN the system SHALL persist the changes

#### Scenario: Admin reassigns exercise to a different lesson

- GIVEN an exercise linked to lesson A
- WHEN an ADMIN updates the exercise with `lessonId` pointing to lesson B
- THEN the system SHALL reassign the exercise to lesson B

#### Scenario: Reassign to non-existent lesson is rejected

- GIVEN an exercise update request with a `lessonId` that does not exist
- WHEN the request is processed
- THEN the system SHALL reject with 400

### Requirement: Admin Exercise Deletion

The system MUST allow ADMIN users to delete exercises.

#### Scenario: Admin deletes an exercise

- GIVEN an existing exercise
- WHEN an ADMIN deletes the exercise
- THEN the system SHALL remove the exercise record

#### Scenario: Delete non-existent exercise returns 404

- GIVEN no exercise exists with the specified ID
- WHEN an ADMIN attempts to delete it
- THEN the system SHALL return 404

### Requirement: Admin Exercise Listing

The system MUST provide admin-only endpoints to list exercises, optionally filtered by `lessonId`.

#### Scenario: Admin lists exercises for a lesson

- GIVEN a lesson with multiple exercises
- WHEN an ADMIN requests exercises filtered by `lessonId`
- THEN the system SHALL return all exercises for that lesson in order

#### Scenario: Admin lists all exercises

- WHEN an ADMIN requests the full exercise list
- THEN the system SHALL return all exercises across all lessons
