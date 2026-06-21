# progress-sync Specification

## Purpose

Keep student progress synchronized between server and client with cumulative totals that persist across curriculum selection changes.

## Requirements

### Requirement: Cumulative Progress Does Not Reset On Selection Changes

The system MUST keep student progress cumulative across selected school-year or difficulty changes and MUST NOT reset totals when the student changes selection.

#### Scenario: Switching selection preserves totals

- GIVEN a student has completed exercises and earned score
- WHEN the student changes the selected school-year or difficulty
- THEN the system SHALL keep the existing cumulative progress

#### Scenario: Progress read remains cumulative

- GIVEN the server returns progress for a student
- WHEN the client reads the progress payload
- THEN the system SHALL show the accumulated score and completed lessons/exercises

### Requirement: Client Sync Merges Exercise Completions

The system MUST sync exercise completion state to the client as idempotent data and MUST preserve already synced completions when refreshing from the server.

#### Scenario: Synced completion is stored locally

- GIVEN the client receives a completed exercise from the server
- WHEN the sync runs
- THEN the system SHALL store that completion locally

#### Scenario: Duplicate sync does not duplicate records

- GIVEN the same completion is received again
- WHEN the client syncs a second time
- THEN the system SHALL keep a single local completion record
