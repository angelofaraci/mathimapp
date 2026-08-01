# Design: Localize UI Copy to Spanish via Compose Resources

## Technical Approach

Add `composeResources/values/strings.xml` (neutral Latin American Spanish, default) and
`composeResources/values-en/strings.xml` (fallback only). Replace literals in `commonMain/.../ui/**`
with `stringResource(Res.string.*)`, screen group by screen group. Domain stays Compose-free;
`StudentTrack` gets a UI-side `@Composable` extension mirroring the existing
`UserRole.displayName()` private extension in `ProfileScreen.kt:426`.

## Architecture Decisions

### Decision: `values/` is Spanish, `values-en/` is the fallback (not the reverse)

**Choice**: default/unqualified `values/strings.xml` holds Spanish; `values-en/` holds English.
**Alternatives**: English default + `values-es/`.
**Rationale**: CMP resolves the unqualified folder when no locale matches. The product audience is
Argentina; an unmatched locale must land on Spanish, not English. No Gradle change: `composeResources/`
is already the configured root (`drawable/`, `font/`, `files/` all resolve today, and
`libs.compose.components.resources` is already in `commonMain`). `values/` is the same sibling-folder
convention — no new registration needed.

### Decision: `StudentTrack.displayName` and `parse()` are untouched — they are a persistence contract

**Choice**: add `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/StudentTrackLabels.kt`
with `@Composable fun StudentTrack.localizedLabel(): String`. `domain/StudentTrack.kt` is not edited.
**Alternatives**: translate `displayName` in place.
**Rationale**: `SqlDelightLearnerProfileRepository.kt:45` writes `profile.studentTrack.displayName`
into `LearnerProfileEntity.studentTrack`, and `parse()` reads it back. Translating `displayName` would
silently orphan every stored profile row. This is stronger than the proposal's "Med likelihood" risk —
it is a certain data break, so the non-goal is a hard constraint.

### Decision: positional placeholders `%1$s` / `%1$d`, always numbered

**Choice**: every interpolated string uses numbered positional placeholders, even single-argument ones.
**Alternatives**: bare `%s`/`%d`.
**Rationale**: Spanish and English word order differ; numbering keeps translations reorderable and is
required for multi-arg strings. Literal `%` must be escaped `%%` (e.g. lesson-map progress).

### Decision: `schoolYearLabel` moves from ViewModel to composable

**Choice**: `ProfileUiState`/`HomeDashboardUiState` expose `schoolYear: Int?` and
`studentTrack: StudentTrack?` instead of the pre-formatted `schoolYearLabel: String?`
(`ProfileViewModel.kt:63`, `HomeDashboardViewModel.kt:128`, both currently English `"Year N • Track"`).
**Alternatives**: leave the label in the ViewModel.
**Rationale**: it is a display string containing `displayName`, so it cannot be localized without
either a Compose dependency in the ViewModel or this state-shape change. It is not an `errorMessage`,
so the proposal's exclusion does not cover it. Scoped to Group D; touches the two ViewModel tests.

### Decision: `salutation()`/`greetingFor()` stay in `HomeDashboardViewModel`

**Choice**: unchanged. **Rationale**: already Spanish, and unit-tested by string value. Logged on the
new backlog follow-up alongside error messages.

## Key Naming Convention

`{screen}_{element}[_{qualifier}]`, snake_case, screen prefix always first. Actions use
`{screen}_action_{verb}`. Full enumeration for the largest file, `OnboardingScreen.kt`:

| Key | Spanish value |
|---|---|
| `onboarding_title` | Completa tu perfil |
| `onboarding_action_logout` | Cerrar sesión |
| `onboarding_action_continue` | Continuar |
| `onboarding_action_back` | Volver |
| `onboarding_action_saving` | Guardando perfil... |
| `onboarding_action_continue_to_courses` | Continuar a los cursos |
| `onboarding_summary_province` | Provincia: %1$s |
| `onboarding_summary_school_year` | Año escolar: %1$s |
| `onboarding_summary_category` | Categoría: %1$s |
| `onboarding_step1_title` | 1. Elige tu provincia |
| `onboarding_step1_description` | Tu provincia determina los años escolares válidos. |
| `onboarding_step2_title` | 2. Elige tu año escolar |
| `onboarding_step2_description` | Los años disponibles ya reflejan la estructura de la provincia seleccionada. |
| `onboarding_step3_title` | 3. Elige tu categoría |
| `onboarding_step3_description` | Se muestran las cuatro categorías. Solo se habilitan las válidas para el año elegido. |
| `onboarding_step4_title` | 4. Confirma tu perfil |
| `onboarding_step4_description` | Revisa la provincia, el año escolar y la categoría antes de continuar a los cursos. |
| `onboarding_track_unavailable` | No disponible para el año escolar seleccionado |

`StepSummary` (line 169-172) and `ConfirmationStep` (line 307-311) reuse the same three
`onboarding_summary_*` keys — 18 keys cover 19 literal sites.

`StudentTrack` keys: `track_primary`, `track_secondary`, `track_technical_secondary`,
`track_self_directed` (Primaria / Secundaria / Secundaria técnica / Autodidacta).

## Interfaces / Contracts

XML:

```xml
<string name="lesson_map_progress_percent">%1$d%% Completado</string>
<string name="lesson_map_progress_lessons">%1$d/%2$d Lecciones</string>
<string name="register_step_indicator">Paso %1$d / 3</string>
<string name="home_level">Nivel %1$d</string>
```

Call sites:

```kotlin
Text(stringResource(Res.string.lesson_map_progress_lessons, completed, total))
Text(stringResource(Res.string.register_step_indicator, currentStep))
```

