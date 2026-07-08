# admin-exercise-crud Specification

## Purpose

Define admin-only CRUD operations for exercises and their assignment to lessons. Exercise-type-specific gameplay behavior is out of scope; this spec covers assignment and basic content management only.

## Requirements

### Requirement: Admin Exercise Creation

The system MUST allow ADMIN users to create exercises with `lessonId`, `type`, and a type-specific `payload` (`MultipleChoicePayload`, `InputValuePayload`, or `MultiSelectPayload`).
(Previously: Admin creates exercises with flat `question`, `options`, and `correctAnswer` fields.)

#### Scenario: Admin creates an exercise for a lesson

- GIVEN an existing lesson
- WHEN an ADMIN submits a valid exercise with type and well-formed payload
- THEN the system SHALL create the exercise linked to that lesson

#### Scenario: Exercise for non-existent lesson is rejected

- GIVEN a creation request with a `lessonId` that does not exist
- WHEN processed
- THEN reject with 400

#### Scenario: Missing required fields are rejected

- GIVEN an ADMIN submits without `type` or valid payload
- WHEN processed
- THEN reject with 400 indicating missing fields

### Requirement: Admin Exercise Update

The system MUST allow ADMIN users to update `type`, `payload`, and `lessonId` (reassignment). Changing type replaces the entire payload.
(Previously: Admin updates flat `type`, `question`, `options`, `correctAnswer`, and `lessonId`.)

#### Scenario: Admin updates exercise payload

- GIVEN an existing exercise
- WHEN an ADMIN updates with a valid typed payload
- THEN the system SHALL persist the changes

#### Scenario: Admin reassigns exercise to a different lesson

- GIVEN an exercise linked to lesson A
- WHEN an ADMIN updates with `lessonId` pointing to lesson B
- THEN the exercise SHALL be reassigned to lesson B

#### Scenario: Reassign to non-existent lesson is rejected

- GIVEN an update request with a non-existent `lessonId`
- WHEN processed
- THEN reject with 400

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

### Requirement: Per-Type Payload Validation

The system MUST validate exercise payloads against type-specific rules during create and update.

| Payload Type | Validation Rule |
|---|---|
| MultipleChoice | Minimum 2 options; `correctOptionId` must reference a valid option |
| InputValue | Non-empty `correctValue` required |
| MultiSelect | Minimum 2 options; at least 1 correct; all `correctOptionIds` must reference valid options |

#### Scenario: MultipleChoice rejects single option

- GIVEN a MultipleChoice payload with 1 option
- WHEN validated
- THEN reject with 400 indicating minimum 2 options required

#### Scenario: InputValue rejects empty correct value

- GIVEN an InputValue payload with empty `correctValue`
- WHEN validated
- THEN reject with 400 indicating correct value is required

#### Scenario: MultiSelect rejects invalid correct reference

- GIVEN a MultiSelect payload with `correctOptionIds` containing an ID not in options
- WHEN validated
- THEN reject with 400 indicating invalid correct option reference

### Requirement: Type-Aware Admin Form

The admin exercise form MUST render different input fields based on the selected exercise type.

| Type | Form Fields |
|---|---|
| MultipleChoice | Option list editor (add/remove) + single correct selector |
| InputValue | Single text input for correct value |
| MultiSelect | Option list with checkboxes for multiple correct answers |

#### Scenario: MultipleChoice form shows option editor

- GIVEN admin selects MultipleChoice type
- WHEN the form renders
- THEN an option list editor with add/remove controls and single correct selector SHALL appear

#### Scenario: InputValue form shows single text input

- GIVEN admin selects InputValue type
- WHEN the form renders
- THEN a single text input for correct value SHALL appear with no options list

#### Scenario: MultiSelect form shows checkbox editor

- GIVEN admin selects MultiSelect type
- WHEN the form renders
- THEN an option list with checkboxes for selecting multiple correct answers SHALL appear

### Requirement: Admin Exercise Listing Returns Typed Payloads

The system MUST return exercises with their full typed payload in admin listing endpoints.

#### Scenario: Admin lists exercises with payload data

- GIVEN a lesson with exercises of different types
- WHEN an admin requests the exercise list
- THEN each exercise SHALL include its typed `payload` field with full data
