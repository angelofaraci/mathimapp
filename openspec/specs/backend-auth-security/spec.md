# backend-auth-security Specification

## Purpose

Protect server routes with authenticated identity and role rules, and keep sensitive credentials out of source and logs.

## Compatibility

During the role rename transition, the system MUST treat legacy `LEARNER` and canonical `STUDENT` values as equivalent when reading persisted user rows, local cache rows, and JWT role claims. New writes and newly issued JWT claims MUST emit `STUDENT`.

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

The system MUST allow public registration to create only STUDENT or TEACHER users and MUST NOT create ADMIN users from client-supplied registration data.

#### Scenario: Student or teacher registration succeeds

- GIVEN a public registration request for STUDENT or TEACHER
- WHEN the request is valid
- THEN the system SHALL create the user with the requested non-admin role

#### Scenario: Admin registration is blocked

- GIVEN a public registration request that asks for ADMIN
- WHEN the request is processed
- THEN the system SHALL not create an ADMIN user from that request

### Requirement: Protected Course And Progress Access

The system MUST require a logged-in user for course reads and course actions, MUST allow students to view only their own progress, and MUST allow admins to view all progress.

#### Scenario: Unauthenticated course access is denied

- GIVEN a request to read or modify course data without authentication
- WHEN the system evaluates the request
- THEN the system SHALL reject it with an authentication failure

#### Scenario: Progress visibility follows role

- GIVEN an authenticated student or admin
- WHEN the user requests progress data
- THEN the system SHALL return only the student's own progress or all progress for admin access

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

### Requirement: Student Responses Hide Correct Answers

The system MUST omit correct answers from normal student-facing content endpoints.

#### Scenario: Student response hides answers

- GIVEN a student request for exercise content
- WHEN the system returns the payload
- THEN the system SHALL exclude the correct answer from the response

#### Scenario: Hidden answers do not break content delivery

- GIVEN an exercise with a correct answer stored server-side
- WHEN a student fetches the content
- THEN the system SHALL still return the exercise content without revealing the answer

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

### Requirement: Lesson Mutation Authorization

The system MUST allow ADMIN users to create, update, and delete any lesson. TEACHER users MAY create course-linked lessons inside courses they own, MAY update or delete course-linked lessons whose parent course they own, and MAY create, update, and delete standalone lessons where `lesson.creatorId == userId`.

#### Scenario: Admin mutates any lesson

- GIVEN an authenticated ADMIN user
- WHEN the user creates, updates, or deletes any lesson
- THEN the system SHALL allow the operation

#### Scenario: Teacher mutates own standalone lesson

- GIVEN an authenticated TEACHER user
- AND a standalone lesson with `creatorId` matching the TEACHER's ID
- WHEN the TEACHER updates or deletes that lesson
- THEN the system SHALL allow the operation

#### Scenario: Teacher mutates own course-linked lesson

- GIVEN an authenticated TEACHER user
- AND a course-linked lesson whose parent course creator matches the TEACHER's ID
- WHEN the TEACHER creates, updates, or deletes that lesson
- THEN the system SHALL allow the operation

#### Scenario: Teacher cannot mutate another's standalone lesson

- GIVEN an authenticated TEACHER user
- AND a standalone lesson with a different `creatorId`
- WHEN the TEACHER attempts to update or delete it
- THEN the system SHALL reject with 403

#### Scenario: Student cannot mutate lessons

- GIVEN an authenticated STUDENT user
- WHEN the user attempts to create, update, or delete a lesson
- THEN the system SHALL reject with 403

### Requirement: Exercise Mutation Ownership via Lesson

The system MUST authorize exercise mutations based on the parent lesson's access rules. ADMIN users MAY mutate any exercise. TEACHER users MAY mutate exercises only when they have write access to the parent lesson.

#### Scenario: Admin mutates any exercise

- GIVEN an authenticated ADMIN user
- WHEN the user creates, updates, or deletes any exercise
- THEN the system SHALL allow the operation

#### Scenario: Teacher mutates exercise in own standalone lesson

- GIVEN a TEACHER owns a standalone lesson
- WHEN the TEACHER creates or updates an exercise in that lesson
- THEN the system SHALL allow the operation

#### Scenario: Teacher denied exercise mutation in another's lesson

- GIVEN a TEACHER who does not own the parent lesson
- WHEN the TEACHER attempts to mutate an exercise in that lesson
- THEN the system SHALL reject with 403
