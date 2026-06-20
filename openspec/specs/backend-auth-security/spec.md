# backend-auth-security Specification

## Purpose

Protect server routes with authenticated identity and role rules, and keep sensitive credentials out of source and logs.

## Requirements

### Requirement: JWT Protected Access

The system MUST verify the authenticated user's JWT identity on protected routes and MUST reject requests that lack a valid identity or required role.

#### Scenario: Authorized request succeeds

- GIVEN a request with a valid JWT for the current user
- WHEN the request targets a protected route allowed for that role
- THEN the system SHALL process the request

#### Scenario: Invalid identity is rejected

- GIVEN a request with a missing, invalid, or mismatched JWT claim
- WHEN the request targets a protected route
- THEN the system SHALL reject the request with 401 or 403, depending on whether identity or authorization failed

### Requirement: Registration Role Limits

The system MUST allow public registration to create only LEARNER or TEACHER users in current code and MUST NOT create ADMIN users from client-supplied registration data.

#### Scenario: Learner or teacher registration succeeds

- GIVEN a public registration request for LEARNER or TEACHER
- WHEN the request is valid
- THEN the system SHALL create the user with the requested non-admin role

#### Scenario: Admin registration is blocked

- GIVEN a public registration request that asks for ADMIN
- WHEN the request is processed
- THEN the system SHALL not create an ADMIN user from that request

### Requirement: Protected Course And Progress Access

The system MUST require a logged-in user for course reads and course actions, MUST allow learners to view only their own progress, and MUST allow admins to view all progress.

#### Scenario: Unauthenticated course access is denied

- GIVEN a request to read or modify course data without authentication
- WHEN the system evaluates the request
- THEN the system SHALL reject it with an authentication failure

#### Scenario: Progress visibility follows role

- GIVEN an authenticated learner or admin
- WHEN the user requests progress data
- THEN the system SHALL return only the learner's own progress or all progress for admin access

#### Scenario: Teacher-scoped progress access is not in scope

- GIVEN an authenticated teacher who owns a course
- WHEN the teacher requests student progress for that course
- THEN the system SHALL not grant teacher-scoped progress visibility in this slice

### Requirement: Secure Secret and Seed Handling

The system MUST read the JWT secret from environment or configuration and MUST source automatic admin seed credentials from environment or configuration without logging reusable secrets.

#### Scenario: Secrets come from configuration

- GIVEN the server starts with configured secret and seed credentials
- WHEN initialization runs
- THEN the system SHALL use those values instead of hardcoded secrets

#### Scenario: Secrets are not exposed in logs

- GIVEN admin seed credentials are loaded
- WHEN the seed runs
- THEN the system SHALL not print reusable credentials or secrets

### Requirement: Learner Responses Hide Correct Answers

The system MUST omit correct answers from normal learner-facing content endpoints.

#### Scenario: Learner response hides answers

- GIVEN a learner request for exercise content
- WHEN the system returns the payload
- THEN the system SHALL exclude the correct answer from the response

#### Scenario: Hidden answers do not break content delivery

- GIVEN an exercise with a correct answer stored server-side
- WHEN a learner fetches the content
- THEN the system SHALL still return the exercise content without revealing the answer
