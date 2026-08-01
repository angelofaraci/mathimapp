# Delta for profile-screen

## MODIFIED Requirements

### Requirement: Bottom Navigation Shell

The system SHALL render a `Scaffold` with a `NavigationBar` of four tabs — Inicio, Actividades, Progreso, Perfil — as the authenticated area after `AuthGate` resolves. The shell's `MainRouterViewModel` SHALL be constructible from Koin DI without requiring a container-resolved `MainTab` binding.
(Previously: no constraint on how `MainRouterViewModel` is registered in DI.)

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

#### Scenario: Router ViewModel resolves from DI without a MainTab binding

- GIVEN the Koin `appModule` graph as configured
- WHEN `MainRouterViewModel` is resolved from the container
- THEN resolution SHALL succeed without requiring any bound `MainTab` definition

#### Scenario: Entering the authenticated area does not crash after onboarding

- GIVEN a learner who just completed onboarding, or a cold start with onboarding already complete
- WHEN the authenticated area is entered
- THEN `AuthenticatedHomeScaffold` and `MainRouterViewModel` SHALL resolve without throwing
- AND the bottom-nav shell SHALL render with Inicio (HOME) selected by default
