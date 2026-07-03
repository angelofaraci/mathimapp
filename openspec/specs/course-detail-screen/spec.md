# course-detail-screen Specification

## Purpose

Provide a course detail screen reachable from the ACTIVITIES tab catalog, showing course metadata, progress summary, and an ordered lesson list.

## Requirements

### Requirement: Activities Tab Local Router

The system MUST provide a local sealed-class router within the ACTIVITIES tab scope with states `Catalog` and `Detail(courseId: String)`. Navigation between states SHALL keep the bottom tab bar visible and SHALL NOT affect the global scaffold router.

#### Scenario: Catalog is the default destination

- GIVEN the user navigates to the ACTIVITIES tab
- WHEN the tab content is rendered
- THEN the local router state SHALL be `Catalog` and the catalog screen SHALL be displayed

#### Scenario: Tap course card navigates to detail

- GIVEN the user is viewing the catalog with course cards
- WHEN the user taps a course card
- THEN the local router state SHALL transition to `Detail(courseId)` and the detail screen SHALL be displayed

#### Scenario: Back from detail returns to catalog

- GIVEN the user is on the detail screen for a course
- WHEN the user triggers back navigation
- THEN the local router state SHALL transition to `Catalog` and the catalog screen SHALL be displayed

### Requirement: Course Detail Screen Content

The system MUST render a course detail screen showing course title, description, difficulty, lesson count, XP reward, and an enrollment CTA. The bottom tab bar SHALL remain visible.

#### Scenario: Detail screen shows all available metadata

- GIVEN a course with title, description, difficulty "Fácil", 8 lessons, and XP 50
- WHEN the detail screen is rendered
- THEN the system SHALL display all five fields in the header area

#### Scenario: Detail screen handles missing optional fields

- GIVEN a course with title and description but no difficulty or XP
- WHEN the detail screen is rendered
- THEN the system SHALL display available fields and omit missing ones

### Requirement: Course Progress Bar

The system SHALL display a progress bar on the detail screen derived from `completedLessonIds.size / totalLessons`. The progress bar SHALL only be visible when the user is enrolled in the course.

#### Scenario: Progress bar shows enrolled progress

- GIVEN the user is enrolled in a course with 8 lessons and has 2 completed
- WHEN the detail screen is rendered
- THEN the system SHALL display a progress bar indicating "2/8 lecciones"

#### Scenario: Progress bar hidden for non-enrolled users

- GIVEN the user is NOT enrolled in the course
- WHEN the detail screen is rendered
- THEN the system SHALL NOT display the progress bar

#### Scenario: Zero progress shows correctly

- GIVEN the user is enrolled but has completed 0 lessons
- WHEN the detail screen is rendered
- THEN the system SHALL display a progress bar indicating "0/8 lecciones"

### Requirement: Lesson List Display

The system MUST render an ordered list of all lessons belonging to the course. Each lesson card SHALL show the lesson title, exercise count, and a status indicator. All lessons SHALL be visible in V1 regardless of completion status (no locking).

#### Scenario: Lessons render in orderIndex order

- GIVEN a course with lessons having orderIndex 1, 2, 3
- WHEN the lesson list is rendered
- THEN the system SHALL display lessons in ascending orderIndex order

#### Scenario: Completed lesson shows checkmark

- GIVEN a lesson whose ID is in the user's `completedLessonIds`
- WHEN the lesson card is rendered
- THEN the system SHALL display a checkmark status indicator

#### Scenario: Incomplete lesson shows arrow

- GIVEN a lesson whose ID is NOT in the user's `completedLessonIds`
- WHEN the lesson card is rendered
- THEN the system SHALL display an arrow status indicator

#### Scenario: All lessons visible without locking

- GIVEN a course with 8 lessons and the user has completed lesson 1 only
- WHEN the lesson list is rendered
- THEN the system SHALL display all 8 lessons (no padlock or hidden lessons)

### Requirement: Lesson Tap Inert in V1

The system SHALL NOT navigate to a lesson detail screen when a lesson card is tapped in V1. The tap SHALL be inert — no navigation, no toast, no placeholder.

#### Scenario: Lesson tap produces no action

- GIVEN the user is on the course detail screen
- WHEN the user taps a lesson card
- THEN the system SHALL NOT navigate, show a toast, or trigger any visible action

### Requirement: Detail Data Fetching

The system MUST fetch course data by ID (including lessons with exerciseCount) and user progress (for `completedLessonIds` and `enrolledCourseIds`) when entering the detail screen.

#### Scenario: Data loads on detail entry

- GIVEN the user navigates to the detail screen for a course
- WHEN the screen is entered
- THEN the system SHALL fetch the course by ID and the user's progress

#### Scenario: Loading state displayed during fetch

- GIVEN the user navigates to the detail screen
- WHEN the data fetch is in progress
- THEN the system SHALL display a loading indicator

#### Scenario: Error state on fetch failure

- GIVEN the course fetch fails with a network error
- WHEN the fetch completes
- THEN the system SHALL display an error state with a retry option
