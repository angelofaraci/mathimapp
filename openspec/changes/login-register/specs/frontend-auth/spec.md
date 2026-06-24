# frontend-auth Specification

## Purpose

Provide login/register entry points for public users, keep the active session in memory, and route authenticated users to `CourseScreen`.

## Requirements

### Requirement: Auth Entry Flow

The system MUST show the Login screen by default and MUST allow switching between Login and Register screens with in-screen text links.

#### Scenario: Default state is login

- GIVEN the app starts with no in-memory token
- WHEN the auth area is rendered
- THEN the Login screen SHALL be visible
- AND the Register screen SHALL NOT be shown

#### Scenario: Text links switch forms

- GIVEN the Login screen is visible
- WHEN the user selects the register link
- THEN the Register screen SHALL replace it

### Requirement: Public Registration Uses Student Role

The system MUST NOT expose role selection during public registration and MUST create new public accounts as `STUDENT` users only.

#### Scenario: Register form has no role picker

- GIVEN the user opens the Register screen
- WHEN the form is displayed
- THEN the system SHALL not ask the user to choose a role

#### Scenario: Successful registration creates a student account

- GIVEN the user submits valid registration data
- WHEN the server returns success
- THEN the created account SHALL be treated as `STUDENT`
- AND the system SHALL continue with the authenticated flow

### Requirement: Successful Authentication Enters the App

The system MUST send login and registration requests through the auth API, MUST store the returned token in memory for the current app process, and MUST show `CourseScreen` after success.

#### Scenario: New user registers successfully

- GIVEN the user submits valid registration data
- WHEN the server returns an auth response
- THEN the system SHALL store the token in memory
- AND the system SHALL show `CourseScreen`

#### Scenario: Login success resumes course view

- GIVEN the user submits valid login credentials
- WHEN the server returns an auth response
- THEN the system SHALL store the token in memory
- AND the system SHALL show `CourseScreen`

### Requirement: Raw Auth Errors Are Visible

The system MUST surface raw server error text for failed login or registration attempts.

#### Scenario: Duplicate email is shown

- GIVEN the server rejects registration with an error body
- WHEN the response is handled
- THEN the system SHALL display that text to the user
