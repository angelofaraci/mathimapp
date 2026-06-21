# exercise-completion Specification

## Purpose

Track authenticated student completion of exercises as the atomic progress unit. Exercises are the completion source for this slice; lesson theory is not completion material.

## Requirements

### Requirement: Student-Only Exercise Completion

The system MUST derive the completer from the authenticated student identity and MUST NOT accept arbitrary userId submission for another student.

#### Scenario: Student completes own exercise

- GIVEN an authenticated student can access an exercise
- WHEN the student submits exercise completion
- THEN the system SHALL record completion for that authenticated student

#### Scenario: Completion for another student is rejected

- GIVEN a request contains a different userId than the authenticated student
- WHEN the request is processed
- THEN the system SHALL reject it or ignore the foreign identity

### Requirement: Idempotent Exercise Completion And Scoring

The system MUST count the first completion of a given exercise once per student and MUST ignore later completions of the same exercise for progress and score totals.

#### Scenario: First completion counts

- GIVEN a student completes an exercise for the first time
- WHEN the completion is stored
- THEN the system SHALL increase progress and total score once

#### Scenario: Duplicate completion does not change totals

- GIVEN the same student submits the same exercise again with a different score
- WHEN the duplicate is processed
- THEN the system SHALL keep the original counted completion only

### Requirement: Exercise Access Follows Course Visibility

The system MUST allow completion of public/app-curated exercises by the student's selected school-year or difficulty and MUST require private course enrollment for teacher-created private exercises.

#### Scenario: Public exercise is available by selection

- GIVEN a student selected a matching school-year or difficulty level
- WHEN the student completes a public exercise
- THEN the system SHALL accept the completion without private enrollment

#### Scenario: Private exercise requires enrollment

- GIVEN a student is not enrolled in a private course
- WHEN the student tries to complete a private exercise from that course
- THEN the system SHALL reject the completion
