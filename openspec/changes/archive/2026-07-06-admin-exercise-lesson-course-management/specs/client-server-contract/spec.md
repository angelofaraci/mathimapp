# Delta for client-server-contract

## ADDED Requirements

### Requirement: Nullable Lesson Course ID

The shared `Lesson` contract MUST allow `courseId` to be nullable (`String?`). The client MUST handle both course-linked and standalone lessons when deserializing.

#### Scenario: Client deserializes course-linked lesson

- GIVEN the server returns a Lesson with a non-null `courseId`
- WHEN the client deserializes the response
- THEN the `Lesson.courseId` SHALL contain the course identifier

#### Scenario: Client deserializes standalone lesson

- GIVEN the server returns a Lesson with `courseId = null`
- WHEN the client deserializes the response
- THEN the `Lesson.courseId` SHALL be null without error

#### Scenario: Client serializes lesson with null courseId

- GIVEN the client sends a Lesson with `courseId = null`
- WHEN the payload is serialized
- THEN the JSON SHALL omit or set `courseId` to null

### Requirement: Lesson Creator ID Field

The shared `Lesson` contract MUST include an optional `creatorId: String?` field to identify the owner of standalone lessons.

#### Scenario: Creator ID is present on standalone lesson

- GIVEN the server returns a standalone lesson
- WHEN the client deserializes the response
- THEN the `Lesson.creatorId` SHALL contain the creator's user ID

#### Scenario: Creator ID may be null for course-linked lessons

- GIVEN the server returns a lesson linked to a course
- WHEN the client deserializes the response
- THEN the `Lesson.creatorId` MAY be null
