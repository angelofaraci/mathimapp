# Delta for client-server-contract

## ADDED Requirements

### Requirement: Sealed ExercisePayload Hierarchy

The shared contract MUST define a sealed `ExercisePayload` hierarchy with `@Serializable` polymorphic discriminators. Phase 1 payload types: `MultipleChoicePayload`, `InputValuePayload`, `MultiSelectPayload`.

#### Scenario: MultipleChoicePayload serializes with discriminator

- GIVEN a `MultipleChoicePayload` with options and correct option ID
- WHEN serialized with `kotlinx.serialization`
- THEN the JSON SHALL include a `type` discriminator field set to `"multipleChoice"`
- AND SHALL include `options` (list of `{id, text}`) and `correctOptionId`

#### Scenario: InputValuePayload serializes with discriminator

- GIVEN an `InputValuePayload` with a correct value
- WHEN serialized with `kotlinx.serialization`
- THEN the JSON SHALL include a `type` discriminator field set to `"inputValue"`
- AND SHALL include `correctValue` as a string

#### Scenario: MultiSelectPayload serializes with discriminator

- GIVEN a `MultiSelectPayload` with options and correct option IDs
- WHEN serialized with `kotlinx.serialization`
- THEN the JSON SHALL include a `type` discriminator field set to `"multiSelect"`
- AND SHALL include `options` (list of `{id, text}`) and `correctOptionIds` (list of strings)

### Requirement: Exercise Model with Payload Field

The shared `Exercise` model MUST include a `payload: ExercisePayload` property alongside persisted base metadata fields (`id`, `lessonId`, `type`, `title`). The public contract MUST NOT expose non-persisted `points` or `difficulty` fields.

#### Scenario: Exercise deserializes with typed payload

- GIVEN a JSON exercise object with a `payload` field containing a `MultipleChoicePayload`
- WHEN the client deserializes it using `kotlinx.serialization`
- THEN the `Exercise.payload` property SHALL be a `MultipleChoicePayload` instance

#### Scenario: Exercise serializes with payload

- GIVEN an `Exercise` with a typed payload
- WHEN serialized to JSON
- THEN the output SHALL include the `payload` field with the correct discriminator

## MODIFIED Requirements

### Requirement: Shared Lesson Completion Contract

The system MUST define lesson completion requests in the shared contract and MUST use that shared shape from the client when submitting completion data. Exercise completion data within lesson completion SHALL reference exercises by ID only; payload details are not included in completion submissions.
(Previously: No mention of exercise payload exclusion from completion requests.)

#### Scenario: Shared request shape is used

- GIVEN the client submits a lesson completion request
- WHEN the request is serialized
- THEN the system SHALL use the shared CompleteLessonRequest contract

#### Scenario: Completion data reaches the server

- GIVEN a valid lesson completion payload
- WHEN the client sends it to the backend
- THEN the system SHALL preserve the expected fields and values