```kotlin
// ui/StudentTrackLabels.kt
@Composable
fun StudentTrack.localizedLabel(): String = stringResource(
    when (this) {
        StudentTrack.PRIMARY -> Res.string.track_primary
        StudentTrack.SECONDARY -> Res.string.track_secondary
        StudentTrack.TECHNICAL_SECONDARY -> Res.string.track_technical_secondary
        StudentTrack.SELF_DIRECTED -> Res.string.track_self_directed
    }
)
```

`allowedTrackSummary()` (`OnboardingScreen.kt:385`) must become `@Composable` to map over
`localizedLabel()`, since `stringResource` cannot be called from a plain function.

## File Changes

| File | Action | Description |
|---|---|---|
| `composeApp/src/commonMain/composeResources/values/strings.xml` | Create | Spanish default, ~110 keys |
| `composeApp/src/commonMain/composeResources/values-en/strings.xml` | Create | English fallback, same key set |
| `.../ui/StudentTrackLabels.kt` | Create | `localizedLabel()` extension |
| `.../ui/OnboardingScreen.kt` | Modify | Group A — 19 sites |
| `.../ui/activities/{LessonMapScreen,TheorySheet,LessonMapNode}.kt`, `.../ui/PlaceholderScreen.kt` | Modify | Group B |
| `.../ui/RegisterScreen.kt`, `.../ui/home/HomeDashboardScreen.kt` | Modify | Group C |
| `.../ui/{LoginScreen,ProfileScreen,AuthScreenScaffold,AuthenticatedHomeScaffold}.kt` | Modify | Group D |
| `.../ui/ProfileViewModel.kt`, `.../ui/home/HomeDashboardViewModel.kt` | Modify | Group D — `schoolYearLabel` → structured fields |
| `commonTest/.../{ProfileViewModelTest,HomeDashboardViewModelTest}.kt` | Modify | Group D — assert structured fields |
| `openspec/backlog.md` | Modify | Item #1 struck through; new follow-up item |
| `.../domain/StudentTrack.kt` | Unchanged | Persistence contract |

## Migration / Rollout — slicing

Four sequential PR slices, each independently buildable and under the 400-line budget
(`ask-on-risk` is the active delivery strategy):

| Group | Scope | Est. lines |
|---|---|---|
| A | `values/` + `values-en/` skeleton + `StudentTrackLabels.kt` + `OnboardingScreen.kt` | ~180 |
| B | `LessonMapScreen`, `TheorySheet`, `PlaceholderScreen`, `LessonMapNode` | ~140 |
| C | `RegisterScreen` ("Back", step indicator, labels), `HomeDashboardScreen` (`JoinCourseCard`, level, alt text) | ~150 |
| D | `LoginScreen`, `ProfileScreen`, `AuthScreenScaffold`, `AuthenticatedHomeScaffold`, `schoolYearLabel` refactor + tests, backlog sync | ~200 |

Both XML files grow additively per group; Group A creates them with only its own keys. Rollback is a
per-group revert.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Compile | Every referenced `Res.string.*` exists | Generated accessors are compile-checked; `:composeApp:assembleDebug` is the gate |
| Unit | `StudentTrack.parse()` round-trips every `displayName`; repository still stores `displayName` | Existing `SqlDelightLearnerProfileRepositoryTest` — add an explicit round-trip case in Group A |
| Unit | `ProfileViewModel`/`HomeDashboardViewModel` expose `schoolYear` + `studentTrack` | Update the two existing tests in Group D |
| Render | Existing `jvmTest` render tests still find their nodes | `ProfileScreenTest`, `ProfileRedesignRenderTest`, lesson-map tests; any test asserting an English literal must be updated in the same group |
| Suite | `./gradlew :composeApp:jvmTest` and `:composeApp:assembleDebug` | Run per group, not just at the end |

## Verification Approach

After each group, run against `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui`:

```
rg -n '(Text\(\s*"|text = "|label = \{ Text\("|placeholder = \{ Text\("|contentDescription = ")' --glob '!**/*ViewModel.kt'
```

Baseline today: 189 quoted-literal hits across 22 files (includes non-UI strings). This is a
**heuristic, not a proof** — it misses concatenated templates, `when` branches returning literals,
and strings passed as default parameter values (e.g. `PlaceholderScreen(message = "...")`). Two
complements are required per group:

1. A manual read of the group's diff for `"` inside any `@Composable`.
2. A separate `contentDescription`/`semantics` sweep, since accessibility text is user-facing but is
   not caught by `Text(` patterns (`LessonMapNode.kt:79-93`, `ProfileScreen.kt:229,311`,
   `LessonMapScreen.kt:451`, `AuthScreenScaffold.kt:102`, `HomeDashboardScreen.kt:277`).

ViewModel files are explicitly excluded from the grep (proposal out-of-scope).

## Backlog Sync (Group D)

- Strike through item #1 of "Onboarding and navigation bug fixes" as resolved, with the change name.
- Add: **"Localize ViewModel-produced display strings via sealed types."** Covers `errorMessage:
  String` fields, raw `Throwable.message`, and the already-Spanish-but-hardcoded
  `salutation()`/`greetingFor()` in `HomeDashboardViewModel.kt:158-167`.

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or
process-integration boundary. This change only moves string literals into resource files.

## Open Questions

- [ ] None blocking. The `schoolYearLabel` state-shape refactor is a scope refinement (a display
  string the proposal did not enumerate, and not an `errorMessage`); flagged for orchestrator
  acknowledgement.
