# ui-localization Specification

## Purpose

Define how Compose Multiplatform screens in `commonMain` resolve user-facing copy, so text is centralized in string resources, translatable, and consistent across the app. Spanish (neutral Latin American, no voseo) is the default locale; English is a fallback resource file only, not a runtime-switchable locale.

## Requirements

### Requirement: String Resource Indirection

Compose screens in `commonMain` MUST resolve user-facing copy via `stringResource(Res.string.*)` (or equivalent Compose resource accessors) instead of hardcoded string literals passed directly to composables such as `Text()`.

#### Scenario: Screen renders copy from a string resource

- GIVEN a Compose screen displays user-facing text
- WHEN the screen composes
- THEN the displayed text SHALL be resolved via `stringResource(Res.string.*)`
- AND no raw string literal SHALL be passed directly as the copy argument

#### Scenario: Code identifiers, comments, and logs are exempt

- GIVEN a Kotlin file contains a code identifier, comment, or log message string
- WHEN the localization migration is applied
- THEN that string MAY remain a literal
- AND it is NOT required to route through `stringResource`

### Requirement: Spanish Default Locale

The system MUST ship `composeResources/values/strings.xml` as the default (no-qualifier) resource file, containing neutral Latin American Spanish (using "tú", not "vos").

#### Scenario: Default resource resolution returns Spanish

- GIVEN the device locale does not match a qualified resource set
- WHEN a screen resolves `stringResource(Res.string.*)`
- THEN the resolved text SHALL be the Spanish string from `values/strings.xml`

#### Scenario: Voseo forms are not used

- GIVEN a Spanish string involves second-person address
- WHEN the string is authored in `values/strings.xml`
- THEN it SHALL use "tú" conjugations
- AND it SHALL NOT use "vos" conjugations

### Requirement: English Fallback Resource Only

The system MUST provide `composeResources/values-en/strings.xml` as a fallback resource set. The system MUST NOT ship an in-app language switcher in this change.

#### Scenario: English file exists without a switcher UI

- GIVEN the app resources include `values-en/strings.xml`
- WHEN the app UI is inspected
- THEN no settings control or menu SHALL allow switching the app's display language
- AND `values-en/strings.xml` SHALL only be reachable via device-level English locale resolution

### Requirement: StudentTrack UI Label Separation

The system MUST provide a UI-layer localized label function for `StudentTrack` (e.g. `StudentTrack.localizedLabel()`) that is distinct from `StudentTrack.displayName`. `StudentTrack.displayName` and `StudentTrack.parse()` MUST remain unchanged by this localization effort.

#### Scenario: UI renders localized label, not displayName

- GIVEN a screen displays a `StudentTrack` value to the user
- WHEN the screen renders the track's label
- THEN it SHALL call the UI-layer localized label function
- AND it SHALL NOT render `StudentTrack.displayName` directly as user-facing copy

#### Scenario: displayName and parse() are unaffected

- GIVEN existing code depends on `StudentTrack.displayName` or `StudentTrack.parse()`
- WHEN the localization change is applied
- THEN `displayName` values and `parse()` behavior SHALL be identical to before the change

### Requirement: Interpolated String Placeholders Preserved

The system MUST preserve runtime interpolation (e.g. level number, lesson counts, step counts) using `%d`/`%s` placeholders in string resources rather than string concatenation.

#### Scenario: Numeric interpolation renders correctly

- GIVEN a string resource defines a placeholder such as `"Nivel %d"`
- WHEN the screen resolves the string with a runtime integer argument
- THEN the rendered text SHALL substitute the argument into the placeholder position
- AND SHALL NOT be built via manual string concatenation

### Requirement: No Remaining Hardcoded Screen Copy

Except for ViewModel `errorMessage` fields and `Throwable.message` propagation (explicitly out of scope), no user-facing string literal MUST remain directly inside a `Text()` or other copy-bearing composable call in `commonMain` screens.

#### Scenario: Migrated screen has no literal copy

- GIVEN a `commonMain` screen file has completed localization migration
- WHEN its composable functions are inspected
- THEN no `Text(...)` or similar call SHALL contain a literal user-facing string
- AND code identifiers, comments, and log messages remain exempt per the String Resource Indirection requirement

#### Scenario: ViewModel error text is out of scope

- GIVEN a `ViewModel` exposes an `errorMessage: String` field or propagates `Throwable.message`
- WHEN the localization migration is applied
- THEN that error text MAY remain unlocalized
- AND its localization is deferred to a separate backlog item
