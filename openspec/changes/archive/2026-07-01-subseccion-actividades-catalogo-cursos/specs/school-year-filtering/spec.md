# Delta for school-year-filtering

## ADDED Requirements

### Requirement: Official Courses Include Discovery Metadata

The system SHALL include `topic`, `difficulty`, `durationMinutes`, and `xpReward` fields in the official course list response alongside the existing `schoolYear` filter.

#### Scenario: Course list includes discovery fields

- GIVEN official courses exist with topic, difficulty, duration, and XP data
- WHEN a client requests the official course list for a school year
- THEN the system SHALL return each course with `topic`, `difficulty`, `durationMinutes`, and `xpReward` populated

#### Scenario: Discovery fields are nullable

- GIVEN an official course exists without discovery metadata
- WHEN the course list is returned
- THEN the system SHALL return the course with null or default values for discovery fields
