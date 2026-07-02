# profile-screen Specification

## Purpose

Bottom-navigation shell for the authenticated area with four tabs and a Profile screen with user identity and client-derived gamification metrics. All logic in `composeApp`.

## Requirements

### Requirement: Bottom Navigation Shell

The system SHALL render a `Scaffold` with a `NavigationBar` of four tabs — Inicio, Actividades, Progreso, Perfil — as the authenticated area after `AuthGate` resolves.

#### Scenario: Authenticated area renders bottom nav

- GIVEN valid auth session and completed onboarding
- WHEN the authenticated area resolves
- THEN a Scaffold with four bottom-nav tabs displays with Inicio selected

#### Scenario: Tab selection switches content

- GIVEN bottom nav visible with Inicio selected
- WHEN the user taps the Perfil tab
- THEN the Profile screen displays and the Perfil tab is visually selected

#### Scenario: Rapid tab switching does not crash

- GIVEN bottom nav is visible
- WHEN the user taps three different tabs rapidly
- THEN the last selected tab displays without exception

### Requirement: Profile Screen Layout

The system SHALL display: avatar placeholder, user display name, level badge, XP progress bar, two stat tiles (streak, completed lessons), and an achievements grid.

#### Scenario: Profile screen shows all sections

- GIVEN the user navigates to the Perfil tab
- THEN avatar, name, level badge, XP bar, stats, and achievements grid are visible

#### Scenario: Missing avatar uses placeholder

- GIVEN the user model has no `avatarUrl`
- THEN the avatar section SHALL display initials or a generic icon

### Requirement: Client-Derived Gamification Metrics

The system SHALL derive level, XP, activity streak, and achievement status from existing local data. Level SHALL equal `totalScore / 100` (integer division). Activity streak SHALL equal `min(completedLessonIds.size, 7)`.

#### Scenario: Level derives from totalScore

- GIVEN user has `totalScore` of 350
- WHEN gamification metrics are computed
- THEN level SHALL be 3 and XP progress SHALL reflect 50 points toward next level

#### Scenario: Activity streak caps at 7

- GIVEN the user has 12 completed lessons
- WHEN the activity streak is computed
- THEN the system SHALL report a streak of 7

#### Scenario: Activity streak equals completed count when below cap

- GIVEN the user has 3 completed lessons
- WHEN the activity streak is computed
- THEN the system SHALL report a streak of 3

#### Scenario: Zero score yields level zero

- GIVEN `totalScore` is 0
- WHEN gamification metrics are computed
- THEN level SHALL be 0 and XP progress SHALL be 0%

### Requirement: Achievement Thresholds

The system SHALL evaluate achievements against progress thresholds. Each achievement SHALL have a name, icon placeholder, and locked/unlocked state.

#### Scenario: Achievement unlocks at threshold

- GIVEN an achievement requires 10 completed lessons
- AND the user has completed 10 lessons
- WHEN achievements are evaluated
- THEN the achievement SHALL be marked unlocked

#### Scenario: Achievement remains locked below threshold

- GIVEN an achievement requires 10 completed lessons
- AND the user has completed 3 lessons
- WHEN achievements are evaluated
- THEN the achievement SHALL be marked locked

### Requirement: Placeholder Tabs

The system SHALL render non-empty placeholder screens for Actividades and Progreso tabs with a title and "under development" message.

#### Scenario: Actividades tab shows placeholder

- GIVEN the user selects the Actividades tab
- WHEN the tab content renders
- THEN the system SHALL display a placeholder with title and "under development" message

#### Scenario: Progreso tab shows placeholder

- GIVEN the user selects the Progreso tab
- WHEN the tab content renders
- THEN the system SHALL display a placeholder with title and "under development" message

### Requirement: Inicio Tab Hosts HomeDashboardScreen

The system SHALL render the `HomeDashboardScreen` as Inicio tab content, replacing the legacy `CourseScreen`.
(Previously: Inicio tab hosted `CourseScreen` with its existing behavior.)

#### Scenario: Inicio displays dashboard content

- GIVEN the user selects the Inicio tab
- WHEN the tab content renders
- THEN the system SHALL display the `HomeDashboardScreen` with greeting, progress summary, and empty-state card

#### Scenario: Dashboard navigation to Activities works

- GIVEN the dashboard is visible on the Inicio tab
- WHEN the user taps a CTA that targets the Activities tab
- THEN the system SHALL switch to the Activities tab and display the course catalog
