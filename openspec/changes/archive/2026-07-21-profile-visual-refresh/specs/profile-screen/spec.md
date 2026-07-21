# Delta for profile-screen

## MODIFIED Requirements

### Requirement: Profile Screen Layout

The system SHALL display a hub layout with: avatar with edit badge overlay, user display name, email, role chip, four navigation cards (Cuenta, Preferencias, Ayuda y soporte, Acerca de), logout button, and version caption. Sub-screen composables render via enum-driven switcher when navigation cards are tapped. The existing loading and error branches SHALL remain available before hub content renders.
(Previously: Gamified dashboard with avatar, name, level badge, XP progress bar, stat tiles, and achievements grid.)

#### Scenario: Profile screen shows hub identity section

- GIVEN the user navigates to the Perfil tab
- THEN avatar with edit badge, name, email, and role chip are visible

#### Scenario: Profile screen shows navigation cards

- GIVEN the hub view is active
- THEN four navigation cards render: Cuenta, Preferencias, Ayuda y soporte, Acerca de

#### Scenario: Missing avatar uses placeholder

- GIVEN the user model has no `avatarUrl`
- THEN the avatar section SHALL display initials or a generic icon

#### Scenario: Logout button remains accessible

- GIVEN the hub view is active
- THEN a logout button is visible and functional with existing logout behavior unchanged

## ADDED Requirements

### Requirement: Hub Identity Data Fields

The system SHALL display `email` and `role` from `AuthSession.user` in the profile hub identity section. `ProfileUiState` SHALL expose `email` and `role` fields alongside existing gamification fields.

#### Scenario: Email and role render from auth session

- GIVEN an authenticated user with email and role populated
- WHEN the hub view composes
- THEN email and role chip display with values from `AuthSession.user`

#### Scenario: Missing role uses fallback

- GIVEN the user model has no role value
- THEN the role chip SHALL display a default or empty state without crashing

### Requirement: Loading and Error States Preserved

The system SHALL preserve the current loading indicator and error-message branches. Hub identity content SHALL render only after loading succeeds without an error.

#### Scenario: Loading state remains visible

- GIVEN `ProfileUiState.isLoading` is true
- THEN the loading indicator renders instead of hub content

#### Scenario: Error state remains visible

- GIVEN `ProfileUiState.errorMessage` is populated
- THEN the error message renders instead of hub content

### Requirement: Bottom Nav Shell Preserved

The system SHALL preserve the existing `Scaffold` with `NavigationBar` of four tabs (Inicio, Actividades, Progreso, Perfil) unchanged. Profile tab selection SHALL continue to show the ProfileScreen composable as before.

#### Scenario: Bottom nav structure unchanged

- GIVEN the authenticated area renders
- THEN the four-tab bottom navigation displays with identical tab labels and behavior

#### Scenario: Profile tab selection unchanged

- GIVEN bottom nav visible with another tab selected
- WHEN the user taps the Perfil tab
- THEN the ProfileScreen composes with Hub as the default sub-screen
