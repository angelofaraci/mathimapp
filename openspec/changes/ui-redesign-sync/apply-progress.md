# Apply Progress: UI Redesign Sync

## Slice Status

| Slice | Status | Notes |
|-------|--------|-------|
| 1 — Foundation/tokens (PR 1) | IMPLEMENTED + TESTED | Manual visual check pending |
| 2 — Auth | IMPLEMENTED + TESTED | Automated tests green (24 suites / 110 tests); manual Jul 16 screenshot check pending |
| 3 — Profile | IMPLEMENTED + TESTED | Automated tests green (25 suites / 115 tests); manual Jul 21 PNG check **blocked by structural PNG↔spec conflict** (see Slice 3 Issues) |
| 4 — Home | IMPLEMENTED + TESTED | Automated tests green (27 suites / 123 tests); manual `inicio-dashboard.png` pixel check pending |
| 5 — Lesson map only | IMPLEMENTED + TESTED; VISUAL CHECK PENDING | Automated tests green (27 suites / 128 tests); maintainer approved `size:exception` for ONE PR (~705 actual vs ~775 approved); manual `mapa-leccion.png` pixel check not performed |
| 6 — Exercise player + TheorySheet + onboarding + states | NOT STARTED | No Slice 6 implementation or verification has been performed |

## Recovery Boundaries

The recovered worktree contains unrelated changes that are **not part of Slices 1–6**. Preserve them as-is and exclude them from redesign branches, commits, staging, and rollback boundaries:

- `package.json`
- `package-lock.json`
- `scripts/configure-android-wsl-portproxy.ps1` (existing deletion)
- `openspec/backlog.md`
- Reference PNG imports outside the redesign slice scope

Do not delete or restore these paths while recovering the redesign; separate ownership must be resolved independently.

## Mode

Standard mode (`strict_tdd: false` in `openspec/config.yaml`). Delivery: chained PR slices of 6, strategy `stacked-to-main` (per tasks.md Review Workload Forecast). Slice 2 stacks on Slice 1 (`feature/auth-redesign` ← `foundation/ui-redesign-tokens`).

## Files Changed (Slice 1)

| File | Action | Lines (+/-) | What |
|------|--------|-------------|------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/ColorTokens.kt` | Modified | +4/-0 | `BrandTrack` #EADFD1, `BrandLock` #CBBEAE, `BrandStripe` #F2E9DD, `BrandCoralShadow` #6BF2654B |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/ShapeTokens.kt` | Modified | +14/-4 | Added `checkbox` 7, `iconBox` 13, `socialButton` 14, `stepSegment` 999; card 28→18, button 20→16, field 18→15 |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/TypeTokens.kt` | Modified | +36/-14 | `rememberSoraFontFamily()` (composeResources, 4 weights), pure `buildAppTypography(fontFamily)` with redesign scale, `AppTypography` now a `@Composable` getter |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/AppTheme.kt` | Modified | +2/-0 | Wired `extraSmall`=checkbox, `extraLarge`=pill into MaterialTheme `Shapes` |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MButton.kt` | Modified | +8/-1 | `Modifier.shadow(12.dp, shape, ambient/spot = BrandCoralShadow)` on Filled variant |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MProgressIndicator.kt` | Modified | +10/-2 | Linear variant: 8dp height, pill clip, round caps, `BrandTrack` track color |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/theme/AppThemeTokensTest.kt` | Modified | +38/-11 | New semantic-color test; updated shape values; typography asserts redesign scale + family injection |
| `composeApp/src/commonMain/composeResources/font/sora_{regular,semibold,bold,extrabold}.ttf` | New (pre-existing from prior batch, verified) | — | Sora OFL, 4 weights |
| `composeApp/src/commonMain/composeResources/files/OFL.txt` | New (pre-existing from prior batch, verified) | — | Sora license |

Authored slice total: ~112 insertions + 32 deletions across 7 Kotlin files (well under the ~160-line forecast; 400-line budget not at risk).

`MCard.kt` and `MTextField.kt`: intentionally unchanged — card 18dp arrives via `shapes.large`, field 15dp via `shapes.small`; `authStyle` already hardcoded 15dp (now token-aligned).

## Files Changed (Slice 2)

| File | Action | Lines (+/-) | What |
|------|--------|-------------|------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginScreen.kt` | Modified | +4/-8 | Forgot-password link → `bodyMedium` (13/600); divider text → `labelMedium` (12/600 muted, lines unchanged); footer → `titleSmall` (14/500 muted) + `titleMedium` (14/700 coral) "Registrate", underline removed per handoff |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterScreen.kt` | Modified | +64/-13 | Step segments 6dp/3dp → 5dp/`stepSegment` (999dp), gap 8→6dp; label "Paso X de 3" → "Paso X / 3" in `labelMedium` (12/600) with mono family; strength fill `primary` → `secondary` (teal #0E9E8E); M3 `Checkbox` → custom 22×22dp box, `checkbox` 7dp radius, coral + on-coral Canvas checkmark, row now single `toggleable(role = Checkbox)` |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthScreenScaffold.kt` | Modified | +24/-5 | Brand mark card radius `card` (18) → `button` token (16dp per handoff); wordmark → 22sp/800, −0.02em, two-tone "Mathim" ink + "App" muted/500 via `AnnotatedString` (single semantics node); form title kept 27/800 (see Deviations) |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MButton.kt` | Modified | +13/-3 | `Social` variant: shape `shapes.medium` (16dp) → `socialButton` token (14dp), border 1→1.5dp `outlineVariant`; Filled/Outline untouched (CTA 16dp + coral shadow already from Slice 1) |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/AuthRedesignRenderTest.kt` | New | +109 | 3 render tests: login redesign copy + "Registrate" navigation; "Paso N / 3" copy follows wizard step; terms checkbox 22×22dp + role/toggle behavior through the row |

Authored slice total: ~114 insertions + 29 deletions across 4 main-source files (+109-line new test file) = ~252 changed lines. Above the ~120-line forecast but far under the 400-line budget — Low risk holds for PR 2.

Theme files (`ui/theme/*`, `MProgressIndicator.kt`): intentionally untouched in this slice — consumed Slice 1 tokens only (`socialButton`, `stepSegment`, `checkbox`, `button`, `secondary`).

## Files Changed (Slice 3)

