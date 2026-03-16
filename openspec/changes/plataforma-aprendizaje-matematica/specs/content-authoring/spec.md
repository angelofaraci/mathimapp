# Content Authoring Specification

## Purpose

Define how teachers assign default material or create custom material for classroom learning.

## Requirements

### Requirement: Teacher-authored content

The system MUST let teachers create custom theory and custom exercises for a class, and SHALL associate that content with the intended class or unit.

#### Scenario: Teacher creates custom unit content

- GIVEN a teacher manages a class
- WHEN the teacher saves custom theory and exercises for a unit
- THEN that class can access the new content in the assigned unit

### Requirement: Reuse of default content

The system MUST let teachers reuse default platform theory and exercises instead of authoring everything from scratch.

#### Scenario: Teacher assigns default material

- GIVEN default unit content exists on the platform
- WHEN a teacher selects that content for a class
- THEN learners in the class see the selected default material as part of their assigned work

#### Scenario: Empty assignment is prevented

- GIVEN a teacher has chosen neither default nor custom content
- WHEN the teacher tries to publish an assignment
- THEN the system SHALL prevent publication until at least one content source is selected
