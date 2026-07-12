# Delta for frontend-auth

## ADDED Requirements

### Requirement: Login Screen UX

The system MUST render the Login screen with Spanish copy, brand hero, field icons, password visibility toggle, email-format validation, forgot-password link, and visual-only Google/Apple social buttons (MUST NOT trigger OAuth). A footer link MUST navigate to registration.

#### Scenario: Password visibility toggle

- WHEN the user selects the visibility toggle on a populated password field
- THEN the field SHALL switch between masked and plain text

#### Scenario: Email-format validation

- WHEN the user enters non-email-format text in the email field
- THEN the system SHALL display a validation error and disable the login button

#### Scenario: Social buttons are non-functional

- WHEN the user selects Google or Apple button on the Login screen
- THEN the system SHALL NOT initiate OAuth or navigate away

#### Scenario: Forgot-password link

- WHEN the user selects the forgot-password link on the Login screen
- THEN the system SHALL navigate to password recovery (or placeholder)

### Requirement: Register Screen 3-Step Wizard

The system MUST render the Register screen as a 3-step wizard with step indicators, back navigation, per-step validation, password visibility toggle, password strength indicator, and explicit terms acceptance. All existing registration data fields MUST be collected and submitted to the backend unchanged.

#### Scenario: Step progression with valid input

- WHEN the user selects continue on step N with all fields valid
- THEN the system SHALL advance to step N+1 and update the indicator

#### Scenario: Back from step 1 to login

- WHEN the user selects back on step 1
- THEN the system SHALL navigate to Login and clear registration state

#### Scenario: Back from steps 2-3

- WHEN the user selects back on step 2 or 3
- THEN the system SHALL return to the previous step with data preserved

#### Scenario: Password strength indicator

- WHEN the user types into the password field
- THEN the system SHALL display a 3-segment strength meter based on length and variety

#### Scenario: Terms acceptance required

- WHEN the user is on the final step with terms unchecked
- THEN the submit button SHALL be disabled and SHALL NOT submit

#### Scenario: Per-step validation blocks progression

- WHEN the user selects continue with incomplete/invalid fields
- THEN the system SHALL display validation errors and SHALL NOT advance

### Requirement: Auth Screen Primitives

`MTextField` MUST support focus glow, 15px corner radius, and leading/trailing icons. `MButton` MUST support disabled state opacity and social-button visual style.

#### Scenario: MTextField focus glow

- GIVEN an `MTextField` receives focus
- THEN the field SHALL display a focus glow and change border color

#### Scenario: MTextField trailing icon

- GIVEN an `MTextField` has a trailing icon configured
- WHEN rendered
- THEN the icon SHALL appear inside the field at the right edge

#### Scenario: MButton disabled state

- GIVEN an `MButton` is disabled
- WHEN rendered
- THEN the button SHALL display at ~0.5 opacity and SHALL NOT respond to tap

## MODIFIED Requirements

### Requirement: Auth Entry Flow

The system MUST show the Login screen by default with Spanish copy and brand hero. Switching between Login and Register SHALL occur via a footer link on Login and back navigation from step 1 of the Register wizard.
(Previously: The system showed Login screen by default and allowed switching via in-screen text links.)

#### Scenario: Default state is login

- GIVEN the app starts with no in-memory token
- WHEN the auth area is rendered
- THEN the Login screen SHALL be visible with Spanish copy and brand hero

#### Scenario: Footer link switches to register

- GIVEN the Login screen is visible
- WHEN the user selects the register footer link
- THEN the Register screen SHALL replace it at step 1

## REMOVED Requirements

None.

## RENAMED Requirements

None.