| File | Action | Lines (+/-) | What |
|------|--------|-------------|------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileScreen.kt` | Modified | +151/-59 | Streak chip + restyled role chip (shared `ProfileChip`: surface pill, 1px line border, 11/700); nav-card icons → tinted vectors (coral/teal/rose/muted); logout → `Surface(onClick)` card 16dp + logout icon + ink text; version caption `"MathimApp · versión ${appVersionName()}"`; header bar 38×38/12dp/surface2 + centered 17/700 title + 38dp spacer; row icons on all 4 sub-screens; dark-mode stub toggle; initials 32/800 + avatar/badge 3px borders; `✎`/`←` glyphs → vectors |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileNavigationCard.kt` | Modified | +11/-5 | Icon box 48→42×42dp, bg primaryContainer→surface2, radius `iconBox` (13dp); title → `titleMedium` 14/700 (dropped SemiBold override); subtitle → `labelSmall` 11/500 muted |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileListRow.kt` | Modified | +14/-0 | New `leadingIcon: DrawableResource?` param — 18dp muted vector, `testTag("rowLeadingIcon")` |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileToggleRow.kt` | Modified | +25/-2 | Same `leadingIcon` param; Switch colors: checked track teal (`secondary`), unchecked track `BrandTrack` (handoff toggle spec) |
| `composeApp/src/commonMain/composeResources/drawable/ic_*.xml` | New (17 files) | +368 | Lucide/Feather-style stroke vectors (#111827/1.8/round, tinted at use): flame, person, settings, help_circle, info, mail, lock, bell, volume, moon, globe, logout, flag, file_text, shield, edit, arrow_left |
| `composeApp/src/commonMain/kotlin/.../ui/AppVersion.kt` (+ android/jvm/ios actuals) | New (4 files) | +21 | `expect fun appVersionName()`; androidMain → `BuildConfig.VERSION_NAME`; jvm/ios pinned `"1.0"` (no BuildConfig there) |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/ProfileScreenTest.kt` | Modified | +3/-3 | Version caption assertion → "MathimApp · versión 1.0" (visual copy only; behavior assertions untouched) |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/ProfileRedesignRenderTest.kt` | New | +135 | 5 render tests: hub chips/boxes/logout/version; streak-chip omission at 0; Cuenta header+3 row icons; Preferencias dark-mode no-op stub + 4 row icons; Ayuda/Acerca headers+icon rows |

Authored slice total: +204/-69 modified Kotlin (273) + 524 new (368 drawable assets, 21 platform glue, 135 tests) = **~797 changed lines vs ~250 forecast → over the 400 budget.** 46% of the diff is 17 independent vector assets; the logic-adjacent Kotlin core matches the forecast. See Slice 3 PR Boundary for the recommended handling.

Theme files (`ui/theme/*`): untouched — consumed Slice 1 tokens only (`iconBox`, `button`, `pill`, `BrandTrack`, `secondary`, `error`, `outlineVariant`). No new Gradle dependency (no material-icons; resources via composeResources like existing tab icons).

## Test Evidence (Slice 3)

- **Focused test command**: `./gradlew :composeApp:jvmTest --console=plain` (same SDK workaround as Slices 1–2; `local.properties` restored, 0 occurrences verified).
- **Result**: `BUILD SUCCESSFUL in 2m 43s` — **25 suites, 115 tests, 0 failures, 0 errors, 0 skipped** (was 24 suites / 110 tests; +1 suite `ProfileRedesignRenderTest`, +5 tests).
- `ProfileRedesignRenderTest`: 5/5 green —
  - `hub renders streak chip 42dp nav icon boxes logout card and dynamic version` ("Racha 5 días" + `streakChip` tag; 4 `navIconBox` @ 42×42dp; "Cerrar sesión"; "MathimApp · versión 1.0")
  - `streak chip is omitted when the user has no streak` (handoff "omit if not exists" rule locked in)
  - `account sub screen renders header bar and leading row icons` ("Volver" + 3 `rowLeadingIcon`)
  - `preferences sub screen renders dark mode stub toggle as a no-op` (3 switches; stub `assertIsOff → click → assertIsOff`)
  - `help and about sub screens render header bars and icon rows`
- `ProfileScreenTest` 2/2 green (logout click + hub↔Cuenta↔back navigation preserved; only the version-caption string assertion updated).
- `ProfileViewModelTest` 3/3 untouched and green — no ViewModel contract change.
- No RED→GREEN cycles this batch — suite green on first run.
- **Runtime harness**: compose render tests exercise the real `ProfileContent` + `AppTheme` (Sora) + composeResources vectors on JVM; hub→sub-screen→back navigation, logout click, and the no-op dark-mode stub all executed.
- Not semantics-observable, deferred to manual check: chip border/hairline rendering, icon glyph fidelity vs handoff, switch track colors, avatar/badge 3px borders.
- **Rollback boundary**: revert touches only `ui/ProfileScreen.kt`, `ui/primitives/{ProfileNavigationCard,ProfileListRow,ProfileToggleRow}.kt`, `ui/AppVersion*.kt` (4), `composeResources/drawable/ic_*.xml` (17), `jvmTest/.../{ProfileScreenTest,ProfileRedesignRenderTest}.kt` — no theme token, navigation, ViewModel, or contract changes.

## Suggested PR / Commit Boundary (Slice 3)

- **Branch**: `feature/profile-redesign` → targets `feature/auth-redesign` (stacked-to-main, per design Migration order).
- **Boundary**: starts at Slice 2 branch tip, ends with this slice — profile hub + sub-screens visuals, icon assets, version glue, render tests. No home/map/exercise screens, no dark-mode behavior, no ViewModel or navigation changes.
- **Suggested single work-unit commit**: `feat(ui): sync profile hub and sub-screens to redesign handoff` — hub chips/cards/logout + sub-screen header/icons + 17 vector assets + `appVersionName()` glue + render tests (tests with the behavior they verify).
- **400-line budget**: actual ~797 vs forecast ~250 → **budget exceeded. Maintainer decision required**: recommend `size:exception` (46% of the diff is 17 self-contained ≤45-line vector assets with zero logic; the Kotlin core is ~273 lines, on-forecast). Alternative if the exception is declined: split 3a hub (`ProfileScreen` hub + `ProfileNavigationCard` + chips/logout + `AppVersion` + 8 hub drawables + hub tests) / 3b sub-screens (rows/toggles + header bar + 9 row drawables + sub-screen tests).
- **Rollback boundary**: per-file revert of the Files Changed (Slice 3) list; Slices 1–2 remain valid without this slice.

## Deviations from Design (Slice 3)

1. **`perfil-usuario.png` (Jul 21) conflicts STRUCTURALLY with the spec deltas — specs/handoff implemented, PNG flagged.** The PNG shows a different profile concept: "Mi Perfil" title, XP progress card ("340 / 500 XP" + teal bar), stat cards ("7 días / Racha actual 🔥", "42 / Lecciones completas"), "MIS LOGROS" achievements + "Ver todas", member-since subtitle — and **no nav cards, no logout card, no version caption**. The spec deltas + Jul 16 handoff describe the nav-hub implemented here (orchestrator scoped this slice "visual-only per specs"). The PNG-wins rule was applied only to styling cues (teal/coral accents, surface cards on cream), which already match the tokens. **Design review required before archive** (design.md Open Question anticipated this). Note: `ProfileViewModel` already exposes `streak`/`completedLessons`/`achievements`/`level`/`currentXp` — the data layer anticipates the PNG layout, suggesting a planned profile v3; if the PNG is confirmed authoritative, that is a new change with spec amendments (structural, not visual-only), not a fix to this slice.
2. **Logout card 16dp, NOT 18dp** — tasks.md 3.2 says 18dp, but the spec delta ("16px radius") and handoff ("radio 16px") say 16. Delivered via the existing `button` shape token (16dp; theme files frozen). tasks.md shorthand treated as stale (same pattern as Slice 2 logo 18dp).
3. **Streak chip omitted when `streak == 0`** — the requirement text reads unconditional, but the scenario conditions on "user with streak" and the handoff rule says "si un ítem mostrado no existe (p. ej. racha), omitir el chip". Resolved in favor of scenario+handoff; locked by test. If the requirement is literal, flip the `streak > 0` guard.
4. **Dynamic version via new `expect/actual appVersionName()` (4 files)** — design's Slice 3 file list omitted it, but "version dynamic string" is impossible cross-platform without it. androidMain is truly dynamic (`BuildConfig.VERSION_NAME`, same import pattern as `ApiBaseUrl.android.kt`); jvm/ios pin `"1.0"` to the declared `versionName` (no BuildConfig on those targets — a shared build-time source is follow-up work).
5. **`ProfileToggleRow.kt` edited (+25/-2) though absent from design's Slice 3 file list** — the spec scenario mandates bell/volume/moon icons on Preferencias rows (3 of 4 are toggle rows) and "Switch with track color". Added `leadingIcon` + teal-on/`BrandTrack`-off colors per the handoff toggle spec. Design table was incomplete; spec won.
6. **`✎` and `←` glyphs replaced by vectors** — Slice 1 switched the family to Sora; dingbat/arrow codepoints are not guaranteed in Sora and would fall back to system fonts nondeterministically. Semantics ("Editar avatar", "Volver") preserved. `›` chevrons kept as text (Latin-1 guillemet, universally present).
7. **Handoff-exact identity polish beyond spec minimum**: initials 27→32/800 (`headlineLarge`), removed `FontWeight.Bold` overrides that downgraded 800-weight tokens to 700 (name, initials, card titles), added 3px surface/app-bg borders on avatar + edit badge. Same rationale as Slice 2's wordmark: explicit in the handoff's component spec.
8. **Both chips restyled to handoff** — role chip was secondaryContainer/no-border/`labelLarge`; the "matches role chip styling" scenario + handoff chip spec require surface bg + 1px line border + pill + 11/700 for both. Role chip text teal, streak chip coral + flame.
9. **Icons are Lucide/Feather-style stroke vectors** (ISC/MIT geometry) matching the project's existing stroke style (#111827, 1.8dp, round caps/joins), tinted via `Icon(tint)`. No `material-icons` dependency added (AGENTS.md dependency rule).
10. **Version caption at 11sp (`labelSmall`), not the handoff's 10px** — no 10sp slot exists in the token scale and hardcoding `10.sp` would be a magic number; minor fidelity gap flagged for design review.

## Issues Found (Slice 3)

- **PNG↔spec structural drift (see Deviation 1)** — the Jul 21 `perfil-usuario.png` cannot be reconciled with the spec deltas within a visual-only slice. This blocks task 3.4's manual pixel comparison: there is no implemented screen comparable to the PNG. Escalate to design review before archive.
- iOS/Android compilation not verified locally (Linux env runs `jvmTest` only). androidMain actual reuses the verified `BuildConfig` import pattern from `ApiBaseUrl.android.kt`; iosMain actual is a trivial constant. Recommend CI or a local `:composeApp:assembleDebug` + iOS compile before PR 3 review closes.

## Risks / Pending (Slice 3)

- **Manual visual verification pending** (task 3.4 second half): pixel comparison vs `perfil-usuario.png` — **blocked by the structural conflict** (Deviation 1); compare against `Perfil v2.dc.html` instead once the design authority question is resolved.
- **400-line budget exceeded** (~797 actual vs ~250 forecast) — maintainer must pick `size:exception` or the 3a/3b split (see Slice 3 PR Boundary) before PR creation.
- Switch unchecked track now uses `BrandTrack` and checked track teal for ALL `ProfileToggleRow`s — consistent with the handoff toggle spec; confirm no other screen consumes `ProfileToggleRow` expecting coral (only Preferencias uses it today).
- `streak > 0` visibility guard (Deviation 3) and 11sp caption (Deviation 10) are judgment calls flagged for design review.

## Files Changed (Slice 4)

| File | Action | Lines (+/-) | What |
|------|--------|-------------|------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardScreen.kt` | Modified | +127/-40 | Greeting row: `headlineSmall` (21/800) + " 👋" appended in screen layer, coral streak pill right (omitted at `streak == 0`, `homeStreakPill` tag, primary bg + `ic_flame` 14dp + "+N días" 12/700); subtitle "¡Es hora de practicar hoy!" replaces old copy, `schoolYearLabel` pill removed from UI; progress card → `MCard` default (18dp/1px border, surface) with "Nivel {level}" 14/700 + "{currentXp} / {xpForNextLevel} XP" 12/600 teal + teal 8dp bar; "MIS CURSOS EN PROGRESO" `labelMedium` muted, 1sp tracking; `CourseProgressCard` (44dp `secondaryContainer` circle + "÷" glyph, title, "Progreso: {N}%", teal "Ir" pill → `onOpenLessonMap`); catalog CTA + `ContinueLearningCard` + `JoinCourseCard` verbatim |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardViewModel.kt` | Modified | +30/-3 | `activityCount` → `streak` (identical `min(completed, 7)` math); `currentXp`/`xpForNextLevel` mirror `ProfileViewModel` (`totalScore % XpPerLevel`, `XpPerLevel`); `HomeCourseProgress` + `inProgressCourses` from `courseRepository.getEnrolledCourses` (percent = completed∩lessons/lessons×100, 0-lesson guard), course-fetch failure degrades to `emptyList()` via `runCatching` |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MProgressIndicator.kt` | Modified | +4/-2 | New `color: Color = primary` param on `MLinearProgressIndicator` (zero existing callers — additive, safe) |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/home/HomeDashboardViewModelTest.kt` | Modified | +88/-6 | `activityCount`→`streak` assertions renamed, same values (7, 0); +`currentXp`/`xpForNextLevel` asserts; 2 new tests: per-course completion percentages, course-fetch failure degradation; fake `getEnrolledCourses` backing |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/home/HomeDashboardRedesignRenderTest.kt` | New | +197 | 6 render tests: greeting/wave/pill; pill omitted at 0; Nivel/XP card + 68% bar via `ProgressBarRangeInfo`; zero → "0 / 100 XP" + empty bar; courses section + "Ir" pill + catalog CTA navigation; enrolled-no-progress keeps `ContinueLearningCard` |

Authored slice total: **497 changed lines** (300 modified Kotlin + 197 new tests) vs ~180 forecast and ~440–470 pre-approved estimate → **over the 400 budget; maintainer pre-approved `size:exception` for ONE PR (4a/4b split explicitly declined).** See Slice 4 PR Boundary.

Theme files (`ui/theme/*`): untouched — consumed Slice 1 tokens only (`BrandTrack`, `secondary`, `secondaryContainer`, `onSecondary`, typography slots). `ic_flame` reused from Slice 3 assets (no new drawable). `MCard` default border/radius consumed unchanged.

## Test Evidence (Slice 4)

- **Focused test command**: `./gradlew :composeApp:jvmTest --console=plain` (same SDK workaround as Slices 1–3; `local.properties` restored, 0 occurrences verified).
- **Result**: `BUILD SUCCESSFUL in 1m 23s` — **27 suites, 123 tests, 0 failures, 0 errors, 0 skipped** (was 25 suites / 115 tests; +1 suite `HomeDashboardRedesignRenderTest` +6 tests, `HomeDashboardViewModelTest` 7→9 tests).
- `HomeDashboardRedesignRenderTest`: 6/6 green —
  - `greeting row renders wave subtitle and coral streak pill` ("Hola, María 👋", "¡Es hora de practicar hoy!", `homeStreakPill` tag, "+7 días")
  - `streak pill is omitted when the user has no streak` (Slice 3 handoff omission rule applied to home)
  - `progress card renders level XP text and a 68 percent bar` ("Nivel 5", "340 / 500 XP", `ProgressBarRangeInfo(340f/500f, 0f..1f, 0)` — spec mock literals passed straight to the content composable)
  - `zero progress renders nivel 0 and an empty bar` ("Nivel 0", "0 / 100 XP", bar 0f)
  - `courses section renders header course cards and ir pill opens the lesson map` ("MIS CURSOS EN PROGRESO", "Progreso: 45%", 2× "Ir" → `onOpenLessonMap` fired; "Abrir mapa de lecciones" → fired)
  - `enrolled dashboard without in-progress courses keeps the continue learning card` ("Ir al mapa" → `onContinueLearning`; catalog CTA preserved)
- `HomeDashboardViewModelTest` 9/9 green — streak rename keeps identical math/values; no behavior assertion weakened.
- One RED→GREEN cycle this batch: first run failed compiling `HomeDashboardRedesignRenderTest` — `Unresolved reference 'onNode'`. In this Compose version `onNode`/`onAllNodes` are **members** of `SemanticsNodeInteractionsProvider` (no import exists); fixed test-side by dropping the bogus import (production code unchanged).
- **Runtime harness**: compose render tests exercise the real `HomeDashboardContent` + `AppTheme` (Sora) + Slice 3 vector assets on JVM; "Ir" pill, catalog CTA, and ContinueLearningCard callbacks all executed.
- Not semantics-observable, deferred to manual check: coral pill/teal bar colors, 44dp circle tint (`secondaryContainer` = teal@14%), "÷" glyph rendering in Sora, 1sp letter-spacing pixels.
- **Rollback boundary**: revert touches only `ui/home/{HomeDashboardScreen,HomeDashboardViewModel}.kt`, `ui/primitives/MProgressIndicator.kt` (the `color` param lines), `commonTest/.../HomeDashboardViewModelTest.kt`, `jvmTest/.../home/HomeDashboardRedesignRenderTest.kt` — no theme token, navigation, repository-contract, or persistence changes.

## Suggested PR / Commit Boundary (Slice 4)

- **Branch**: `feature/home-redesign` → targets `feature/profile-redesign` (stacked-to-main, per design Migration order).
- **Boundary**: starts at Slice 3 branch tip, ends with this slice — home dashboard visuals + VM state fields (streak rename, XP mirror, in-progress courses) + primitive `color` param + render/VM tests. No lesson-map/exercise screens, no navigation or repository-contract changes.
- **Suggested single work-unit commit**: `feat(ui): sync home dashboard to redesign handoff` — greeting/streak pill + Nivel/XP progress card + MIS CURSOS course cards + VM state + tests (tests with the behavior they verify).
- **400-line budget**: actual 497 vs forecast ~180 → **budget exceeded; maintainer pre-approved `size:exception` as ONE PR** (~440–470 estimated; actual +6% over the estimate, driven by the 197-line render test — the 6th test covers the enrolled-no-progress fallback the plan requires preserved). The 4a/4b split was explicitly declined. Production Kotlin is 206 lines, on-forecast; the remainder is verification.
- **Rollback boundary**: per-file revert of the Files Changed (Slice 4) list; Slices 1–3 remain valid without this slice.

## Deviations from Design (Slice 4)

1. **Spec-number mock literals are unreachable with `XpPerLevel = 100`** — "340/500 XP" at level 5 and "0/0 XP" cannot occur in real state (`currentXp ∈ [0,99]`, `xpForNextLevel = 100`). Implemented as formatted state values; the 68% bar scenario is honored by passing the mock literals straight to the content composable in the render test (`ProgressBarRangeInfo` semantics). Real zero progress renders "Nivel 0" + "0 / 100 XP" + empty bar (locked by test).
2. **`schoolYearLabel` pill removed from the home UI, VM field kept** — not present in `inicio-dashboard.png` (PNG-authoritative per resolved plan, Engram #69). The VM still computes it (existing VM test assertion untouched); removing the field would be a contract change beyond visual scope.
3. **Wave " 👋" appended in the screen layer only** — `greetingFor` untouched, preserving clock logic and the `endsWith(name)` behavior assertions. Spec scenario copy "Hola, María 👋" is mock copy; the time-based salutation requirement is unchanged.
4. **Streak pill omitted when `streak == 0`** — same resolution as Slice 3 Deviation 3 (scenario conditions on "user with streak"; handoff omits missing items); locked by test.
5. **All course cards use the static "÷" glyph** (Latin-1, Sora-safe) — the PNG shows "÷"/"×" per course but no per-course icon contract exists in spec/design; resolved plan picked the single glyph. Per-course deep-link from "Ir" is out of scope (no contract — pill navigates to the lesson map like the catalog CTA).
6. **"Ir"/streak pill text at 12sp/700 via `labelMedium` + `FontWeight.Bold` override** — the token slot is 12/600; the handoff weight (700) requires the override. No token edit (theme files frozen per slice boundary).

## Issues Found (Slice 4)

- iOS/Android compilation not verified locally (Linux env runs `jvmTest` only). New code is `commonMain`-only Compose + an `expect`-free VM; the only resource consumed (`ic_flame`) already shipped in Slice 3. Recommend CI or a local `:composeApp:assembleDebug` + iOS compile before PR 4 review closes.
- `HomeDashboardContent` branches on `inProgressCourses.isNotEmpty()`; an enrolled user whose courses all have 0% progress still sees the courses section (0% cards) rather than `ContinueLearningCard` — matches the resolved plan ("percent computed, no filter") and the PNG's 12% card, but flag for design review if 0% should fall back to the empty state.

## Risks / Pending (Slice 4)

- **Manual visual verification pending** (task 4.4 second half): pixel comparison vs `inicio-dashboard.png` — pill/bar colors, circle tint, glyph, tracking. Required before PR 4 review closes.
- **`size:exception` accepted**: 497 changed lines in ONE PR (see Slice 4 PR Boundary). No further budget action needed; recorded for the archive ledger.
- `schoolYearLabel` is now dead UI state on home (Deviation 2) — a future contract cleanup could drop it from `HomeDashboardUiState`, but that is a behavior-adjacent change outside this visual slice.

## Files Changed (Slice 5)

| File | Action | Lines (+/-) | What |
|------|--------|-------------|------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/activities/LessonMapNode.kt` | Rewritten | +52/-155 (199 → 96 lines) | Card composable → 56dp circular node: Completed teal (`secondary`) + white `ic_check`; Current/Unlocked coral (`primary`) + white `ic_play`; Locked `BrandLock` + white `ic_lock`, non-clickable; `clickable(enabled = Unlocked \|\| Current)` preserves the VM gating contract; `testTag("lessonMapNode-{index}")`; `internal const LessonNodeSizeDp` shared with screen geometry |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/activities/LessonMapScreen.kt` | Rewritten (map branch only) | +188/-91 (498 → 595) | Header: 38×38/12dp back box (`ic_arrow_left`, "Volver", → `onShowHome`) + title `headlineSmall` + "{N} Lecciones" `bodySmall` muted + coral "Ver teoría" Surface pill (`testTag("theoryPill")`, 12/700 white, `isTheoryAvailable` gating kept); `LessonMapProgress` (teal 8dp `MLinearProgressIndicator(color = secondary)` + derived "{P}% Completado" / "{C}/{N} Lecciones"); `LessonMapPath` (`BoxWithConstraints` + Canvas `testTag("lessonMapPath")`, 4dp round-cap `drawLine` segments, dash 8dp/6dp density-scaled, nodes via `absoluteOffset`); old header card, `ActiveExerciseCard`, `LessonMapConnector` out; exercise-player composables, loading/error branches, and `TheorySheet` wiring untouched |
| `composeApp/src/commonMain/composeResources/drawable/ic_check.xml` + `ic_play.xml` | New (2 files) | +26 | Slice 3 stroke style (#111827 / 1.8 / round caps+joins); `ic_lock` + `ic_arrow_left` reused from Slice 3 assets |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/activities/LessonMapRedesignRenderTest.kt` | New | +193 | 5 render tests (see Test Evidence) |

Authored slice total: **~705 changed lines** (240 insertions + 246 deletions across the 2 Kotlin files, +26 vector assets, +193 tests) vs the ~775 approved `size:exception` envelope → within envelope.

Theme files (`ui/theme/*`): untouched — consumed Slice 1 tokens only (`secondary`, `primary`, `BrandLock`, `BrandTrack` via the primitive, `pill` shape, typography slots) and Slice 4's `color` param on `MLinearProgressIndicator`. No new Gradle dependency.

## Test Evidence (Slice 5)

- **Focused test command**: `./gradlew :composeApp:jvmTest --console=plain` (same SDK workaround as Slices 1–4; `local.properties` restored, 0 occurrences verified).
- **Result**: `BUILD SUCCESSFUL in 4m 5s` — **27 suites, 128 tests, 0 failures, 0 errors, 0 skipped** (Slice 4 reported 27 suites / 123 tests; +1 suite `LessonMapRedesignRenderTest`, +5 tests — test totals reconcile exactly: 123 + 5 = 128; suite count by the XML-counting method implies Slice 4's actual was 26).
- `LessonMapRedesignRenderTest`: 5/5 green —
  - `header renders title lesson count theory pill and back arrow` ("Fundamentos" + "4 Lecciones"; "Ver teoría" click → `onOpenTheory` fired; "Volver" click → `onShowHome` fired — rulings 2 & 4)
  - `progress bar derives percent and counts from node states` (3/8 → "37% Completado" + "3/8 Lecciones" + `ProgressBarRangeInfo(3f/8f, 0f..1f, 0)` — ruling 1)
  - `node states render and locked and completed nodes are non interactive` (Completed/Current/Locked tags exist; locked click → no selection; completed click → no selection — ruling 3)
  - `tapping an unlocked node selects its exercise` (node 3 click → `onExerciseSelected("ex-3")`)
  - `canvas path renders with height derived from node count` (`lessonMapPath` exists; height = 2 × 120dp)
- `LessonMapViewModelTest`: 7/7 untouched and green — no ViewModel contract change.
- One RED→GREEN cycle this batch: first run failed compiling `LessonMapScreen.kt` — `absoluteOffset` imported from `androidx.compose.ui.layout`; correct package is `androidx.compose.foundation.layout`. Fixed import only. Verbatim: `e: .../LessonMapScreen.kt:38:35 Unresolved reference 'absoluteOffset'`.
- **Runtime harness**: compose render tests exercise the real `LessonMapContent` + `AppTheme` (Sora) + composeResources vectors (`ic_check`/`ic_play` new; `ic_lock`/`ic_arrow_left` reused) on JVM; pill, back, and node tap callbacks all executed.
- Not semantics-observable, deferred to manual check: teal/coral/`BrandLock` node + segment colors, dash pattern and 4dp round caps, serpentine 72dp x positions, icon glyph fidelity vs handoff.
- **Rollback boundary**: revert touches only `ui/activities/{LessonMapNode,LessonMapScreen}.kt`, `composeResources/drawable/{ic_check,ic_play}.xml`, `jvmTest/.../activities/LessonMapRedesignRenderTest.kt` — no theme token, navigation, ViewModel, or contract changes; the exercise-player composables inside `LessonMapScreen.kt` are unchanged from Slice 4.

## Suggested PR / Commit Boundary (Slice 5)

- **Branch**: `feature/lesson-map-rewrite` → targets `feature/home-redesign` (stacked-to-main, per design Migration order).
- **Boundary**: starts at Slice 4 branch tip, ends with this slice — lesson map header + theory pill + progress block + Canvas serpentine path + circular node rewrite + 2 vector assets + render tests. No exercise-player restyle (Slice 6), no navigation, ViewModel, or contract changes.
- **Suggested single work-unit commit**: `feat(ui): rewrite lesson map as serpentine path per redesign handoff` — header/theory pill + progress + Canvas path + node rewrite + icons + render tests (tests with the behavior they verify).
- **400-line budget**: actual ~705 vs ~350 forecast → **budget exceeded; maintainer approved `size:exception` for ONE PR** (~775 approved envelope, Budget Gate option (a); the pre-approved 5a/5b split was declined). ~35% of the diff is wholesale-replacement deletions (246 lines shed by the two target files) plus self-contained vector assets and verification.
- **Rollback boundary**: per-file revert of the Files Changed (Slice 5) list; Slices 1–4 remain valid without this slice.

## Deviations from Design (Slice 5)

1. **Ruling 1 — progress percent derived from state**: `completed*100/total` (3/8 → "37% Completado", bar `3f/8f`); the PNG's "45%" is mock math. Spec scenario "teal bar at 45%" flagged as spec deviation; locked by test.
2. **Ruling 2 — subtitle is "{N} Lecciones" only**: `Lesson` has no `unit` field, so "Unidad 2 ·" would require a contract change (out of visual scope). The "Unidad" concept is flagged for the future learning-paths slice.
3. **Ruling 3 — node tap gating preserved**: nodes are clickable only when Unlocked/Current, mirroring `selectExercise`'s gate (`LessonMapViewModel.kt:64`). The spec scenario "tapping completed node opens exercise" is NOT implemented — flagged for a future contract change; locked by test.
4. **Ruling 4 — back arrow preserves the existing `onShowHome` callback** (spec says "course catalog"): navigation contract unchanged; spec scenario flagged.
5. **Ruling 5 — dash rule per PNG**: segment i→i+1 is dashed `BrandLock` when `nodes[i+1].state == Locked`; solid teal (`secondary`) when `nodes[i]` is Completed; solid coral (`primary`) otherwise. Overrides design.md's "if node i is Locked" wording.
6. **Serpentine parity read on the node's 1-based `index`** so the FIRST node sits on the RIGHT as in the authoritative PNG — design.md's literal 0-based reading ("even-index → left", `y = i*120+60`) would start the path on the left and fail the pixel check. Design's "even → left, odd → right" wording holds against the 1-based index; matches PNG nodes 1–4 (the PNG's 5th node sits left, likely mock imprecision). Flag for the manual pixel check.
7. **Unlocked renders identically to Current** (coral + play icon) — the PNG defines no separate unlocked look; the next actionable node is the coral one. Both states were already tappable, so behavior is unchanged.
8. **Fixed header + scrollable path** (header/progress live outside the `verticalScroll`) per the PNG; the old implementation scrolled the entire column. Interaction contracts unaffected.
9. **"Ver teoría" is a compact Surface pill, not `MButton`** — MButton's 56dp min-height + 12dp coral shadow contradict the handoff's compact pill; the Slice 3/4 chip pattern (12/700 white on coral, `pill` radius) is reused. `isTheoryAvailable` gating preserved (alpha 0.5 + disabled when unavailable).
10. **Map branch copy switched to Spanish** per the PNG ("Ver teoría", "% Completado", "Lecciones", "Volver"), consistent with Slices 2–4 Spanish redesign copy. Loading/error branches and the exercise player keep their pre-existing English copy — outside this slice's scope (player restyle is Slice 6).
11. **`ActiveExerciseCard` removed** per the approved Budget Gate plan — the PNG has no current-exercise card; the active exercise is already the coral node. `uiState.activeNode` remains consumed by the player header ("Exercise N").

## Issues Found (Slice 5)

- iOS/Android compilation not verified locally (Linux env runs `jvmTest` only). New code is `commonMain`-only Compose + composeResources vectors (same delivery mechanism as Slice 3's 17 icons). Recommend CI or a local `:composeApp:assembleDebug` + iOS compile before PR 5 review closes.
- `BoxWithConstraints` subcomposition + Canvas redraw per scroll frame is fine for realistic lesson sizes (< 20 exercises); a path with hundreds of nodes would want virtualization — not a real scenario today.
- Spec scenarios deliberately NOT implemented per maintainer rulings (all flagged for archive-time spec amendments): "tapping completed node opens exercise" (ruling 3), "navigates to course catalog" (ruling 4), "Unidad 2 ·" subtitle (ruling 2), "45%" mock percent (ruling 1).

## Risks / Pending (Slice 5)

- **Manual visual verification pending** (task 5.4 second half): pixel comparison vs `mapa-leccion.png` — serpentine x positions + first-node side (Deviation 6), dash density, node colors, pill shape. Required before PR 5 review closes.
- **`size:exception` accepted**: ~705 changed lines in ONE PR (approved envelope ~775). No further budget action needed; recorded for the archive ledger.
- Parity call (Deviation 6) is the one geometry judgment not covered by an explicit ruling — if the maintainer prefers design.md's literal 0-based reading, flipping is a one-line change (`node.index % 2` → position parity); the Canvas and node placement share the same helper, so they cannot drift apart.

## Test Evidence

- **Focused test command**: `./gradlew :composeApp:jvmTest --console=plain`
- **Result**: `BUILD SUCCESSFUL in 2m 57s` — **23 suites, 107 tests, 0 failures, 0 errors, 0 skipped** (was 106; +1 new `semanticFoundationColorsMatchRedesign`).
- `AppThemeTokensTest`: 4/4 green — `lightColorSchemeMatchesBrandedFoundationPalette`, `semanticFoundationColorsMatchRedesign`, `shapeTokensExposeReviewableFoundationValues`, `typographyMatchesSoraScaleWithInjectedFamily`.
- Existing compose-rule render tests (`OnboardingScreenTest`, `ProfileScreenTest`, etc.) exercise `AppTheme` → Sora `Font()` resource loading path on JVM — all green, proving the font wiring resolves at runtime.
- **Runtime harness**: `N/A` for Slice 1 in-app navigation — visual-only tokens; runtime boundary exercised indirectly via the 107-test jvmTest suite (compose render tests). Manual device check pending (see Risks).
- **SDK workaround**: `sdk.dir` patched to `/mnt/c/Users/Nahuel/AppData/Local/Android/Sdk` before the run, restored after (verified 0 occurrences remain).

## Test Evidence (Slice 2)

- **Focused test command**: `./gradlew :composeApp:jvmTest --console=plain` (same SDK workaround as Slice 1; `local.properties` restored, 0 occurrences verified).
- **Result**: `BUILD SUCCESSFUL in 1m 6s` — **24 suites, 110 tests, 0 failures, 0 errors, 0 skipped** (was 23 suites / 107 tests; +1 suite `AuthRedesignRenderTest`, +3 tests).
- `AuthRedesignRenderTest`: 3/3 green —
  - `login renders redesign copy and footer link navigates to register` (handoff copy present; "Registrate" click → `onSwitchToRegister` fired — behavior preserved)
  - `register step label uses handoff copy and follows the wizard step` ("Paso 1 / 3" → drive VM → "Paso 2 / 3")
  - `terms checkbox is 22 by 22 dp and toggles acceptance from the row` (`assertWidthIsEqualTo(22.dp)`/`assertHeightIsEqualTo(22.dp)` on the tagged box; `Role.Checkbox` node `assertIsOff` → `performClick` → `assertIsOn` + `acceptedTerms == true`)
- One RED→GREEN cycle during the batch: first run failed on `onNodeWithTag("termsCheckboxBox")` — the `toggleable` row merges descendants, so the tag only exists in the unmerged tree. Fixed with `useUnmergedTree = true` (test-side fix; production code unchanged). Verbatim failure: `Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'termsCheckboxBox'). However, the unmerged tree contains '1' node that matches.`
- Existing suites untouched and still green: `LoginViewModelTest`, `RegisterViewModelTest`, `AuthGateViewModelTest`, `AppThemeTokensTest`, `OnboardingScreenTest`, `ProfileScreenTest` — no behavior assertion was weakened or edited.
- **Runtime harness**: compose render tests exercise the real `LoginScreen`/`RegisterScreen` + ViewModel + `AppTheme` (Sora loading) path on JVM. Manual pixel comparison vs Jul 16 `.dc.html` pending (see Risks).
- Not semantics-observable, deferred to manual check: coral shadow blur on CTA, teal segment color, Sora/mono glyph rendering, 14dp social radius pixels.
- **Rollback boundary**: revert touches only `ui/LoginScreen.kt`, `ui/RegisterScreen.kt`, `ui/AuthScreenScaffold.kt`, `ui/primitives/MButton.kt` (Social branch), `src/jvmTest/.../AuthRedesignRenderTest.kt` — no theme token, navigation, ViewModel, or contract changes.

## Suggested PR / Commit Boundary

- **Branch**: `foundation/ui-redesign-tokens` → targets `main` (first in the stacked chain; slices 2–6 branch off it per design Migration section).
- **Boundary**: starts at `main` tip, ends with this slice — tokens, primitives, font resources, updated token tests. No feature-screen restyle, no dark mode.
- **Suggested single work-unit commit**: `feat(ui): add Sora typography and semantic foundation tokens for redesign` — includes resources + tokens + primitives + tests (tests stay with the behavior they verify).
- **Rollback boundary**: revert touches only `ui/theme/*`, `ui/primitives/{MButton,MProgressIndicator}.kt`, `ui/theme/AppThemeTokensTest.kt`, `composeResources/{font,files}/` — no screen, navigation, contract, or persistence changes.

## Suggested PR / Commit Boundary (Slice 2)

- **Branch**: `feature/auth-redesign` → targets `foundation/ui-redesign-tokens` (stacked-to-main, per design Migration order).
- **Boundary**: starts at Slice 1 branch tip, ends with this slice — auth screens + Social button variant + render tests. No profile/home/map/exercise screens, no dark mode, no ViewModel or navigation changes.
- **Suggested single work-unit commit**: `feat(ui): sync auth screens to Jul 16 redesign handoff` — Login/Register/scaffold visuals + `MButton.Social` radius + `AuthRedesignRenderTest` (tests with the behavior they verify).
- **Rollback boundary**: per-file revert of the 5 files listed in Files Changed (Slice 2); Slice 1 tokens remain valid without this slice.
- **Drift identified vs Jul 12 implementation** (for reviewer focus): social radius 16→14dp, divider 12/500→12/600, footer 13sp+underline→14sp no-underline, forgot link 12→13sp/600, step segments 6dp/3dp→5dp/999dp, step label copy+mono, strength coral→teal, M3 checkbox→22×22/7dp coral, logo card 18→16dp, wordmark flat-17→two-tone 22/800.

## Deviations from Design

1. **`AppTypography` became a `@Composable` getter** — composeResources `Font()` is composable, so a top-level non-composable `val` is impossible. Added pure `buildAppTypography(fontFamily)` to keep the scale unit-testable; `AppTheme.kt` usage unchanged in shape. Only callers were `AppTheme.kt` and the token test — both updated.
2. **Line heights not specified in design** — picked ~1.25–1.45× per slot (headLg 32/40, headMd 27/34, headSm 21/27, titleLg 17/23, titleMd 14/20, bodyLg 15/22, bodyMd 13/18, bodySm 12/16, labelMd 12/16). Flag for design review.
3. **Unspecified Typography slots** (display*, titleSmall, labelLarge, labelSmall) keep prior sizes/weights, family switched to Sora — design only specified 9 slots.
4. **`MCard.kt` edited 0 lines** (design predicted +1) — 18dp arrives transitively via `shapes.large`; behavior identical, no magic numbers introduced.
5. **Font fallback**: design pseudo-code `FontFamily(listOf(..., FontFamily.SansSerif))` is not valid API (a `FontFamily` is not a `Font`). Bundled resources cannot go missing at runtime, so the "Nunito fallback" scenario is satisfied by packaging; unmapped weights (e.g. Medium 500, no 500 ttf bundled) resolve to the nearest bundled Sora weight via Compose font resolution — no crash path.
6. **Naming**: `BrandTrack`/`BrandLock`/`BrandStripe`/`BrandCoralShadow` follow the file's existing `Brand*` convention (design text said `Track`/`Lock`/`Stripe`).
7. **Pre-existing unrelated working-tree changes NOT touched**: `docs/ui/screens/*.png`, `openspec/backlog.md`, deleted `scripts/configure-android-wsl-portproxy.ps1` — left exactly as found.

## Deviations from Design (Slice 2)

1. **Form title kept at 27/800, NOT 32/800** — design.md ("formTitle 32/800 Sora") and tasks.md 2.3 ("title 32/800") predate the **Jul 16 handoff**, which specifies 27px/800 for "Hola de nuevo" and "Creá tu cuenta" (README: "Título de pantalla: 27px / 800… login y 'Creá tu cuenta'"). Current code already renders 27/800 via `headlineMedium` + `ExtraBold`. Kept the Jul 16 value; **flag for design review** — if 32/800 was intentional, that's a regression against the handoff and needs a design amendment.
2. **Logo mark card radius 16dp, NOT 18dp** — tasks.md 2.3 says "logo 18dp", but the spec scenario ("52×52px, 16px radius"), design.md ("Brand logo box 16dp radius"), and the Jul 16 handoff (`border-radius:16px`) all say 16. Delivered 16dp via the existing `button` shape token (no new token — theme files frozen per slice boundary). tasks.md shorthand treated as stale.
3. **JetBrains Mono delivered as `FontFamily.Monospace`** — the font is not bundled (only Sora is), and adding a `.ttf` + accessor would cross the "do not edit theme files" slice boundary (new composeResources asset + theme-area helper). `FontFamily.Monospace` preserves the mono aesthetic at 12/600. **Follow-up candidate**: bundle `jetbrains_mono` (OFL) in a foundation PR if pixel-perfect matters.
4. **Wordmark two-tone 22/800 implemented** — beyond the spec scenario minimum (spec's brand-hero parenthetical only mandates logo/52×52/16px), but it is explicit in the Jul 16 handoff brand block (22px/800, "Mathim" ink + "App" muted/500, −0.02em). Delivered as a single `AnnotatedString` node so any `onNodeWithText("MathimApp")` lookup keeps working.
5. **Checkbox is a custom composable, not M3 `Checkbox`** — spec scenario mandates 22×22px/7px/coral, which M3 Checkbox cannot express. Toggle behavior consolidated into a single `Modifier.toggleable(role = Checkbox)` on the row (previously two handlers: row `clickable` + Checkbox `onCheckedChange`; net behavior identical — one toggle per tap, same `onAcceptedTermsChange` contract).
6. **`MButton.Social` border 1→1.5dp** — handoff shows `1.5px solid line` on social buttons; radius (14dp) is the spec-mandated part, border width is handoff fidelity. Filled/Outline variants untouched.
7. **Password-strength label kept muted, not teal** — spec scenario mandates teal for the meter *fill* only ("meter fills with teal"). Label copy/logic preserved exactly (behavior); the handoff's teal "Buena" label is per-state styling not required by the spec.
8. **Not implemented (out of spec scope, noted for transparency)**: register header back-arrow + one-question-per-screen restructure (spec says "Data fields unchanged"; step→field mapping preserved), "Continuar" arrow glyph on CTA (not in spec scenarios), footer bottom-anchoring (structural layout, not mandated).

## Issues Found

- Prior apply batch left the font assets but no code/test/progress changes — recovered in this batch (this is the corrective run).
- `MLinearProgressIndicator` had zero callers before this change; it is the building block for slices 2–6 (header bars).
- Slice 2: design.md/tasks.md carry **stale pre-Jul-16 values** (title 32/800, logo 18dp) that conflict with the Jul 16 handoff and the spec delta — resolved in favor of handoff+spec (see Slice 2 Deviations 1–2). Recommend amending design.md at archive time.
- Slice 2: register "Back" outline button uses English copy ("Back") on a Spanish screen — pre-existing, out of scope, noted for a future copy pass.

## Risks / Pending

- **Manual visual verification pending** (task 1.8 second half): Sora rendering + CTA coral shadow on device/emulator — cannot be done headless. Required before PR 1 review closes.
- **Manual visual verification pending** (task 2.3 second half): pixel comparison vs Jul 16 `.dc.html` — shadow blur, teal strength fill, mono step label, 22×22 checkbox, two-tone wordmark. Required before PR 2 review closes.
- Weight 500 (Medium) has no dedicated Sora ttf — resolves to nearest weight; visually indistinguishable at 11–12sp but flag if pixel-perfect matters.
- Line-height picks (deviation 2) are the only unconfirmed token values.
- Slice 2: `FontFamily.Monospace` renders a system mono, not JetBrains Mono glyphs — visible in side-by-side pixel comparison if inspected closely (see Slice 2 Deviation 3).
- Slice 2: button text remains `labelLarge` (14/600) while the handoff shows 16/700 button text — that is a theme-level slot change (Slice 1 territory, affects every button) and was intentionally not done in a feature slice. Flag for design review.

## Superseded Snapshot: Slice 5 Budget Gate Before Implementation

**Current resolution**: maintainer approved `size:exception` for ONE PR 5 (~775-line envelope; actual ~705), and Slice 5 implementation plus automated tests are now complete. The pre-approved 5a/5b split (option b) was declined. The 5 spec/design conflicts below were ruled on by the maintainer and are recorded as binding in Slice 5 Deviations 1–5. The remaining Slice 5 requirement is the manual `mapa-leccion.png` visual check.

> **Historical snapshot, superseded by the current status above:** At the budget-gate checkpoint, the apply batch had stopped before writing code because the estimate exceeded 400 lines. At that time no Slice 5 files had been created or modified and tasks 5.1–5.4 were unchecked. This statement is retained only as decision and budget evidence; it does not describe the recovered worktree's current state.

### Disciplined estimate (additions + deletions, per work-unit-commits counting rule)

| Work item | + | − | Diff lines |
|---|---|---|---|
| `ui/activities/LessonMapNode.kt` — wholesale rewrite (199-line card composable → ~100-line circular node) | ~100 | ~180 | ~280 |
| `ui/activities/LessonMapScreen.kt` — map branch rewrite (header/progress/path composables in; old header card, `ActiveExerciseCard`, `LessonMapConnector` out; exercise-player code untouched) | ~198 | ~131 | ~329 |
| `composeResources/drawable/ic_check.xml` + `ic_play.xml` (new vectors, Slice 3 stroke style) | ~26 | 0 | ~26 |
| `jvmTest/.../activities/LessonMapRedesignRenderTest.kt` (5 tests: header/theory/back, progress bar, node states + locked gating, tap wiring, canvas) | ~140 | 0 | ~140 |
| **Total** | | | **~775** |

~775 vs 400 budget (~2×) and vs ~350 forecast (~2.2×). Deletions (~310) are irreducible: the slice is the approved structural rewrite and both target files must shed the card-list implementation. No honest trim (fewer tests, partial node rewrite, dead code) brings ONE PR under 400.

### Options for the maintainer (pick one)

- **(a) `size:exception` — ONE PR 5 (~775 lines).** Same resolution as Slice 3 (~797) and Slice 4 (497); ~40% of the diff is wholesale-replacement deletions + self-contained vector assets. Branch `feature/lesson-map-rewrite` → `feature/home-redesign`.
- **(b) Pre-approved 2-way split.** 5a geometry/layout (~255–300): screen-only — scrollable Box + Canvas polyline + serpentine offsets (120dp step) + tap wiring with a temporary minimal private node circle (old `LessonMapNode.kt` untouched); tasks 5.3, 5.4 + placement half of 5.2. 5b node polish/states (~480–520, targets 5a branch): full node rewrite (56dp, state icons, locked non-clickable) + `ic_check`/`ic_play` + header/progress/theory pill + state tests; task 5.1 + 5.2 remainder. **Warning**: 5b still exceeds 400 (node-file deletions + assets + honest tests cannot subdivide further).
- **(c) 3-way split** (geometry ~260 / header+progress ~210 / node+icons+tests ~420): all PRs ≤ ~420, higher process cost.

### Analysis completed (implementation resumes immediately on decision)

- PNG delta (`mapa-leccion.png` vs `old/`): structure unchanged (serpentine path, back header, theory pill, progress block, dashed locked segments); palette/typography migrate to light theme (teal completed, coral current, `BrandLock` gray locked, Sora).
- Geometry per design: 120dp vertical step, node centers y = i*120+60dp, even-index x = 72dp from left / odd-index 72dp from right; Canvas polyline 4dp round-cap; dash effect `dashPathEffect` density-scaled from design's 8f/6f (design chose Canvas for resolution independence — raw px contradicts it).
- Tokens consumed (no theme edits): `secondary` (teal), `primary` (coral), `BrandLock`, `BrandTrack`, shapes `pill`/`button`; `MLinearProgressIndicator(color = secondary)`; vectors `ic_arrow_left`/`ic_lock` reused.
- Bottom-nav spec scenario needs no screen work — host `AuthenticatedHomeScaffold` already renders 4 tabs with Actividades selected.

### Spec/design conflicts to rule on WITH the workload call

1. **"45% Completado" at 3/8 is mock math** (3/8 = 37.5%) — derive percent from state (`completed*100/total`); same precedent as Slice 4 Deviation 1.
2. **"Unidad 2 · 8 Lecciones"** — `Lesson` model has NO unit field; subtitle can only be `"{N} Lecciones"` without a contract change (out of visual scope).
3. **Spec scenario "tapping completed node opens exercise" conflicts with the VM contract** — `selectExercise` gates to Unlocked/Current (`LessonMapViewModel.kt:64`); the slice mandate ("preserve ALL behavior… visual/structural, not functional") keeps gating as-is; flag scenario for a future contract change.
4. **Back arrow → "course catalog" per spec vs existing `onShowHome` callback** — preserve `onShowHome` (navigation contract unchanged); flag.
5. **Dashed-segment rule** — PNG (authoritative) dashes the segment INTO a locked node (current→locked dashed in both PNGs); design.md says "if node i is Locked". PNG wins: dashed when `nodes[i+1].state == Locked`; solid teal when `nodes[i]` Completed, solid coral otherwise.

## Remaining Tasks

- [x] Slice 1 implementation and automated tests (PR 1): foundation/tokens
- [ ] Slice 1 manual visual check: Sora rendering and CTA shadow
- [x] Slice 2 implementation and automated tests (PR 2): auth screens
- [ ] Slice 2 manual visual check: Jul 16 screenshot
- [x] Slice 3 implementation and automated tests (PR 3): profile
- [ ] Slice 3 manual visual check: blocked by the documented PNG/spec conflict
- [x] Slice 4 implementation and automated tests (PR 4): home
- [ ] Slice 4 manual visual check: `inicio-dashboard.png`
- [x] Slice 5 implementation and automated tests (PR 5): lesson map only
- [ ] Slice 5 manual visual check: `mapa-leccion.png`
- [ ] Slice 6 implementation and verification (PR 6): exercise player + `TheorySheet` + onboarding + empty/loading states — not started
