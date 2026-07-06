# Delta for lesson-read-access

## ADDED Requirements

### Requirement: Standalone Lesson Read Visibility

The system SHALL enforce visibility rules for standalone lessons (`courseId = null`) based on ownership and role.

| Role | Can Read Standalone Lesson | Condition |
|------|--------------------------|-----------|
| **admin** | Always | Any ADMIN user |
| **creator** | Always | `lesson.creatorId == userId` |
| **other** | Never | All other users |

#### Scenario: Admin reads standalone lesson

- GIVEN a standalone lesson exists
- WHEN an ADMIN requests the lesson
- THEN the lesson is returned with exercises (answers hidden)

#### Scenario: Creator reads own standalone lesson

- GIVEN a standalone lesson with `creatorId = X`
- WHEN user X requests the lesson
- THEN the lesson is returned with exercises (answers visible)

#### Scenario: Non-creator denied standalone lesson

- GIVEN a standalone lesson with `creatorId = X`
- WHEN a different user Y requests the lesson
- THEN Forbidden is returned

### Requirement: Standalone Lesson List Access

The system SHALL provide an admin-only endpoint to list standalone lessons. Non-admin users SHALL NOT access this list.

#### Scenario: Admin lists standalone lessons

- WHEN an ADMIN requests standalone lessons
- THEN the system SHALL return all lessons where `courseId = null`

#### Scenario: Non-admin cannot list standalone lessons

- GIVEN a TEACHER or STUDENT user
- WHEN the user requests the standalone lesson list
- THEN the system SHALL reject with 403
