# Design: Auth Registration Flow Stability

## Technical Approach

Keep one Koin application alive outside the Compose tree, then make auth and home navigation state Koin ViewModels. This implements the proposal and stabilizes `frontend-auth` session routing and `onboarding-flow` actions across Android configuration changes. No API, shared-contract, or persistence change is required.

## Architecture Decisions

| Decision | Options / tradeoff | Choice and rationale |
|---|---|---|
| Koin lifetime | `KoinApplication` in Compose recreates singleton graph; platform startup needs platform DB wiring | Start once at each platform entry point and guard with `KoinPlatform.getKoinOrNull()`. A common initializer receives a platform `Module`, starts `appModule` plus that module only when absent. This prevents a configuration-change composition from stopping or replacing repositories used by surviving ViewModels. |
| Router ownership | `remember` is composition-scoped; `rememberSaveable` only solves target serialization | Replace `AuthGateRouter` and `MainRouter` with `ViewModel` state holders registered by `viewModelOf`. Their `StateFlow` APIs and routing helpers remain unchanged, while the platform `ViewModelStore` retains their state through Android recreation. |
| Onboarding layout | Unbounded list displaces footer; scrolling the entire page hides actions | Keep header/error/footer in the outer `Column`; give the current step a bounded `weight(1f)` region. Each list step uses a root `Column` and a weighted `LazyColumn`, so its title stays above a scrollable list and Continue/Back remain outside it. |

## Data Flow

```text
Android MainActivity / iOS MainViewController
  -> initKoin(platformModule + appModule, once)
  -> App -> AuthGateViewModel + AuthRepository (same Koin graph)
  -> session/target -> Login | Register | Onboarding | Home

AuthenticatedHomeScaffold -> MainRouterViewModel -> selected tab
Onboarding bounded step list -> selection -> visible footer action -> ViewModel
```

## File Changes

| File | Action | Description |
|---|---|---|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/KoinInitializer.kt` | Create | Start Koin once with a caller-supplied platform module and `appModule`. |
| `composeApp/src/androidMain/kotlin/com/example/proyectofinal/MainActivity.kt` | Modify | Build the Android `DatabaseDriverFactory(applicationContext)` module and initialize Koin before `setContent`. |
| `composeApp/src/iosMain/kotlin/com/example/proyectofinal/MainViewController.kt` | Modify | Build the native driver module and initialize Koin before `ComposeUIViewController`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt` | Modify | Remove `KoinApplication` and composable platform-module setup; obtain `AuthGateViewModel` with `koinViewModel`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthGateRouter.kt` | Modify | Rename/convert router to `AuthGateViewModel : ViewModel`; preserve target and switching methods. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/MainRouter.kt` | Modify | Rename/convert router to `MainRouterViewModel : ViewModel`; preserve tab helpers. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthenticatedHomeScaffold.kt` | Modify | Inject `MainRouterViewModel` rather than create a remembered router. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardScreen.kt` | Modify | Accept `MainRouterViewModel` so dashboard navigation keeps the retained router type. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modify | Register both router ViewModels with `viewModelOf`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/OnboardingScreen.kt` | Modify | Bound list-step height and expose the content function internally for JVM UI testing. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/AuthGateRoutingTest.kt` | Modify | Update to `AuthGateViewModel`; assert form target remains after view-model reuse. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/MainRouterTest.kt` | Modify | Update to `MainRouterViewModel`; assert selected tab remains. |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/OnboardingScreenTest.kt` | Create | Render a province list and assert the Continue footer is displayed and callable. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/PlatformModule.kt` | Delete | Remove the Compose-only platform-module expectation superseded by entry-point modules. |
| `composeApp/src/androidMain/kotlin/com/example/proyectofinal/di/PlatformModule.android.kt` | Delete | Remove the Android Compose-only module factory. |
| `composeApp/src/iosMain/kotlin/com/example/proyectofinal/di/PlatformModule.ios.kt` | Delete | Remove the iOS Compose-only module factory. |
| `composeApp/src/jvmMain/kotlin/com/example/proyectofinal/di/PlatformModule.jvm.kt` | Delete | Remove the JVM Compose-only module factory. |

## Interfaces / Contracts

```kotlin
fun initializeKoin(platformModule: Module) {
    if (KoinPlatform.getKoinOrNull() == null) startKoin {
        modules(platformModule, appModule)
    }
}

class AuthGateViewModel : ViewModel() {
    val target: StateFlow<AuthScreenTarget>
    fun switchToLogin()
    fun switchToRegister()
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Auth form target and selected main tab | Existing common tests instantiate the renamed ViewModels and verify state transitions/retention. |
| JVM Compose | Province list does not hide Continue | Render `OnboardingContent` at a constrained viewport; assert Continue is displayed after selection and invokes its callback. |
| Manual platform smoke | Shared Koin graph and state across recreation | Android: rotate during login/register and a non-home tab. iOS: recreate/view-resume the Compose controller, then login and complete onboarding. |

Run `./gradlew :composeApp:jvmTest :composeApp:assembleDebug`; also compile iOS framework if the local toolchain is available.

## Migration / Rollout

No migration required. In-memory tokens still intentionally do not survive process death.

## Open Questions

None. The documented Register back-to-login behavior is outside this change's stated scope and should be addressed separately.
