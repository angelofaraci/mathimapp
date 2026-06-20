# Learning Specification

## Purpose

Define how learners start, navigate curriculum, study theory, and advance through Argentina-aligned math paths.

## Requirements

### Requirement: School-year-aware onboarding

The system MUST collect learner type and current school year during onboarding, and SHALL use that profile to recommend the initial curriculum path. The MVP SHALL NOT subdivide curriculum or theory content by province.

#### Scenario: Recommended path is created

- GIVEN a new learner completes onboarding with learner type and school year
- WHEN the profile is saved
- THEN the app presents a starting path aligned to that school year

#### Scenario: Missing school year blocks completion

- GIVEN a new learner has not selected a school year
- WHEN they try to finish onboarding
- THEN the app SHALL require school-year selection before continuing

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

### Requirement: Lesson theory access

The system MUST expose a theory entry point from each module or lesson, and SHALL link that entry point to the theory needed for the current lesson.

#### Scenario: Learner opens lesson theory

- GIVEN a learner is viewing a lesson
- WHEN the lesson screen is displayed
- THEN a theory button is available for that lesson

#### Scenario: Missing theory is handled safely

- GIVEN a lesson has no theory content assigned
- WHEN the learner opens the lesson
- THEN the theory entry point SHALL not lead to unrelated content

### Requirement: Topic-scoped learner chatbot

The system MUST provide a chatbot for learner questions about the currently opened theory or topic, and SHALL keep that chatbot scoped to the active context.

#### Scenario: Theory view opens chatbot

- GIVEN a learner is viewing theory for a topic
- WHEN the learner selects the chatbot button
- THEN the chatbot opens with the same topic context

#### Scenario: Chatbot answers in current topic context

- GIVEN a learner asks a question in the chatbot
- WHEN the question is submitted
- THEN the response SHALL be based on the currently opened theory or topic
