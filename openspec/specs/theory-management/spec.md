# theory-management Specification

## Purpose

Control loading and updating of lesson theory content with role-scoped access.

## Requirements

### Requirement: Theory Content Read Access

The system MUST allow authenticated users to load lesson theory content for lessons they can access.

#### Scenario: Lesson theory is returned

- GIVEN an authenticated user requests a lesson they can view
- WHEN the lesson payload is returned
- THEN the response SHALL include the lesson theory content

#### Scenario: Inaccessible lesson is blocked

- GIVEN an authenticated user requests a lesson they cannot access
- WHEN the request is processed
- THEN the system SHALL reject the request

### Requirement: Theory Content Update Scope

The system MUST allow ADMIN users to update theory for any lesson in an official course and MUST allow TEACHER users to update theory only for lessons in courses they created.

#### Scenario: Admin updates official lesson theory

- GIVEN an ADMIN requests a theory update for an official lesson
- WHEN the update is valid
- THEN the system SHALL store the new theory content

#### Scenario: Teacher is limited to own courses

- GIVEN a TEACHER requests a theory update for a lesson in a course they created
- WHEN the update is valid
- THEN the system SHALL store the new theory content
- AND when the lesson belongs to another teacher's course, the system SHALL reject the update
