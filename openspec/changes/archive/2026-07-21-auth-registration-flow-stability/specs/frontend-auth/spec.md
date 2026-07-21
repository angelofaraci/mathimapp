# Delta for frontend-auth

## ADDED Requirements

### Requirement: Auth Gate Survives Configuration Changes

The system MUST ensure the auth gate target and DI context survive device configuration changes (rotation, locale change, dark-mode toggle) without resetting the auth flow or creating divergent `AuthRepository` instances between the auth gate and child ViewModels. Koin startup SHALL occur at the platform entry point, not inside Compose. The auth gate router SHALL be backed by a `ViewModel` instance.

#### Scenario: Login survives device rotation

- GIVEN the user is on the Login screen with credentials entered
- WHEN the device rotates
- THEN the Login screen SHALL remain visible with entered data preserved
- AND the system SHALL NOT reset to a different auth target

#### Scenario: Registration step survives rotation

- GIVEN the user is mid-registration (step 2 or 3 of the wizard)
- WHEN the device rotates
- THEN the system SHALL remain on the same registration step
- AND the system SHALL NOT revert to the Login screen
- AND previously entered registration data SHALL be preserved

#### Scenario: DI singleton consistency after rotation

- GIVEN the app is running with an active Koin context
- WHEN a configuration change triggers recomposition
- THEN the `AuthRepository` instance observed by the auth gate SHALL be the same instance observed by child ViewModels
- AND no duplicate Koin context SHALL be created

#### Scenario: Auth gate target consistency after rotation

- GIVEN the auth gate has resolved to the Register target
- WHEN the device rotates
- THEN the auth gate SHALL continue targeting Register
- AND the system SHALL NOT revert to the Login target
