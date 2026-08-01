# Proposal: Fix Post-Onboarding Course Navigation Crash

## Intent

The app hard-crashes on the first composition of `AuthenticatedHomeScaffold` — in practice when a learner finishes onboarding and taps "Continue to courses", and on every cold start once onboarding is complete. The authenticated area is therefore unreachable: onboarding is a dead end.

Root cause is confirmed from a device logcat (`InstanceCreationException` → `NoDefinitionFoundException: No definition found for type '...MainTab'`), not inferred. `AppModule.kt:63` registers `viewModelOf(::MainRouterViewModel)`; Koin's reflective `constructorOf` DSL resolves *every* constructor parameter through the container and ignores Kotlin default values, so `MainRouterViewModel(initialTab: MainTab = MainTab.HOME)` fails on the unbound `MainTab` enum. The crash is unconditional, not race- or data-dependent.

## Scope

### In Scope
- Replace `viewModelOf(::MainRouterViewModel)` with an explicit `viewModel { MainRouterViewModel() }` lambda registration in `di/AppModule.kt`.
- Add a regression test asserting `MainRouterViewModel` resolves from `appModule` (or a Koin module-verification test covering the graph).

### Out of Scope
- The other three onboarding issues tracked in `openspec/backlog.md` under "Onboarding and navigation bug fixes": Spanish localization, onboarding step order (province/year/track), logout button visible during onboarding. Each is a separate SDD change.
- Any change to `MainRouterViewModel`'s public API, `MainTab`, tab behavior, or the `AuthGate` transition flow.
- The latent `LearnerProfileEntity` single-row and `PRAGMA foreign_keys` findings from exploration (separate backlog items).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `profile-screen`: the existing "Bottom Navigation Shell" requirement gains a scenario asserting the shell's router ViewModel is constructible from DI without a container-resolved `MainTab`.

## Approach

Explicit lambda registration is preferred over binding `MainTab` in Koin: the initial tab is a local default, not a dependency, and binding an enum in the container would be semantically wrong. The lambda also stops the reflective DSL from ever inspecting the constructor. All other registered ViewModels take only repository dependencies, so no other registration is affected.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `composeApp/.../di/AppModule.kt` | Modified | Line 63 registration + `viewModel` import. |
| `composeApp/src/commonTest` or `jvmTest` | New | Koin resolution regression test. |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Fix compiles but crash persists for another reason | Low | Stack trace is conclusive; verify by launching to the authenticated area on device. |

## Rollback Plan

Revert the single commit. No schema, contract, or persisted-state change.

## Dependencies

None.

## Success Criteria

- [ ] Completing onboarding navigates to the authenticated area without crashing.
- [ ] Cold start with onboarding already complete reaches the bottom-nav shell with Inicio selected.
- [ ] Regression test resolving `MainRouterViewModel` from `appModule` passes.
- [ ] `./gradlew :composeApp:jvmTest` passes.
