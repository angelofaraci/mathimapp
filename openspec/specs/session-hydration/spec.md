# session-hydration Specification

## Purpose

Ensure that when the app restores a valid authenticated session after restart, it also recovers enough current-user information for screens like course detail, home dashboard, and profile to load correctly without forcing re-login.

## Requirements

### Requirement: Session Restore Hydrates Current User

The system MUST recover the authenticated user's identity and profile data alongside the persisted token when restoring a session after app restart. The system SHALL NOT require re-authentication if a valid token exists but user data is missing.

#### Scenario: Valid token with missing user triggers hydration

- GIVEN the app restarts with a persisted valid auth token
- AND the current user identity is not available in memory
- WHEN the session initialization flow runs
- THEN the system SHALL call a user-info endpoint using the persisted token
- AND the system SHALL populate the current user in the auth session state

#### Scenario: Hydration succeeds and app enters authenticated flow

- GIVEN the app restarts with a valid token
- WHEN the user-info endpoint returns successfully
- THEN the system SHALL store the user in the auth session
- AND the system SHALL navigate to the post-auth screen (onboarding or home dashboard)

#### Scenario: Hydration fails with invalid token triggers re-login

- GIVEN the app restarts with a persisted token
- WHEN the user-info endpoint returns 401 Unauthorized
- THEN the system SHALL discard the persisted token
- AND the system SHALL navigate to the Login screen

#### Scenario: Hydration fails with network error shows retry

- GIVEN the app restarts with a valid token
- WHEN the user-info endpoint is unreachable
- THEN the system SHALL display a loading error with a retry option
- AND the system SHALL NOT navigate to the Login screen immediately

### Requirement: Hydration Must Complete Before Screen Data Fetches

The system MUST ensure user hydration completes before any screen (course detail, home, profile) attempts to fetch user-scoped data. Screens SHALL NOT proceed with a null user identity.

#### Scenario: Course detail waits for hydration

- GIVEN the app restores a session and the user opens the ACTIVITIES tab
- WHEN the course detail screen attempts to load
- THEN the system SHALL wait for user hydration to complete before fetching course progress

#### Scenario: Home dashboard waits for hydration

- GIVEN the app restores a session
- WHEN the home dashboard attempts to load
- THEN the system SHALL wait for user hydration to complete before fetching enrolled courses

### Requirement: Hydration Is Idempotent

The system SHALL NOT re-hydrate the user if the current auth session already contains valid user data.

#### Scenario: Already-hydrated session skips hydration

- GIVEN the app has a valid auth session with user data already populated
- WHEN the session initialization flow runs
- THEN the system SHALL NOT call the user-info endpoint
- AND the system SHALL proceed directly to the post-auth screen
