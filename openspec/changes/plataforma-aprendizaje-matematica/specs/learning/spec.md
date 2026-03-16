# Learning Specification

## Purpose

Define how learners start, navigate curriculum, study theory, and advance through Argentina-aligned math paths.

## Requirements

### Requirement: Province-aware onboarding

The system MUST collect learner type, Argentine province, and current school year during onboarding, and SHALL use that profile to recommend the initial curriculum path.

#### Scenario: Recommended path is created

- GIVEN a new learner completes onboarding with province and school year
- WHEN the profile is saved
- THEN the app presents a starting path aligned to that province and year

#### Scenario: Missing province blocks completion

- GIVEN a new learner has not selected a province
- WHEN they try to finish onboarding
- THEN the app SHALL require province selection before continuing

### Requirement: Dual progression navigation

The system MUST let learners progress by school year and by individual topic, and SHALL show units with always-available theory before or during practice.

#### Scenario: Learner enters a unit from a topic

- GIVEN a learner opens a topic from the curriculum map
- WHEN they enter a unit
- THEN theory and practice entry points are both visible in that unit

### Requirement: Gamified practice and progress

The system MUST provide interactive exercises with rewards or streak feedback, and SHALL record unit completion and topic mastery for later resumption.

#### Scenario: Progress updates after successful practice

- GIVEN a learner completes a practice activity successfully
- WHEN the result is accepted
- THEN the learner's unit progress and reward state are updated
