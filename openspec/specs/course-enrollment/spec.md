# course-enrollment Specification

## Purpose

Enable learners to enroll in official courses via a backend endpoint and client repository method, replacing the visual-only enrollment button from V1 catalog.

## Requirements

### Requirement: Enrollment Endpoint

The system MUST provide `POST /courses/{id}/enroll` that enrolls the authenticated user in an official course. The endpoint SHALL insert an enrollment record and return updated user progress.

#### Scenario: Successful enrollment in official course

- GIVEN an authenticated student and an official course that exists
- WHEN the student sends `POST /courses/{id}/enroll`
- THEN the system SHALL create an enrollment record and return 200 with updated `UserProgress` including the course ID in `enrolledCourseIds`

#### Scenario: Enrollment in non-existent course

- GIVEN an authenticated student
- WHEN the student sends `POST /courses/{id}/enroll` for a course that does not exist
- THEN the system SHALL return 404 NotFound

#### Scenario: Unauthenticated enrollment attempt

- GIVEN no authenticated user
- WHEN a request is sent to `POST /courses/{id}/enroll`
- THEN the system SHALL return 401 Unauthorized

#### Scenario: Enrollment in non-official course

- GIVEN an authenticated student and a non-official course
- WHEN the student sends `POST /courses/{id}/enroll`
- THEN the system SHALL return 400 BadRequest (non-official courses require a join code, not this endpoint)

#### Scenario: Already-enrolled user

- GIVEN an authenticated student already enrolled in the official course
- WHEN the student sends `POST /courses/{id}/enroll`
- THEN the system SHALL return 200 with current `UserProgress` without duplicating the enrollment record

### Requirement: Client Enrollment Repository

The system MUST expose an `enroll(courseId: String)` method in the `CourseRepository` that calls `POST /courses/{id}/enroll` and returns the updated `UserProgress`.

#### Scenario: Repository calls enrollment endpoint

- GIVEN the client has a valid auth token
- WHEN `enroll(courseId)` is called
- THEN the system SHALL send a POST request to `/courses/{courseId}/enroll` with the Bearer token

#### Scenario: Repository returns updated progress

- GIVEN the enrollment endpoint returns successfully
- WHEN the repository processes the response
- THEN the system SHALL return the `UserProgress` with the course ID added to `enrolledCourseIds`

#### Scenario: Repository propagates network errors

- GIVEN the enrollment endpoint is unreachable
- WHEN `enroll(courseId)` is called
- THEN the system SHALL propagate the error to the caller

### Requirement: Enrollment CTA Replaces Visual-Only Button

The system MUST replace the visual-only "Inscribirse" button on course catalog cards with a functional enrollment action that calls the enrollment endpoint and updates local state.

#### Scenario: Enroll button triggers network call

- GIVEN the user taps "Inscribirse" on a course card in the catalog
- WHEN the tap is processed
- THEN the system SHALL call the enrollment endpoint for that course

#### Scenario: Successful enrollment updates local state

- GIVEN the enrollment call succeeds
- WHEN the response is received
- THEN the system SHALL update the local progress state to include the course in `enrolledCourseIds`

#### Scenario: Enrollment failure shows error

- GIVEN the enrollment call fails
- WHEN the error is received
- THEN the system SHALL display an error message to the user
