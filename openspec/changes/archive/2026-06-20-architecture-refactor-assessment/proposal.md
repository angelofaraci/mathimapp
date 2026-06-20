# Proposal: Architecture Refactor Assessment

## Intent

Harden the young KMP codebase so it can scale new features without a full rewrite. Address missing layers (DI, service separation), remove noise (`SharedAliases`), and add tests. Keep changes incremental and reviewable for a university project context.

## Scope

### In Scope
- Koin DI in `composeApp` (replace globals, manual ViewModel wiring).
- Remove `SharedAliases.kt` indirection.
- Extract `server` service layer from route files.
- Add unit tests for services and ViewModels.

### Out of Scope
- Full Clean/Hexagonal rewrite.
- Offline-first sync or complex local caching.
- Module/schema changes; `shared` remains unchanged.
- iOS native wiring.

## Capabilities

### New Capabilities
None

### Modified Capabilities
None

## Approach

Adopt the pragmatic path from exploration: inject dependencies with Koin, delete `SharedAliases.kt`, and thin Ktor routes by moving business logic into `server/service/` classes. Deliver in three reviewable slices: (1) DI setup + `SharedAliases` removal; (2) server service layer; (3) tests for services and ViewModels.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/.../NetworkClient.kt` | Modified | Remove global mutable state; inject `HttpClient` and config |
| `composeApp/.../App.kt` | Modified | Replace manual ViewModel creation with Koin |
| `composeApp/.../domain/SharedAliases.kt` | Removed | Use `shared` models directly |
| `composeApp/.../data/Ktor*Repository.kt` | Modified | Inject API and DB dependencies |
| `composeApp/.../domain/*Api.kt` | Modified | Inject `HttpClient` and base URL |
| `server/.../routes/*.kt` | Modified | Thin to HTTP/auth only |
| `server/.../service/` | New | Service classes owning business logic |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Scope creep beyond 400-line budget | Med | Split into 3 autonomous PR slices |
| Regression without tests | Med | Add tests for the slice being touched |
| Koin setup friction | Low | Use standard KMP Koin patterns |

## Rollback Plan

Revert the specific PR slice. Each slice is autonomous and does not depend on subsequent slices.

## Dependencies

- Koin-BOM for Kotlin Multiplatform and Compose integration.

## Success Criteria

- [ ] Koin injects `HttpClient`, repositories, and ViewModels with no globals.
- [ ] `SharedAliases.kt` removed; project compiles.
- [ ] Server routes contain no Exposed queries; logic lives in services.
- [ ] Existing and new tests pass; no auth or contract regressions.

## Proposal Question Round

**Assumptions made:**
1. Slice order: DI + cleanup first, then server services, then tests.
2. No new product behavior changes; pure refactor.
3. Koin version managed via `gradle/libs.versions.toml`.

**Questions:**
1. Do you want Koin in `server` as well, or is manual instantiation there acceptable?
2. Should the server service layer be one PR or split per domain (auth, courses, etc.)?
3. Is a 60% line-coverage target for new service tests acceptable?
