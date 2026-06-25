# Delta for backend-auth-security

## ADDED Requirements

### Requirement: Admin Route Guard

The system MUST enforce a strict ADMIN-only gate on all `/admin/*` routes. `requireAdmin()` MUST reject non-ADMIN tokens with 403 and MUST allow ADMIN tokens to proceed.

#### Scenario: Admin request succeeds

- GIVEN a request with a valid JWT containing role ADMIN
- WHEN the request targets an `/admin/*` route
- THEN the system SHALL allow the request

#### Scenario: Non-admin request is rejected

- GIVEN a request with a valid JWT containing role STUDENT or TEACHER
- WHEN the request targets an `/admin/*` route
- THEN the system SHALL reject with 403
