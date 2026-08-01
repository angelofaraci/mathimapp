# Delta for onboarding-flow

## ADDED Requirements

### Requirement: Onboarding Copy Renders in Spanish

The system MUST render all onboarding flow copy (step labels, options, buttons, helper text) in neutral Latin American Spanish by default, resolved via `stringResource(Res.string.*)` from `composeResources/values/strings.xml`. `values-en/strings.xml` exists only as a fallback resource set and MUST NOT be exposed via an in-app language switcher.

#### Scenario: Onboarding screen text is in Spanish

- GIVEN the user is on any onboarding step (province, school year, or onboarding category)
- WHEN the step renders under the default (no-qualifier) resource resolution
- THEN all visible labels, options, and buttons SHALL be Spanish text sourced from `values/strings.xml`
- AND no English literal SHALL be hardcoded in the onboarding composables

#### Scenario: No language switcher is exposed during onboarding

- GIVEN the user is anywhere in the onboarding flow
- WHEN the onboarding UI is inspected
- THEN no control SHALL allow switching the onboarding flow's display language
- AND English text SHALL only be reachable via device-level English locale resolution

#### Scenario: Interpolated onboarding copy remains correct in Spanish

- GIVEN an onboarding step displays interpolated text (e.g. a step counter like "Paso %d / 3")
- WHEN the step renders with a runtime step number
- THEN the Spanish string resource SHALL substitute the number via its placeholder
- AND the rendered sentence SHALL read as grammatically correct Spanish
