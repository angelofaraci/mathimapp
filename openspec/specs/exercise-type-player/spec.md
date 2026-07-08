# Exercise Type Player Specification

## Purpose

Define type-specific exercise rendering, answer validation, and wrong-answer retry behavior in the Compose app for MultipleChoice, InputValue, and MultiSelect payloads.

## Requirements

### Requirement: Type-Specific Player Dispatch

The system MUST render a different Compose player composable based on the `ExercisePayload` type. Phase 1 types: `MultipleChoicePayload`, `InputValuePayload`, `MultiSelectPayload`.

| Payload Type | UI Component | Selection Mode |
|---|---|---|
| MultipleChoice | Radio-button option cards | Single select |
| InputValue | Text input + submit button | Free text |
| MultiSelect | Checkbox option cards | Multi select |

#### Scenario: MultipleChoice renders radio-button cards

- GIVEN an exercise with `MultipleChoicePayload` containing N options
- WHEN the lesson player loads the exercise
- THEN N radio-button cards SHALL render with single-selection behavior

#### Scenario: InputValue renders text input

- GIVEN an exercise with `InputValuePayload`
- WHEN the lesson player loads the exercise
- THEN a text input field and submit button SHALL render

#### Scenario: MultiSelect renders checkbox cards

- GIVEN an exercise with `MultiSelectPayload` containing N options
- WHEN the lesson player loads the exercise
- THEN N checkbox cards SHALL render with multi-selection behavior

#### Scenario: Unknown payload type shows error

- GIVEN an unrecognized payload type discriminator
- WHEN the player attempts to render
- THEN a fallback error placeholder SHALL display without crashing

### Requirement: Type-Specific Answer Validation

The system MUST validate submitted answers using type-specific logic.

| Payload Type | Validation Rule |
|---|---|
| MultipleChoice | Selected option ID matches `correctOptionId` |
| InputValue | Trimmed input equals `correctValue` (case-insensitive) |
| MultiSelect | Selected set exactly matches `correctOptionIds` |

#### Scenario: MultipleChoice validates single option

- GIVEN `correctOptionId = "B"`
- WHEN the student selects option B and submits
- THEN the answer SHALL be marked correct

#### Scenario: InputValue validates trimmed text

- GIVEN `correctValue = "42"`
- WHEN the student types `" 42 "` and submits
- THEN the answer SHALL be marked correct

#### Scenario: MultiSelect validates exact set match

- GIVEN `correctOptionIds = ["A", "C"]`
- WHEN the student selects exactly A and C and submits
- THEN the answer SHALL be marked correct

#### Scenario: MultiSelect rejects partial match

- GIVEN `correctOptionIds = ["A", "C"]`
- WHEN the student selects only A and submits
- THEN the answer SHALL be marked incorrect

#### Scenario: InputValue rejects empty submission

- GIVEN an InputValue exercise
- WHEN the student submits an empty string
- THEN the submission SHALL be rejected client-side

### Requirement: Wrong-Answer Immediate Retry with Feedback

The system MUST provide immediate feedback and allow retry on wrong answers without advancing.

#### Scenario: Wrong answer shows feedback and stays on exercise

- GIVEN a student submits an incorrect answer
- WHEN validated
- THEN feedback indicating wrong answer SHALL display
- AND the exercise SHALL remain on screen for retry

#### Scenario: Correct answer advances

- GIVEN a student submits a correct answer
- WHEN validated
- THEN the exercise SHALL be marked completed and advance to next

#### Scenario: Retry allows selection change

- GIVEN a student selected a wrong option
- WHEN preparing to retry
- THEN the student SHALL be able to change their selection

#### Scenario: Multiple wrong attempts do not block

- GIVEN a student answers incorrectly multiple times
- WHEN they eventually answer correctly
- THEN the exercise SHALL be marked completed and advance

### Requirement: Answer Hiding in Player Payload

The system MUST NOT receive correct answer data in student-facing payloads.

#### Scenario: Student payload omits correct answer

- GIVEN a student loads a lesson
- WHEN the payload is received
- THEN `correctOptionId`, `correctValue`, and `correctOptionIds` SHALL be absent or null

#### Scenario: Admin payload includes correct answer

- GIVEN an admin loads an exercise for editing
- WHEN the payload is received
- THEN all correct answer fields SHALL be present
