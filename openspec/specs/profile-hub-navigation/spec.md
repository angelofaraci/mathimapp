# Profile Hub Navigation Specification

## Purpose

Local enum-driven navigation within `ProfileScreen` that switches between a hub view and stubbed sub-screen composables (Cuenta, Preferencias, Ayuda y soporte, Acerca de). All navigation is intra-tab; no global router changes.

## Requirements

### Requirement: ProfileSubScreen Enum

The system SHALL define a local `ProfileSubScreen` enum with Kotlin identifiers `HUB`, `ACCOUNT`, `PREFERENCES`, `HELP`, and `ABOUT`. This enum drives which composable renders inside the Profile tab; the rendered titles remain localized independently.

#### Scenario: Enum covers all declared sub-screens

- GIVEN the `ProfileSubScreen` enum is defined
- THEN it SHALL contain exactly five values: `HUB`, `ACCOUNT`, `PREFERENCES`, `HELP`, and `ABOUT`

#### Scenario: Enum is local to ProfileScreen

- GIVEN the enum is defined
- THEN it SHALL NOT be referenced by the global navigation router or other tabs

### Requirement: Intra-Tab Sub-Screen Switching

The system SHALL switch between sub-screen composables based on the current `ProfileSubScreen` value using `AnimatedContent`. Navigation actions from hub cards update the enum; in-app and Android system-back actions from a local sub-screen return to `HUB`.

#### Scenario: Tapping a navigation card switches sub-screen

- GIVEN the hub view is visible with navigation cards
- WHEN the user taps the "Cuenta" card
- THEN the Cuenta sub-screen composable renders and hub is replaced

#### Scenario: In-app back action returns to HUB

- GIVEN a sub-screen (e.g., Cuenta) is visible
- WHEN the user taps the local back affordance
- THEN the `HUB` sub-screen renders

#### Scenario: Android system back returns to HUB

- GIVEN a local sub-screen (e.g., `ACCOUNT`) is visible
- WHEN the user invokes Android system back
- THEN the system-back event SHALL be consumed by `ProfileScreen`
- AND the current sub-screen SHALL become `HUB`
- AND the Profile tab or host activity SHALL NOT be exited by that event

#### Scenario: Default sub-screen is Hub

- GIVEN the user navigates to the Perfil bottom tab
- WHEN the ProfileScreen composes
- THEN the current sub-screen SHALL be `HUB`

### Requirement: Stubbed Sub-Screen Composables

The system SHALL render stubbed composables for Cuenta, Preferencias, Ayuda, and Acerca de. Each stub SHALL display a title, placeholder content, and a TODO comment indicating unimplemented behavior.

#### Scenario: Cuenta stub renders with TODO

- GIVEN the Cuenta sub-screen is active
- THEN a screen with title "Cuenta", placeholder text, and TODO comment renders

#### Scenario: Preferencias stub renders with TODO

- GIVEN the Preferencias sub-screen is active
- THEN a screen with title "Preferencias", placeholder text, and TODO comment renders

#### Scenario: Ayuda stub renders with TODO

- GIVEN the Ayuda sub-screen is active
- THEN a screen with title "Ayuda y soporte", placeholder text, and TODO comment renders

#### Scenario: AcercaDe stub renders with TODO

- GIVEN the AcercaDe sub-screen is active
- THEN a screen with title "Acerca de", placeholder text, and TODO comment renders

### Requirement: No Functional Wiring in Stubs

The system SHALL NOT wire stubbed sub-screens to real account actions, preference persistence, or backend calls. All interactive elements in stubs SHALL be no-ops with TODO comments.

#### Scenario: Stub interactions are no-ops

- GIVEN any stubbed sub-screen is visible
- WHEN the user taps any interactive element
- THEN no network request, state change, or navigation occurs outside the profile tab