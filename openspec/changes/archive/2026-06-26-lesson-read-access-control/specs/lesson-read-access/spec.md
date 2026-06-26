# lesson-read-access Specification

## Purpose

Control who can read lesson content via GET `/lessons/{id}` and GET `/courses/{courseId}/lessons` based on role and enrollment.

## Requirements

### Requirement: Lesson Read Visibility Tiers

The system SHALL enforce four visibility tiers when a user requests lesson content:

| Tier | Who Can Read | Condition |
|------|-------------|-----------|
| **official** | Any authenticated user | Course `isOfficial = true` |
| **enrolled** | Enrolled students | User enrolled in the course |
| **owner** | Course creator (TEACHER) | `course.creatorId == userId` |
| **admin** | Any ADMIN user | Role is `ADMIN` |

#### Scenario: Admin reads any lesson

- GIVEN a lesson exists in any course
- WHEN an ADMIN requests the lesson
- THEN the lesson is returned with exercises (answers hidden)

#### Scenario: Teacher reads own course lessons

- GIVEN a lesson belongs to a course created by the TEACHER
- WHEN the TEACHER requests the lesson
- THEN the lesson is returned with exercises (answers visible)

#### Scenario: Student reads official course lesson

- GIVEN a lesson belongs to an official course
- WHEN any authenticated STUDENT requests the lesson
- THEN the lesson is returned with exercises (answers hidden)

#### Scenario: Enrolled student reads private course lesson

- GIVEN a lesson belongs to a non-official course
- AND the STUDENT is enrolled in that course
- WHEN the STUDENT requests the lesson
- THEN the lesson is returned with exercises (answers hidden)

#### Scenario: Outsider student denied private lesson

- GIVEN a lesson belongs to a non-official course
- AND the STUDENT is NOT enrolled
- WHEN the STUDENT requests the lesson
- THEN Forbidden is returned

#### Scenario: Other teacher denied private lesson

- GIVEN a lesson belongs to a course created by a different TEACHER
- WHEN a non-creator TEACHER requests the lesson
- THEN Forbidden is returned

#### Scenario: Non-existent lesson returns NotFound

- GIVEN no lesson exists with the requested ID
- WHEN any user requests the lesson
- THEN NotFound is returned

### Requirement: Course Lesson List Read Access

The system SHALL apply the same visibility tiers for GET `/courses/{courseId}/lessons`.

#### Scenario: Enrolled student lists private course lessons

- GIVEN a non-official course with lessons
- AND the STUDENT is enrolled
- WHEN the STUDENT requests the lesson list
- THEN all lessons are returned in order

#### Scenario: Outsider student denied course lesson list

- GIVEN a non-official course
- AND the STUDENT is NOT enrolled
- WHEN the STUDENT requests the lesson list
- THEN Forbidden is returned

### Requirement: Exercise Answers Hidden for Students

The system SHALL hide exercise `correctAnswer` when the requester has role `STUDENT`.

#### Scenario: Student receives blank answers

- GIVEN a lesson with exercises
- WHEN a STUDENT reads the lesson
- THEN each exercise `correctAnswer` is empty string

#### Scenario: Teacher receives visible answers

- GIVEN a lesson with exercises
- WHEN the course-creator TEACHER reads the lesson
- THEN each exercise `correctAnswer` contains the actual answer
