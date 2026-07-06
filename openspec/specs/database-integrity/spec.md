# database-integrity Specification

## Purpose

Preserve relational correctness in the database schema and define deletion behavior for parent-child records.

## Requirements

### Requirement: Foreign Key Relationships

The system MUST model course, lesson, exercise, enrollment, and progress relationships with foreign key references instead of unconstrained identifier columns.

#### Scenario: Valid references are accepted

- GIVEN a child record points to an existing parent record
- WHEN the record is inserted or queried
- THEN the system SHALL preserve the relationship

#### Scenario: Unrelated identifiers are not treated as valid relationships

- GIVEN an identifier that does not match an existing parent
- WHEN the schema validates the relation
- THEN the system SHALL not treat it as a valid linked record

### Requirement: Hierarchical Delete Cascade

The system MUST cascade deletions from course to lessons, exercises, enrollments, and progress; from lesson to exercises and progress; and from deleted students to progress and enrollments.

#### Scenario: Course deletion removes dependents

- GIVEN a course with lessons, exercises, enrollments, and progress
- WHEN the course is deleted
- THEN the system SHALL delete the dependent records

#### Scenario: Student deletion removes student data only

- GIVEN a student with progress and enrollments
- WHEN the student is deleted
- THEN the system SHALL delete the student's progress and enrollments

### Requirement: Teacher Deletion Does Not Remove Courses

The system MUST NOT cascade-delete teacher-owned courses when a teacher is deleted in this slice.

#### Scenario: Teacher deletion preserves courses

- GIVEN a teacher owns one or more courses
- WHEN the teacher record is deleted
- THEN the system SHALL retain the courses for this release slice

#### Scenario: Teacher deletion does not break course records

- GIVEN retained courses that reference the deleted teacher
- WHEN the database processes the deletion
- THEN the system SHALL keep the course records intact

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
