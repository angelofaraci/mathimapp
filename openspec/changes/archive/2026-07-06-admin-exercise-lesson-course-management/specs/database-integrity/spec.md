# Delta for database-integrity

## ADDED Requirements

### Requirement: Nullable Course ID Foreign Key

The system MUST allow `Lessons.course_id` to be nullable in the database schema. When `course_id` is not null, it MUST reference a valid `Courses.id`.

#### Scenario: Insert lesson with null course_id

- WHEN a lesson is inserted with `course_id = null`
- THEN the system SHALL accept the row without a foreign key violation

#### Scenario: Insert lesson with valid course_id

- WHEN a lesson is inserted with `course_id` pointing to an existing course
- THEN the system SHALL accept the row with the foreign key constraint

#### Scenario: Insert lesson with invalid course_id

- WHEN a lesson is inserted with `course_id` pointing to a non-existent course
- THEN the system SHALL reject with a foreign key violation

### Requirement: Lesson Creator ID Column

The system MUST add a `creator_id` column to the `Lessons` table. This column MAY be nullable for course-linked lessons but MUST be set for standalone lessons.

#### Scenario: Standalone lesson has creator

- WHEN a lesson is inserted with `course_id = null`
- THEN the system SHALL require `creator_id` to be non-null

### Requirement: Cascade Behavior for Unassigned Lessons

The system MUST NOT cascade-delete standalone lessons when no course is involved. Standalone lessons are only deleted when explicitly deleted or when their creator is deleted (per teacher-deletion rules).

#### Scenario: Course deletion does not affect standalone lessons

- GIVEN a course is deleted
- WHEN the cascade processes
- THEN the system SHALL NOT delete lessons where `course_id = null`

#### Scenario: Explicit lesson deletion cascades to exercises

- GIVEN a standalone lesson with exercises
- WHEN the lesson is explicitly deleted
- THEN the system SHALL delete the exercises linked to that lesson
