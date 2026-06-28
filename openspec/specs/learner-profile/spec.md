# learner-profile Specification

## Purpose

Define the local SQLDelight persistence layer for the onboarding outcome. The learner profile stores `schoolYear`, `studentTrack`, `province`, and `onboardingComplete` so the app gate survives recomposition and app restart on the current device.

## Requirements

### Requirement: Local Profile Schema

The system MUST define a SQLDelight table (`LearnerProfileEntity`) with columns for `schoolYear` (INTEGER), `studentTrack` (TEXT), `province` (TEXT), and `onboardingComplete` (INTEGER/BOOLEAN). The table SHALL support at most one active profile row.

#### Scenario: Table supports single profile row

- GIVEN the SQLDelight schema is compiled
- WHEN the `LearnerProfileEntity` table is queried
- THEN the system SHALL return at most one row
- AND the row SHALL contain all four fields

#### Scenario: Schema compiles across all platforms

- GIVEN the `.sq` file defines the `LearnerProfileEntity` table
- WHEN the project builds for JVM, Android, and iOS targets
- THEN the SQLDelight code generation SHALL succeed without errors

### Requirement: Profile Persistence on Completion

The system MUST write the learner profile to the local database when onboarding is completed. The write operation SHALL set `onboardingComplete` to true.

#### Scenario: Profile is saved after onboarding

- GIVEN the user completes all onboarding steps
- WHEN the completion action is triggered
- THEN the system SHALL insert or update the `LearnerProfileEntity` row
- AND `onboardingComplete` SHALL be set to true
- AND `schoolYear`, `studentTrack`, `province` SHALL match the user's selections

#### Scenario: Duplicate completion does not create multiple rows

- GIVEN a profile row already exists
- WHEN the user completes onboarding again
- THEN the system SHALL update the existing row
- AND the table SHALL still contain exactly one row

### Requirement: Onboarding Completion Check

The system MUST provide a query to check whether onboarding has been completed. This check SHALL be used by the auth gate to determine whether to show onboarding or `CourseScreen`.

#### Scenario: Completed onboarding returns true

- GIVEN a profile row exists with `onboardingComplete` = true
- WHEN the completion check is executed
- THEN the system SHALL return true

#### Scenario: Missing profile returns false

- GIVEN no profile row exists in the database
- WHEN the completion check is executed
- THEN the system SHALL return false

#### Scenario: Incomplete profile returns false

- GIVEN a profile row exists with `onboardingComplete` = false
- WHEN the completion check is executed
- THEN the system SHALL return false

### Requirement: Profile Retrieval for Course Filtering

The system MUST provide a query to retrieve the stored `schoolYear` value from the learner profile. This value SHALL be used when calling `CourseRepository.getOfficialCourses(schoolYear)`.

#### Scenario: School year is retrievable for course fetch

- GIVEN a completed profile with `schoolYear` = 5
- WHEN the profile is queried for course filtering
- THEN the system SHALL return schoolYear = 5

#### Scenario: Null school year when no profile exists

- GIVEN no profile row exists
- WHEN the profile is queried for course filtering
- THEN the system SHALL return null for schoolYear

### Requirement: Student Track Enum Mapping

The system MUST map the four student track values (`Primary`, `Secondary`, `Technical Secondary`, `Self-directed`) to and from the database storage format. The mapping SHALL be consistent across read and write operations.

#### Scenario: Student track round-trips correctly

- GIVEN the student track is `Technical Secondary`
- WHEN the profile is written and then read back
- THEN the system SHALL return `Technical Secondary` unchanged

#### Scenario: Self-directed round-trips correctly

- GIVEN the student track is `Self-directed`
- WHEN the profile is written and then read back
- THEN the system SHALL return `Self-directed` unchanged

### Requirement: No Diagnostic Persistence in This Slice

The system MUST NOT add mastery, level, or diagnostic-answer columns to the local learner profile in this slice.

#### Scenario: Schema excludes diagnostic fields

- GIVEN the local learner profile schema for this slice
- WHEN the table definition is reviewed
- THEN the stored onboarding data SHALL be limited to province, school year, student track, and onboarding completion state
