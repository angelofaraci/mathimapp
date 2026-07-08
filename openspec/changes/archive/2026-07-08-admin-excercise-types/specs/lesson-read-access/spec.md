# Delta for lesson-read-access

## ADDED Requirements

### Requirement: Typed Payload Answer Stripping for Students

The system MUST strip correct answer fields from typed exercise payloads when returning lesson content to users with role `STUDENT`. The stripping rules per payload type are:

| Payload Type | Fields to Strip |
|---|---|
| `MultipleChoicePayload` | `correctOptionId` |
| `InputValuePayload` | `correctValue` |
| `MultiSelectPayload` | `correctOptionIds` |

#### Scenario: Student receives MultipleChoice payload without correct answer

- GIVEN a lesson with a MultipleChoice exercise
- WHEN a STUDENT reads the lesson
- THEN the exercise payload SHALL omit `correctOptionId`
- AND the `options` list SHALL remain intact

#### Scenario: Student receives InputValue payload without correct value

- GIVEN a lesson with an InputValue exercise
- WHEN a STUDENT reads the lesson
- THEN the exercise payload SHALL omit `correctValue`

#### Scenario: Student receives MultiSelect payload without correct IDs

- GIVEN a lesson with a MultiSelect exercise
- WHEN a STUDENT reads the lesson
- THEN the exercise payload SHALL omit `correctOptionIds`
- AND the `options` list SHALL remain intact

#### Scenario: Admin receives full typed payload

- GIVEN a lesson with exercises of any type
- WHEN an ADMIN reads the lesson
- THEN each exercise payload SHALL include all correct answer fields

#### Scenario: Teacher (creator) receives full typed payload

- GIVEN a lesson with exercises of any type
- WHEN the course-creator TEACHER reads the lesson
- THEN each exercise payload SHALL include all correct answer fields

## MODIFIED Requirements

### Requirement: Exercise Answers Hidden for Students

The system SHALL hide exercise correct answer fields when the requester has role `STUDENT`. For typed payloads, this means stripping `correctOptionId`, `correctValue`, or `correctOptionIds` from the respective payload type. The options/content of the exercise SHALL remain visible.
(Previously: Exercise `correctAnswer` field was set to empty string for students.)

#### Scenario: Student receives exercises without correct answers

- GIVEN a lesson with exercises
- WHEN a STUDENT reads the lesson
- THEN each exercise payload SHALL have its correct answer field removed

#### Scenario: Teacher receives visible answers

- GIVEN a lesson with exercises
- WHEN the course-creator TEACHER reads the lesson
- THEN each exercise payload SHALL contain all correct answer fields
