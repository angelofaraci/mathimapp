# school-year-filtering Specification

## Purpose

Filter the official course catalog by school year.

## Requirements

### Requirement: Official Courses Are Filtered By School Year

The system MUST return only official courses whose schoolYear matches the requested school year when the official course list is queried with a schoolYear filter.

#### Scenario: Matching official courses are returned

- GIVEN official courses exist for school year 3
- WHEN a client requests the official course list for school year 3
- THEN the system SHALL return only official courses with schoolYear 3

#### Scenario: No matches returns empty list

- GIVEN no official courses exist for school year 6
- WHEN the client requests school year 6
- THEN the system SHALL return an empty collection

#### Scenario: Invalid school year is rejected

- GIVEN the client supplies a non-numeric schoolYear value
- WHEN the request is processed
- THEN the system SHALL reject the request as invalid
