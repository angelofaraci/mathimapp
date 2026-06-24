# auth-logout Specification

## Purpose

Allow the authenticated user to end the current session and return to the login flow.

## Requirements

### Requirement: Logout Ends the Session

The system MUST clear the in-memory auth token when the user logs out and MUST return to the Login screen.

#### Scenario: Logged-in user logs out

- GIVEN the user is authenticated
- WHEN the user selects logout
- THEN the in-memory token SHALL be cleared
- AND the Login screen SHALL be shown

#### Scenario: Logout without a session is safe

- GIVEN no token is stored
- WHEN logout is requested
- THEN the system SHALL remain on the auth flow without error
