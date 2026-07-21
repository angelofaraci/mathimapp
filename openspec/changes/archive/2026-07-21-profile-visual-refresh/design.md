# Design: Profile Visual Refresh

## Technical Approach

Replace the gamified body of `ProfileScreen` with the light-theme profile hub from the handoff. Preserve the loading/error branches and scaffold-owned logout callback. A screen-local enum drives visual transitions to four stub sub-screens; a local `BackHandler` restores the hub from any sub-screen. The ViewModel only maps already-available identity data. This implements the proposal without changing `MainRouter`, contracts, persistence, themes, fonts, or backend behavior.

## Architecture Decisions

| Decision | Options / tradeoff | Choice and rationale |
|---|---|---|
| Nested navigation | MainRouter; local state | Keep `ProfileSubScreen` local to `ProfileScreen`. The router owns only bottom tabs, so this prevents a profile detail from leaking into global navigation. |
| System back | Allow host back; intercept only local sub-screens | Use Compose `BackHandler(enabled = destination != ProfileSubScreen.HUB)` to set `destination = HUB`. The hub preserves normal host behavior; every local sub-screen returns to the hub. |
| Identity data | New API/contract; existing session user | Add `email` and `role: UserRole` to `ProfileUiState`, populated from `AuthSession.user`. The data already exists and needs no backend or `shared` change. |
| Streak display | Reuse capped `streak`; omit the chip | Omit the streak chip. The current value counts completed lessons capped at seven, not consecutive days, so it is not valid identity data for a day streak. |
| Stubbed controls | Functional settings/actions; visual callbacks | Render static values and explicit `TODO` no-op callbacks for every non-logout action. This prevents accidental preference/account behavior. |
| Reuse | One large screen file; small primitives | Add three presentational primitives in `ui/primitives` for repeated handoff patterns while keeping screen composition local and reviewable. |

## Data Flow

```
AuthRepository.session.user ──> ProfileViewModel ──> ProfileUiState
UserRepository + learner profile ────────────────┘        │
                                                          v
ProfileScreen (remember ProfileSubScreen + BackHandler) ──> Hub / local stub
Logout button ──────────────────────────────> onLogout (unchanged)
```

`ProfileUiState` keeps every current gamification field for test/source compatibility, though the refreshed UI no longer renders them. It adds:

```kotlin
val email: String = ""
val role: UserRole = UserRole.STUDENT
```

`ProfileScreen` owns `var destination by remember { mutableStateOf(ProfileSubScreen.HUB) }`, where private `ProfileSubScreen` contains `HUB`, `ACCOUNT`, `PREFERENCES`, `HELP`, and `ABOUT`. `AnimatedContent(destination)` selects the body. Selecting a hub card sets its destination; each sub-screen back button restores `HUB`. `BackHandler(enabled = destination != ProfileSubScreen.HUB)` also sets `destination = HUB`, consuming Android system back only outside the hub. No state is saved or persisted.

## Compose Structure

- `ProfileContent`: retains the existing loading indicator and error-message branches before it hosts local destination state and `AnimatedContent`.
- `ProfileHub`: centered `ProfileIdentity`, four `ProfileNavigationCard`s, the real logout `MButton`, and static `"MathimApp · version X"` caption. Avatar edit is a TODO no-op. The avatar uses the display-name initials fallback (`"U"` when blank); no streak chip renders.
- `ProfileSubScreenScaffold`: back button and centered title shared by all local pages.
- `AccountScreen`: name/email/password `ProfileListRow`s, legal text, and delete button. Each click is TODO/no-op; no destructive dialog or account mutation.
- `PreferencesScreen`: static enabled notifications/sounds `ProfileToggleRow`s and a static Spanish `ProfileListRow`. Toggle and language callbacks are TODO/no-ops. **Do not render a dark-mode row.**
- `HelpScreen` and `AboutScreen`: hardcoded list rows matching the card pattern; each trailing action is TODO/no-op.

The implementation uses the existing light `MaterialTheme`, color tokens, `MCard`, and `MButton`; it does not alter `AppTheme`, tokens, typography, or Gradle dependencies. The fixed version caption is visual-only with a TODO for a future cross-platform source; no version resolution is added.

## File Changes

| File | Action | Description |
|---|---|---|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileScreen.kt` | Modify | Hub, local enum switcher, sub-screen composables, and explicit stub callbacks; retain logout. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileViewModel.kt` | Modify | Map session user email/role into state without removing metrics. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileNavigationCard.kt` | Create | Clickable icon/title/subtitle/chevron card. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileToggleRow.kt` | Create | Label plus stateless `Switch` row. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileListRow.kt` | Create | Label/value row with supplied trailing chevron/action. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/ProfileViewModelTest.kt` | Modify | Assert email and role in success and fallback states. |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/ProfileScreenTest.kt` | Create | Compose interaction coverage for local navigation and logout. |

## Interfaces / Contracts

New primitives are UI-only and accept callbacks supplied by `ProfileScreen`; they do not own state or repositories.

```kotlin
@Composable fun ProfileNavigationCard(title: String, subtitle: String, onClick: () -> Unit, ...)
@Composable fun ProfileToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit)
@Composable fun ProfileListRow(label: String, value: String, onClick: () -> Unit, ...)
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Email/role mapping and retained metric derivation | Extend `ProfileViewModelTest` fakes/assertions. |
| Compose JVM | Hub-to-sub-screen, in-app back, Android system-back-to-`HUB`, loading/error branches, initials fallback, and logout callback | Add semantic text/click assertions in `ProfileScreenTest`. |
| Integration | Regression suite | Run `./gradlew :composeApp:jvmTest`. |

## Migration / Rollout

No migration required. This is an in-place visual replacement with local ephemeral navigation.

## Open Questions

None. Future behavior (avatar editing, account operations, settings persistence, language selection, version source, dark mode, and Sora) requires separate scoped changes.
