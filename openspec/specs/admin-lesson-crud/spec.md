# admin-lesson-crud Specification

## Purpose

Define admin-only CRUD operations for lessons. Lessons MAY belong to a course (`courseId` is nullable) or exist standalone. Standalone lessons MUST have a `creatorId` to track ownership.

## Requirements

### Requirement: Admin Lesson Creation

The system MUST allow ADMIN users to create lessons with a title, optional `courseId`, and optional `creatorId`. When `courseId` is null, the persisted lesson MUST have a `creatorId`; if the request omits it, the system SHALL default it to the authenticated ADMIN user ID.

#### Scenario: Admin creates a course-linked lesson

- GIVEN an existing course
- WHEN an ADMIN creates a lesson with a valid `courseId`
- THEN the system SHALL create the lesson linked to that course

#### Scenario: Admin creates a standalone lesson

- GIVEN no course is specified
- WHEN an ADMIN creates a lesson without `courseId`
- THEN the system SHALL create the lesson with `courseId = null` and set `creatorId` to the ADMIN user ID

#### Scenario: Standalone lesson defaults creator when omitted

- GIVEN a lesson creation request with `courseId = null`
- WHEN the request omits `creatorId`
- THEN the system SHALL create the lesson successfully
- AND the stored `creatorId` SHALL default to the authenticated ADMIN user ID

#### Scenario: Link to non-existent course is rejected

- GIVEN a lesson creation request with a `courseId` that does not exist
- WHEN the request is processed
- THEN the system SHALL reject the request with 400

### Requirement: Admin Lesson Update

The system MUST allow ADMIN users to update lesson fields: title, `courseId` (including reassignment and unassignment), and `creatorId` for standalone lessons.

#### Scenario: Admin reassigns lesson to a different course

- GIVEN a lesson currently linked to course A
- WHEN an ADMIN updates the lesson with `courseId` pointing to course B
- THEN the system SHALL reassign the lesson to course B

#### Scenario: Admin unassigns lesson from its course

- GIVEN a lesson linked to a course
- WHEN an ADMIN updates the lesson setting `courseId = null`
- THEN the system SHALL detach the lesson from its course
- AND the lesson SHALL become a standalone lesson with its existing `creatorId`

#### Scenario: Admin assigns standalone lesson to a course

- GIVEN a standalone lesson (`courseId = null`)
- WHEN an ADMIN updates the lesson with a valid `courseId`
- THEN the system SHALL link the lesson to the specified course

### Requirement: Admin Lesson Deletion

The system MUST allow ADMIN users to delete lessons. Deletion MUST cascade to exercises and progress per the `database-integrity` spec.

#### Scenario: Admin deletes a lesson

- GIVEN a lesson with exercises
- WHEN an ADMIN deletes the lesson
- THEN the system SHALL delete the lesson and all its exercises

#### Scenario: Delete non-existent lesson returns 404

- GIVEN no lesson exists with the specified ID
- WHEN an ADMIN attempts to delete it
- THEN the system SHALL return 404

### Requirement: Admin Lesson Listing

The system MUST provide admin-only endpoints to list all lessons, optionally filtered by `courseId`.

#### Scenario: Admin lists all lessons

- WHEN an ADMIN requests the full lesson list
- THEN the system SHALL return all lessons regardless of course association

#### Scenario: Admin lists lessons for a specific course

- GIVEN a course with multiple lessons
- WHEN an ADMIN requests lessons filtered by `courseId`
- THEN the system SHALL return only lessons belonging to that course

#### Scenario: Admin lists standalone lessons

- WHEN an ADMIN requests lessons with the standalone filter (`courseId` query present but empty)
- THEN the system SHALL return only standalone lessons
