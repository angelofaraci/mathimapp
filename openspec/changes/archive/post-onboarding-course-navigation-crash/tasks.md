# Tasks: Fix Post-Onboarding Course Navigation Crash

## Review Workload Forecast

Decision needed before apply: No
Chained PRs: No
Chain strategy: single-PR
400-line budget risk: Low

| PR | Est. Lines | Risk |
|----|------------|------|
| 1 fix/main-router-di | ~15 | Low |

## PR 1: fix/main-router-di (~15 lines)

- [x] 1.1 In `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt`, add `import org.koin.core.module.dsl.viewModel` (keep the existing `viewModelOf` import — still used by the other eight registrations) — satisfies profile-screen spec "Router ViewModel resolves from DI without a MainTab binding"
- [x] 1.2 Replace line 63 `viewModelOf(::MainRouterViewModel)` with `viewModel { MainRouterViewModel() }` — satisfies profile-screen spec "Entering the authenticated area does not crash after onboarding" and "Router ViewModel resolves from DI without a MainTab binding"
- [x] 1.3 Create `composeApp/src/commonTest/kotlin/com/example/proyectofinal/di/AppModuleTest.kt`: build an isolated `koinApplication { modules(appModule) }` (never `startKoin`), resolve `koin.get<MainRouterViewModel>()` and assert it does not throw, assert the resolved instance's `target.value == MainTab.HOME`, then `koin.close()` — satisfies profile-screen spec "Router ViewModel resolves from DI without a MainTab binding" (RED before fix: fails with `NoDefinitionFoundException` for `MainTab`; GREEN after fix)
- [x] 1.4 Run `MainRouterViewModelTest` (unchanged) and the new `AppModuleTest` to confirm both pass
- [x] 1.5 Manual on-device verification: completing onboarding no longer crashes — confirmed by user on device. (User also observed onboarding being skipped/auto-completed before they could interact with it; that is a separate pre-existing bug, not a regression from this fix — tracked as a new change, `onboarding-profile-scoped-to-user`.)
- [x] 1.6 Manual on-device verification: no crash on entering the authenticated area — confirmed by user on device (see note on 1.5).

Sequencing: 1.1 → 1.2 (same file, sequential) → 1.3 (independent new file, can be written in parallel with 1.1/1.2 but depends on the fix to go GREEN) → 1.4 (depends on 1.1–1.3) → 1.5/1.6 (depend on 1.4, can run in parallel with each other).

## Suggested Work Units

| Unit | PR | Test | Harness | Rollback |
|------|----|------|---------|----------|
| DI fix + regression test | 1 | `AppModuleTest`, `MainRouterViewModelTest` | Unit (isolated Koin container) | `di/AppModule.kt`, `di/AppModuleTest.kt` |
