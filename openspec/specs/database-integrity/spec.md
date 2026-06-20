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
