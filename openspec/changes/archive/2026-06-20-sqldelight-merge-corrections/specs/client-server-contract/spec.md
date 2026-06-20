# Delta for client-server-contract

## ADDED Requirements

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
