# Delta for frontend-auth

## MODIFIED Requirements

### Requirement: Successful Authentication Enters the App

The system MUST send login and registration requests through the auth API, MUST store the returned token in memory for the current app process, and MUST show the onboarding flow after registration if onboarding is not complete, or `HomeDashboardScreen` if onboarding is already complete.
(Previously: After successful authentication, the system showed `CourseScreen` directly if onboarding was complete.)

#### Scenario: New user registers and must complete onboarding

- GIVEN the user submits valid registration data
- WHEN the server returns an auth response
- THEN the system SHALL store the token in memory
- AND the system SHALL check if onboarding is complete
- AND if onboarding is NOT complete, the system SHALL show the onboarding flow
- AND the system SHALL NOT show `HomeDashboardScreen` until onboarding completes

#### Scenario: Returning user with completed onboarding enters dashboard

- GIVEN the user has a valid auth session
- AND onboarding was previously completed
- WHEN the app resolves the post-auth view
- THEN the system SHALL show `HomeDashboardScreen` directly
- AND the system SHALL NOT show the onboarding flow

#### Scenario: Login success with incomplete onboarding

- GIVEN the user logs in successfully
- AND onboarding is not complete for this session
- WHEN the auth response is received
- THEN the system SHALL store the token in memory
- AND the system SHALL show the onboarding flow
