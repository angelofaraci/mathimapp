# lesson-progress-derivation Specification

## Purpose

Derive lesson completion from completed exercises and remove direct student lesson-completion behavior from the normal flow.

## Requirements

### Requirement: Lesson Completion Is Derived From Exercises

The system MUST mark a lesson complete only when the student has completed all exercises in that lesson, and theory content MUST NOT count toward completion.

#### Scenario: All exercises complete the lesson

- GIVEN a lesson has multiple exercises
- WHEN the student completes the final missing exercise
- THEN the system SHALL mark the lesson complete

#### Scenario: Theory alone does not complete a lesson

- GIVEN a student reads or updates lesson theory without completing exercises
- WHEN progress is evaluated
- THEN the system SHALL not mark the lesson complete

### Requirement: Direct Lesson Completion Is Deprecated For Students

The system MUST deprecate direct student-driven completion through POST /progress for normal completion because completion is derived from exercise completions.

#### Scenario: Student direct completion is blocked

- GIVEN a student submits POST /progress to finish a lesson directly
- WHEN the request is processed
- THEN the system SHALL not treat it as the normal completion path

#### Scenario: Exercise completion triggers completion instead

- GIVEN the student completes the last exercise in a lesson
- WHEN the server evaluates lesson progress
- THEN the system SHALL complete the lesson through derivation
