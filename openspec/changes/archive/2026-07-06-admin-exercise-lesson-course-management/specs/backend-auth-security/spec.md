# Delta for backend-auth-security

## ADDED Requirements

### Requirement: Lesson Mutation Authorization

The system MUST allow ADMIN users to create, update, and delete any lesson. TEACHER users MAY create course-linked lessons inside courses they own, MAY update or delete course-linked lessons whose parent course creator matches the TEACHER's ID, and MAY create, update, and delete standalone lessons where `lesson.creatorId == userId`.

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
