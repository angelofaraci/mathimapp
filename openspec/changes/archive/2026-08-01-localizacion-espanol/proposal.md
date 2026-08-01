# Proposal: Localize UI Copy to Spanish via Compose Resources

## Intent

App copy is hardcoded literals in Compose screens: most screens are already Spanish, but `OnboardingScreen` is fully English and fragments leak English elsewhere. Learners (Argentina primary/secondary) hit an English wall right after registration. Backlog item #1 of "Onboarding and navigation bug fixes". Goal: a single localization indirection (`stringResource`) with Spanish as default, so copy is consistent, translatable, and no longer a per-screen one-off.

## Scope

### In Scope

- Add `composeApp/src/commonMain/composeResources/values/strings.xml` (Spanish default) and `values-en/strings.xml` (English fallback).
- Migrate hardcoded copy in `OnboardingScreen.kt`, `LessonMapScreen.kt`, `TheorySheet.kt`, `PlaceholderScreen.kt`, `RegisterScreen.kt` ("Back"), `HomeDashboardScreen.kt` (`JoinCourseCard`).
- Translate the remaining English literals above to Spanish.
- Migrate already-Spanish literals in Login/Register/Profile/Home/`AuthenticatedHomeScaffold`/`LessonMapNode` so indirection is complete, not partial.
- Add a UI-layer localized label for `StudentTrack` (e.g. `@Composable fun StudentTrack.localizedLabel()`), leaving `StudentTrack.displayName` untouched.
- Preserve interpolation via `%s`/`%d` placeholders (`Nivel %d`, `%d/%d Lecciones`, `Paso %d / 3`).
- Add backlog item: "Localize ViewModel error messages via sealed error types".

### Out of Scope

- ViewModel `errorMessage: String` fields and raw `Throwable.message` (deferred to the new backlog item).
- `androidMain/res/values/strings.xml` (Android app label only).
- Server, admin-web, code identifiers, comments, runtime locale switcher.

## Capabilities

### New Capabilities

- `ui-localization`: user-facing copy resolves from Compose Multiplatform string resources with Spanish default and English fallback.

### Modified Capabilities

- `onboarding-flow`: onboarding copy MUST render in Spanish (currently specified/implemented in English).

## Approach

Additive. `composeResources/` already exists and is wired (drawables/fonts in use), so no Gradle change is needed. Introduce `values/strings.xml` (es) + `values-en/strings.xml`, then replace literals screen-by-screen with `stringResource(Res.string.*)`. Keys use `screen_element` naming. Domain stays Compose-free; `StudentTrack` gets a UI-side mapping.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/composeResources/values*/strings.xml` | New | es default + en fallback |
| `composeApp/src/commonMain/kotlin/.../ui/**` | Modified | literals → `stringResource` |
| `composeApp/src/commonMain/kotlin/.../domain/StudentTrack.kt` | Unchanged | parse identity preserved |
| `openspec/backlog.md` | Modified | mark #1 done, add error-message follow-up |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Touching `StudentTrack.displayName` breaks `parse()` | Med | Explicit non-goal; UI-only mapping |
| >400 changed lines across screens | High | Slice by screen group in tasks phase |
| Tests asserting UI literals | Low | UI copy tests are minimal; ViewModel errors excluded |
| Missing/renamed keys at runtime | Med | Generated `Res.string` is compile-checked |

## Rollback Plan

Revert the change branch. The two `strings.xml` files are additive; deleting them plus reverting screen edits restores prior literals with no data or schema impact.

## Dependencies

None. Compose Multiplatform 1.8.0 + `libs.compose.components.resources` already present.

## Success Criteria

- [ ] No user-facing string literal remains in commonMain Compose screens (excluding ViewModel error text).
- [ ] Onboarding, lesson map, theory sheet, placeholder, join-course, and register flows render fully in Spanish.
- [ ] `./gradlew :composeApp:jvmTest` and `:composeApp:assembleDebug` pass.
- [ ] `StudentTrack.parse()` behavior unchanged.
- [ ] Backlog updated: item #1 resolved, error-message localization logged.

## Proposal question round

Confirmed by user:

1. Spanish variant = neutral Latin American Spanish (no "vos"/voseo).
2. `values-en/` is a fallback only; no in-app language switcher ships now.
3. Existing Spanish copy is moved verbatim into resources, not re-edited for tone.
4. English-only strings get new translations authored during apply (no glossary review step).
