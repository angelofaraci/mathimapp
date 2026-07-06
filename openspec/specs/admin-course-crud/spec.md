# admin-course-crud Specification

## Purpose

Define admin-only CRUD operations for courses in the admin-web panel. Admins can create, list, update, and delete courses. This capability is separate from the read-only course overview currently in the admin panel.

## Requirements

### Requirement: Admin Course Creation

The system MUST allow ADMIN users to create new courses with a title, description, optional `isOfficial` flag, and optional `schoolYear`.

#### Scenario: Admin creates a course successfully

- GIVEN an authenticated ADMIN user
- WHEN the user submits a valid course creation form with title and description
- THEN the system SHALL create the course and return the created course payload
- AND the course SHALL appear in the admin course list

#### Scenario: Non-admin cannot create courses

- GIVEN an authenticated TEACHER or STUDENT user
- WHEN the user attempts to create a course via admin endpoints
- THEN the system SHALL reject the request with 403

#### Scenario: Missing required fields are rejected

- GIVEN an ADMIN user submits a course creation request
- WHEN the request lacks a course title
- THEN the system SHALL reject the request with 400 and indicate the missing field

### Requirement: Admin Course Update

The system MUST allow ADMIN users to update existing course fields: title, description, `isOfficial`, and `schoolYear`.

#### Scenario: Admin updates course fields

- GIVEN an existing course
- WHEN an ADMIN submits valid update data
- THEN the system SHALL persist the changes and return the updated course

#### Scenario: Update of non-existent course is rejected

- GIVEN no course exists with the specified ID
- WHEN an ADMIN attempts to update it
- THEN the system SHALL return 404

### Requirement: Admin Course Deletion

The system MUST allow ADMIN users to delete courses. Deletion MUST cascade to lessons, exercises, enrollments, and progress per the `database-integrity` spec.

#### Scenario: Admin deletes a course

- GIVEN a course with no active enrollments
- WHEN an ADMIN deletes the course
- THEN the system SHALL remove the course and all dependent records

#### Scenario: Delete cascades to lessons and exercises

- GIVEN a course with lessons and exercises
- WHEN an ADMIN deletes the course
- THEN the system SHALL delete all associated lessons and exercises

### Requirement: Admin Course Listing

The system MUST provide an admin-only endpoint to list all courses for this phase. Pagination MAY be added in a later phase, but it is out of scope for this change.

#### Scenario: Admin lists all courses

- GIVEN multiple courses exist
- WHEN an ADMIN requests the course list
- THEN the system SHALL return all courses with basic metadata
