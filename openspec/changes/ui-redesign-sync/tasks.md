# Tasks: UI Redesign Sync

## Review Workload Forecast

Decision needed before apply: Yes
Chained PRs: Yes
Chain strategy: stacked-to-main
400-line budget risk: Medium

| PR | Est. Lines | Risk |
|----|------------|------|
| 1 foundation/tokens | ~160 | Low |
| 2 feature/auth | ~120 | Low |
| 3 feature/profile | ~250 | Med |
| 4 feature/home | ~180 | Low |
| 5 feature/lesson-map | ~350 | Med |
| 6 feature/exercise-states | ~370 | Med |

## PR 1: foundation/tokens (~160 lines)

- [x] 1.1 Bundle Sora `.ttf` (4 weights) in `composeResources/font/`; add `OFL.txt`
- [x] 1.2 Add `Track`, `Lock`, `Stripe` colors to `ColorTokens.kt`
- [x] 1.3 Update `AppShapeTokens`: add checkbox, iconBox, socialButton, stepSegment; card=18, button=16, field=15
- [x] 1.4 Sora `FontFamily` + scale in `TypeTokens.kt`
- [x] 1.5 Wire shapes into MaterialTheme in `AppTheme.kt`
- [x] 1.6 Coral `shadow(12.dp)` on `MButton.Filled`; 18dp on `MCard.kt`
- [x] 1.7 8dp linear `MProgressIndicator` with track color
- [x] 1.8 Test: `AppThemeTokensTest`
- [ ] 1.9 Manual visual check: Sora font + CTA shadow

## PR 2: feature/auth (~120 lines)

- [x] 2.1 `LoginScreen.kt`: social 14dp, divider 12/600 muted + lines, footer coral
- [x] 2.2 `RegisterScreen.kt`: step 5px/999px, JetBrains Mono, strength teal, checkbox 22×22 7dp
- [x] 2.3 `AuthScreenScaffold.kt`: logo 18dp, title 32/800
- [ ] 2.4 Manual visual check: Jul 16 screenshot

## PR 3: feature/profile (~250 lines)

- [x] 3.1 Streak chip in `ProfileScreen.kt`: coral pill + flame
- [x] 3.2 Logout restyle: surface card, icon + text, 16dp
- [x] 3.3 `ProfileNavigationCard.kt`: 42×42 icon box, 13dp, colored SVGs
- [x] 3.4 `ProfileListRow.kt` leading icon; sub-screens
- [ ] 3.5 Manual visual check: `perfil-usuario.png` (currently blocked by the documented PNG/spec conflict)

## PR 4: feature/home (~180 lines)

- [x] 4.1 `HomeDashboardViewModel.kt`: course list, streak capped 7
- [x] 4.2 Greeting: inline coral pill, subtitle
- [x] 4.3 Progress card: level, 8dp teal bar, XP, 18dp card
- [x] 4.4 Course cards
- [ ] 4.5 Manual visual check: `inicio-dashboard.png`

## PR 5: feature/lesson-map (~350 lines)

- [x] 5.1 `LessonMapNode.kt`: 56dp circle, state icons, locked non-clickable
- [x] 5.2 Header: back, title, coral pill, 8dp progress bar
- [x] 5.3 Canvas polyline: verticalScroll + Box, solid/dashed
- [x] 5.4 Serpentine nodes and tap wiring
- [x] 5.5 Automated render and ViewModel tests
- [ ] 5.6 Manual visual check: `mapa-leccion.png`

## PR 6: feature/exercise-states (~370 lines) — NOT STARTED

- [ ] 6.1 Header: close X, hearts, 8dp coral progress bar
- [ ] 6.2 Question 22dp + border; 2-col grid, coral glow
- [ ] 6.3 Hint link + bottom-fixed CTA 18dp + shadow
- [ ] 6.4 `TheorySheet.kt` sections; `OnboardingScreen.kt` SelectionCard 18dp, buttons 16dp
- [ ] 6.5 `PlaceholderScreen.kt` empty/loading states
- [ ] 6.6 Automated exercise/player/state tests
- [ ] 6.7 Manual visual check: `ejercicio-gameplay.png` and onboarding/state references

## Suggested Work Units

| Unit | PR | Test | Harness | Rollback |
|------|----|------|---------|----------|
| Tokens | 1 | `AppThemeTokensTest` | N/A | `ui/theme/*`, `composeResources/` |
| Auth | 2 | ViewModel + semantics | Render | `LoginScreen.kt`, `RegisterScreen.kt`, `AuthScreenScaffold.kt` |
| Profile | 3 | `ProfileViewModelTest` | Hub render | `ProfileScreen.kt`, `ProfileNavigationCard.kt`, `ProfileListRow.kt` |
| Home | 4 | `HomeDashboardViewModelTest` | Dashboard | `HomeDashboardScreen.kt`, `HomeDashboardViewModel.kt` |
| Lesson | 5 | Node palette | Path render | `LessonMapScreen.kt`, `LessonMapNode.kt` |
| Exercise | 6 | Grid+hearts+CTA | Player render | `ExercisePlayerContent.kt`, `TheorySheet.kt`, `OnboardingScreen.kt`, `PlaceholderScreen.kt` |
