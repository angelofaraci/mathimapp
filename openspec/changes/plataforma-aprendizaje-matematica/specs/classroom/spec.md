# Classroom Specification

## Purpose

Define how schools, teachers, and learners participate in classroom-based learning on the same product used by individual learners.

## Requirements

### Requirement: Code-based class joining

The system MUST let a teacher create a class with a join code, and learners SHALL be able to join that class by entering the valid code.

#### Scenario: Learner joins with a valid code

- GIVEN a teacher has an active class code
- WHEN a learner submits that code
- THEN the learner is enrolled in the class

#### Scenario: Invalid code is rejected

- GIVEN a learner enters an unknown or expired class code
- WHEN the join request is submitted
- THEN the system SHALL reject the request and keep the learner out of the class

### Requirement: Shared and individual use coexist

The system MUST support both school-managed learners and independent learners from launch, and SHALL preserve learner progress whether or not the learner belongs to a class.

#### Scenario: Independent learner keeps studying

- GIVEN a learner is not enrolled in any class
- WHEN they open the app
- THEN they can continue their assigned curriculum and saved progress
