# Delta for backend-auth-security

## ADDED Requirements

### Requirement: Theory Mutation Authorization

The system MUST require authenticated access for lesson theory updates and MUST allow only ADMIN users to update theory for official lessons while TEACHER users MAY update theory only for lessons in courses they created.

#### Scenario: Admin updates official lesson theory

- GIVEN an authenticated ADMIN user
- WHEN the user updates theory for a lesson in an official course
- THEN the system SHALL accept the update

#### Scenario: Teacher is limited to own courses

- GIVEN an authenticated TEACHER user
- WHEN the user updates theory for a lesson in another teacher's course
- THEN the system SHALL reject the request with 403

#### Scenario: Missing authentication is rejected

- GIVEN no valid JWT is present
- WHEN the request targets the theory update route
- THEN the system SHALL reject it with 401
