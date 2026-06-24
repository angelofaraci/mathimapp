# Delta for client-server-contract

## ADDED Requirements

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
