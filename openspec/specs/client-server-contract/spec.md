# client-server-contract Specification

## Purpose

Keep the client request shape aligned with the shared server contract and ensure authenticated requests carry the access token.

## Requirements

### Requirement: Shared Lesson Completion Contract

The system MUST define lesson completion requests in the shared contract and MUST use that shared shape from the client when submitting completion data.

#### Scenario: Shared request shape is used

- GIVEN the client submits a lesson completion request
- WHEN the request is serialized
- THEN the system SHALL use the shared CompleteLessonRequest contract

#### Scenario: Completion data reaches the server

- GIVEN a valid lesson completion payload
- WHEN the client sends it to the backend
- THEN the system SHALL preserve the expected fields and values

### Requirement: Authorization Header Injection

The system MUST attach a Bearer Authorization header to authenticated requests when a token is available.

#### Scenario: Token is present

- GIVEN the client has a stored access token
- WHEN it sends a network request
- THEN the system SHALL add an Authorization header with that token

#### Scenario: Token is absent

- GIVEN the client has no stored token
- WHEN it sends a network request
- THEN the system SHALL not invent an Authorization header

### Requirement: Memory-Only Token Storage

The system MAY keep the access token in memory only and SHALL NOT require persistent token storage for this slice.

#### Scenario: Session token is usable in memory

- GIVEN the client has received a token during the current session
- WHEN later requests are sent in the same session
- THEN the system SHALL reuse the token from memory

#### Scenario: App restart clears the token

- GIVEN the app restarts
- WHEN no persisted token exists
- THEN the system SHALL treat the client as logged out until a new token is obtained

### Requirement: Shared Course School Year Field

The shared Course contract MUST include schoolYear and the client MUST preserve it when serializing or deserializing course data.

#### Scenario: Course carries school year

- GIVEN the server returns a Course with schoolYear
- WHEN the client parses the response
- THEN the Course SHALL retain the same schoolYear value

#### Scenario: Course data keeps school year through the client

- GIVEN the client sends or receives course data
- WHEN the contract is serialized
- THEN schoolYear SHALL remain part of the payload

### Requirement: Shared Theory Update Request

The shared contract MUST define a theory update request for lesson theory changes and the client MUST use that shared shape when sending updates.

#### Scenario: Theory update request is shared

- GIVEN a user submits theory content for a lesson
- WHEN the client serializes the request
- THEN the payload SHALL match the shared theory update contract

#### Scenario: Server receives theory content unchanged

- GIVEN a valid theory update request
- WHEN the server deserializes it
- THEN the lesson id and theory content SHALL remain intact

### Requirement: Shared Auth DTO Contracts

The system MUST define `RegisterRequest`, `LoginRequest`, and `AuthResponse` in the shared contract and MUST use those shared types for auth requests and responses on both client and server.

#### Scenario: Registration request uses shared type

- GIVEN the client prepares a registration call
- WHEN the payload is serialized
- THEN the system SHALL use the shared `RegisterRequest` shape

#### Scenario: Login response is shared

- GIVEN the server returns a successful login response
- WHEN the client deserializes it
- THEN the system SHALL use the shared `AuthResponse` shape
- AND preserve the token and user data

### Requirement: Shared Course Discovery Fields

The shared `Course` contract MUST include `topic: String?`, `difficulty: String?`, `durationMinutes: Int?`, and `xpReward: Int?` fields. These fields SHALL be serializable and deserializable by both client and server using the existing `@Serializable` mechanism.

#### Scenario: Server serializes discovery fields

- GIVEN the server returns a Course with topic "Fracciones", difficulty "Fácil", durationMinutes 15, xpReward 50
- WHEN the response is serialized
- THEN the JSON payload SHALL contain `topic`, `difficulty`, `durationMinutes`, and `xpReward`

#### Scenario: Client deserializes discovery fields

- GIVEN the client receives a Course JSON payload with discovery fields
- WHEN the client deserializes the response
- THEN the `Course` object SHALL retain all discovery field values

#### Scenario: Null discovery fields are handled

- GIVEN the server returns a Course with null discovery fields
- WHEN the client deserializes the response
- THEN the `Course` object SHALL have null values for those fields without error
