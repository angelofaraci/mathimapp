# Design: Fix Post-Onboarding Course Navigation Crash

## Technical Approach

Single-line DI wiring fix plus a regression test. `appModule` registers the router
ViewModel with Koin's reflective constructor DSL:

```kotlin
viewModelOf(::MainRouterViewModel)   // AppModule.kt:63
```

`viewModelOf` builds the instance through `constructorOf`, which resolves **every**
constructor parameter from the container and has no access to Kotlin default values
(defaults live in the synthetic `$default` bridge, not in the constructor signature the
DSL reflects over). `MainRouterViewModel(initialTab: MainTab = MainTab.HOME)` therefore
asks the container for a `MainTab`, which nothing binds, and the first composition of
`AuthenticatedHomeScaffold` throws `NoDefinitionFoundException`.

Fix: register with an explicit lambda so the reflective path is never taken and the
Kotlin default applies normally.

```kotlin
viewModel { MainRouterViewModel() }   // import org.koin.core.module.dsl.viewModel
```

No production behavior changes: the resulting instance is identical to the one the
existing `MainRouterViewModelTest` already constructs directly. `MainRouterViewModel`
and `MainTab` are untouched.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| Registration style | `viewModel { MainRouterViewModel() }` lambda | `single<MainTab> { MainTab.HOME }` in the container | The initial tab is a local default, not a collaborator. Binding a UI enum globally is semantically wrong and would let any consumer inject it. |
| Registration style | same | Drop the `initialTab` parameter from the constructor | Moves the problem rather than fixing it — the reflective DSL stays in place and would break again on the next defaulted parameter, and it changes a public API the proposal puts out of scope. |
| Scope of the DSL change | Only line 63 | Convert all nine `viewModelOf` registrations to lambdas | The other eight ViewModels take only container-bound repository dependencies; `viewModelOf` is correct and more concise for them. Follow the existing pattern where it is safe. |
| Regression test placement | `commonTest`, isolated `koinApplication` | `koin-test` `checkModules` | `checkModules` needs a new test dependency and a stub for the platform-only `DatabaseDriverFactory` that `single { createAppDatabase(get()) }` requires. The narrow test needs neither and covers the confirmed defect. |

## Data Flow

    AuthGate ──→ AuthenticatedHomeScaffold ──→ koinViewModel<MainRouterViewModel>()
                                                        │
                                                        ▼
                                          appModule definition (Factory)
                                                        │
                          before: constructorOf ──→ get<MainTab>() ──→ THROWS
                          after:  lambda ──→ MainRouterViewModel() ──→ HOME

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modify | Line 63 → `viewModel { MainRouterViewModel() }`; add `import org.koin.core.module.dsl.viewModel` (keep `viewModelOf` import — still used by the other eight). |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/di/AppModuleTest.kt` | Create | Koin resolution regression test (below). |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (existing) | `MainRouterViewModel` defaults to `MainTab.HOME` | `MainRouterTest.kt` — unchanged, must keep passing |
| DI regression (new) | `MainRouterViewModel` resolves from `appModule` without throwing, and starts on `MainTab.HOME` | Isolated `koinApplication { modules(appModule) }`, then `koin.get<MainRouterViewModel>()` |
| Manual | Onboarding "Continue to courses" and cold start reach the bottom-nav shell with Inicio selected | Device run |

The new test MUST:

1. Build an **isolated** container via `koinApplication { modules(appModule) }` — never
   `startKoin`, which collides with the guarded global context in `KoinInitializer.kt`.
2. Assert `koin.get<MainRouterViewModel>()` completes without exception. This is the RED
   assertion: before the fix it fails with `NoDefinitionFoundException` for `MainTab`.
3. Assert the resolved instance's `target.value == MainTab.HOME`, proving the Kotlin
   default was applied rather than a container-supplied value.
4. Close the container (`koin.close()`) so the test leaves no global state.

No stub for `DatabaseDriverFactory` is needed: Koin `single` definitions are lazy, and
resolving the router touches neither the database nor the network module.

## Threat Matrix

N/A — no routing shell-out, subprocess, VCS/PR automation, executable-file
classification, or process-integration boundary. (In-app tab routing is not a
process-integration boundary.)

## Migration / Rollout

No migration required. Single commit, revertible; no schema, contract, or persisted-state
change.

## Open Questions

None.
