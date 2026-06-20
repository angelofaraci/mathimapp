# Delta for client-server-contract

## ADDED Requirements

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
