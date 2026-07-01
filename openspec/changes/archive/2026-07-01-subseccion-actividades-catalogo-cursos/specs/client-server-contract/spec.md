# Delta for client-server-contract

## ADDED Requirements

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
