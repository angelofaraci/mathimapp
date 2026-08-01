# Delta for learner-profile

## MODIFIED Requirements

### Requirement: Local Profile Schema

The system MUST define a SQLDelight table (`LearnerProfileEntity`) with columns for `userId` (TEXT, NOT NULL), `schoolYear` (INTEGER), `studentTrack` (TEXT), `province` (TEXT), and `onboardingComplete` (INTEGER/BOOLEAN). The table SHALL support at most one row per `userId`.
(Previously: the table supported at most one active profile row globally, with no `userId` column.)

#### Scenario: Table supports one row per user

- GIVEN the SQLDelight schema is compiled
- WHEN the `LearnerProfileEntity` table is queried for a given `userId`
- THEN the system SHALL return at most one row for that `userId`
- AND the row SHALL contain `userId` plus all four profile fields

#### Scenario: Schema compiles across all platforms

- GIVEN the `.sq` file defines the `LearnerProfileEntity` table
- WHEN the project builds for JVM, Android, and iOS targets
- THEN the SQLDelight code generation SHALL succeed without errors

#### Scenario: Existing installs are migrated via table rebuild

- GIVEN an existing installation has a `LearnerProfileEntity` table without a `userId` column
- WHEN the app starts and the schema-fix routine runs
- THEN the system SHALL rebuild the table with the `userId`-keyed shape (an additive `ALTER TABLE` cannot drop the legacy `PRIMARY KEY`/`CHECK` constraint)
- AND pre-existing rows SHALL receive a sentinel `userId` value that matches no real authenticated user

### Requirement: Profile Persistence on Completion

The system MUST write the learner profile to the local database, scoped to the authenticated user's `userId`, when onboarding is completed. The write operation SHALL set `onboardingComplete` to true for that `userId`'s row.
(Previously: the write targeted a single global row with no `userId` scoping.)

#### Scenario: Profile is saved after onboarding for the current user

- GIVEN the user completes all onboarding steps while authenticated with `userId` = A
- WHEN the completion action is triggered
- THEN the system SHALL insert or update the `LearnerProfileEntity` row for `userId` = A
- AND `onboardingComplete` SHALL be set to true for that row
- AND `schoolYear`, `studentTrack`, `province` SHALL match the user's selections

#### Scenario: Duplicate completion does not create multiple rows for the same user

- GIVEN a profile row already exists for `userId` = A
- WHEN the user (still `userId` = A) completes onboarding again
- THEN the system SHALL update the existing row for `userId` = A
- AND the table SHALL still contain exactly one row for `userId` = A

#### Scenario: Different users on the same device get independent rows

- GIVEN a profile row already exists for `userId` = A
- WHEN a different authenticated user (`userId` = B) completes onboarding on the same device
- THEN the system SHALL insert a separate row for `userId` = B
- AND the row for `userId` = A SHALL remain unchanged

### Requirement: Onboarding Completion Check

The system MUST provide a query, scoped to a given `userId`, to check whether onboarding has been completed for that user. This check SHALL be used by the auth gate to determine whether to show onboarding or `CourseScreen` for the currently authenticated user.
(Previously: the check had no `userId` parameter and evaluated a single global row.)

#### Scenario: Completed onboarding returns true for that user

- GIVEN a profile row exists for `userId` = A with `onboardingComplete` = true
- WHEN the completion check is executed for `userId` = A
- THEN the system SHALL return true

#### Scenario: Missing profile returns false for that user

- GIVEN no profile row exists for `userId` = B
- WHEN the completion check is executed for `userId` = B
- THEN the system SHALL return false, even if a completed row exists for a different `userId`

#### Scenario: Incomplete profile returns false

- GIVEN a profile row exists for `userId` = A with `onboardingComplete` = false
- WHEN the completion check is executed for `userId` = A
- THEN the system SHALL return false

### Requirement: Profile Retrieval for Course Filtering

The system MUST provide a query, scoped to a given `userId`, to retrieve the stored `schoolYear` value from that user's learner profile. This value SHALL be used when calling `CourseRepository.getOfficialCourses(schoolYear)` for the currently authenticated user.
(Previously: the query had no `userId` parameter and read from a single global row.)

#### Scenario: School year is retrievable for the authenticated user's course fetch

- GIVEN a completed profile for `userId` = A with `schoolYear` = 5
- WHEN the profile is queried for course filtering with `userId` = A
- THEN the system SHALL return schoolYear = 5

#### Scenario: Null school year when no profile exists for that user

- GIVEN no profile row exists for `userId` = B
- WHEN the profile is queried for course filtering with `userId` = B
- THEN the system SHALL return null for schoolYear, even if a profile exists for a different `userId`

### Requirement: Student Track Enum Mapping

The system MUST map the four student track values (`Primary`, `Secondary`, `Technical Secondary`, `Self-directed`) to and from the database storage format. The mapping SHALL be consistent across read and write operations, and SHALL be applied per-`userId` row.
(Previously: no `userId` scoping was mentioned since rows were global.)

#### Scenario: Student track round-trips correctly

- GIVEN the student track is `Technical Secondary` for `userId` = A
- WHEN the profile is written and then read back for `userId` = A
- THEN the system SHALL return `Technical Secondary` unchanged

#### Scenario: Self-directed round-trips correctly

- GIVEN the student track is `Self-directed` for `userId` = A
- WHEN the profile is written and then read back for `userId` = A
- THEN the system SHALL return `Self-directed` unchanged

### Requirement: No Diagnostic Persistence in This Slice

The system MUST NOT add mastery, level, or diagnostic-answer columns to the local learner profile in this slice.

#### Scenario: Schema excludes diagnostic fields

- GIVEN the local learner profile schema for this slice
- WHEN the table definition is reviewed
- THEN the stored onboarding data SHALL be limited to `userId`, province, school year, student track, and onboarding completion state
