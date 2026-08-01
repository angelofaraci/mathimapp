# Delta for onboarding-flow

## MODIFIED Requirements

### Requirement: Mandatory Onboarding Gate

The system MUST display the onboarding flow after successful registration and MUST prevent access to `CourseScreen` until all onboarding steps are completed. The gate MUST evaluate onboarding completion for the currently authenticated user's `userId`, so a different account authenticating on the same device is never treated as onboarded based on another account's completion state. If the session's user id is missing or blank at gate-evaluation time, the system MUST treat the user as not onboarded and show the onboarding flow.
(Previously: the gate evaluated a single global onboarding-completion flag with no `userId` scoping.)

#### Scenario: Registration redirects to onboarding

- GIVEN the user completes registration successfully
- WHEN the auth session is established
- THEN the system SHALL navigate to the onboarding flow
- AND the system SHALL NOT show `CourseScreen`

#### Scenario: Incomplete onboarding blocks course access

- GIVEN the user has not completed onboarding
- WHEN the app attempts to resolve the post-auth view
- THEN the system SHALL display the onboarding flow
- AND the system SHALL NOT allow navigation to `CourseScreen`

#### Scenario: A different account on the same device is not treated as onboarded

- GIVEN account A previously completed onboarding on this device
- AND account B has never completed onboarding
- WHEN account B logs in on the same device
- THEN the system SHALL evaluate onboarding completion for account B's `userId`
- AND the system SHALL display the full onboarding wizard for account B
- AND the system SHALL NOT skip onboarding based on account A's completed state

#### Scenario: Returning to a previously onboarded account does not repeat onboarding

- GIVEN account A previously completed onboarding on this device
- WHEN account A logs back in on the same device
- THEN the system SHALL evaluate onboarding completion for account A's `userId`
- AND the system SHALL navigate directly to `CourseScreen`
- AND the system SHALL NOT re-display the onboarding flow

#### Scenario: A fresh registration always runs onboarding, even after a prior completed profile

- GIVEN a device has a prior completed profile belonging to account A
- WHEN a brand-new account is registered on that device
- THEN the system SHALL evaluate onboarding completion for the new account's `userId`
- AND the system SHALL display the full onboarding wizard for the new account

#### Scenario: Missing session user id fails toward showing onboarding

- GIVEN the auth session has no user id, or the user id is blank, at gate-evaluation time
- WHEN the app attempts to resolve the post-auth view
- THEN the system SHALL treat the user as not onboarded
- AND the system SHALL display the onboarding flow
- AND the system SHALL NOT navigate to `CourseScreen`
