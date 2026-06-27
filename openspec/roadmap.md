# OpenSpec Roadmap: Plataforma de Aprendizaje Matemática

> **Umbrella initiative**: `plataforma-aprendizaje-matematica`
> **Purpose**: Decompose the broad umbrella into ordered, reviewable feature slices so the team never attempts to implement the umbrella directly.

## Quick Path

1. ✅ `role-naming-cleanup` — completed.
2. ✅ `versioned-db-migrations` — completed and archived.
3. ✅ `lesson-read-access-control` — completed and archived.
4. Pick the next product track: learner-first (`onboarding-school-year`) or classroom-first (`teacher-course-ownership`).

## Completed Slices

| Slice | Type | What it delivered | Verification |
|-------|------|-------------------|--------------|
| `course-empty-state` | Engram-only UI | Empty-state rendering in `CourseList` | Manual + `:composeApp:jvmTest` |
| `theory-content-loading` | OpenSpec archived | Runtime theory loading, school-year filtering, role-scoped theory editing | 48 tests, 0 failures |
| `lesson-progress-tracking` | OpenSpec archived | Exercise completion as atomic progress, lesson derivation from exercises, client-server progress sync | 35+ tests, 0 failures |
| `configurable-api-base-url` | OpenSpec archived | Configurable base URL resolution for Android, iOS, and JVM targets | `:composeApp:jvmTest` + targeted platform checks |
| `role-naming-cleanup` | OpenSpec archived | Rename `LEARNER` → `STUDENT` in shared models, server, compose app, and specs with backward-compatible parser | 64 tests, 0 failures — `:server:test`, `:composeApp:jvmTest` |
| `versioned-db-migrations` | OpenSpec archived | Flyway-based versioned migrations with baseline + guarded school-year follow-up migration | 30 tests, 0 failures — `:server:test` |
| `lesson-read-access-control` | OpenSpec archived | Canonical lesson-read visibility and answer-masking reconciliation; theory read-access spec synced to existing backend behavior | 42 tests, 0 failures — `:server:test` |

## Next Slices (Ordered)

### Phase 1 — Learner Experience

#### 1. `onboarding-school-year`
- **Scope**: Collect learner type and school year during onboarding; recommend the initial curriculum path.
- **Rationale**: First learner-facing feature from the umbrella `learning` spec; unlocks the curriculum map.
- **Dependencies**: `school-year-filtering` (delivered).
- **Affected modules**: `composeApp`.
- **Expected verification**: `:composeApp:jvmTest`; manual onboarding flow validation.
- **Review-size risk**: **Medium** (~200–300 lines).

#### 2. `exercise-practice-ui`
- **Scope**: Build the interactive exercise screen where learners answer, submit, and receive immediate feedback; wire to `POST /exercises/{id}/complete`.
- **Rationale**: Completes the end-to-end exercise-completion flow that currently exists only at the API/repository layer.
- **Dependencies**: `exercise-completion` (delivered), `lesson-progress-tracking` (delivered), `onboarding-school-year`.
- **Affected modules**: `composeApp`.
- **Expected verification**: `:composeApp:jvmTest`; manual end-to-end flow.
- **Review-size risk**: **Medium-High** (~300–400 lines). Consider chained PRs: PR 1 = UI components, PR 2 = wiring + ViewModel.

#### 3. `gamification-rewards`
- **Scope**: Streaks and reward feedback after exercise completion; derive from cumulative progress already synced.
- **Rationale**: Gamified practice from the umbrella `learning` spec; depends on stable progress tracking.
- **Dependencies**: `exercise-practice-ui`, `progress-sync` (delivered).
- **Affected modules**: `composeApp`.
- **Expected verification**: `:composeApp:jvmTest`.
- **Review-size risk**: **Medium** (~200–300 lines).

### Phase 2 — Teacher & Classroom

#### 4. `teacher-course-ownership`
- **Scope**: Teacher-owned courses with Google Classroom-style behavior; teachers view student progress only for courses they own.
- **Rationale**: First classroom slice now that lesson-read visibility is archived and no longer blocks ownership rules.
- **Dependencies**: `backend-auth-security` (delivered), `lesson-read-access-control` (delivered).
- **Affected modules**: `server`, `shared`, `composeApp`.
- **Expected verification**: `:server:test`, `:composeApp:jvmTest`.
- **Review-size risk**: **Medium** (~250–350 lines). Consider chained PRs: PR 1 = backend ownership model + routes, PR 2 = client UI.

#### 5. `classroom-join-codes`
- **Scope**: Teacher creates a class and generates a join code; learner enrolls by entering the code.
- **Rationale**: Core classroom feature from the umbrella `classroom` spec.
- **Dependencies**: `teacher-course-ownership`.
- **Affected modules**: `server`, `shared`, `composeApp`.
- **Expected verification**: `:server:test`, `:composeApp:jvmTest`.
- **Review-size risk**: **Medium** (~200–300 lines).

### Phase 3 — Content Authoring

#### 6. `teacher-content-assignment`
- **Scope**: Teachers assign default platform theory/exercises or create custom content for a class/unit.
- **Rationale**: Core content-authoring feature from the umbrella `content-authoring` spec.
- **Dependencies**: `classroom-join-codes`, `theory-management` (delivered).
- **Affected modules**: `server`, `shared`, `composeApp`.
- **Expected verification**: `:server:test`, `:composeApp:jvmTest`.
- **Review-size risk**: **Medium-High** (~300–400 lines). Consider chained PRs.

## Recommended Next Slice

**`onboarding-school-year`**
- Foundation reconciliation is complete, so the smallest dependency-clean product slice is now the learner onboarding flow.
- It unlocks the learner track (`exercise-practice-ui` → `gamification-rewards`) without reopening any backend access-control ambiguity.
- Alternative: `teacher-course-ownership` if product wants to switch immediately to the classroom-management track now that `lesson-read-access-control` is archived.

## Deferred / Non-Goals

These are explicitly out of scope for the current roadmap cycle. Revisit after Phase 3 or when product requirements change.

| Item | Rationale |
|------|-----------|
| Learner/teacher web experience beyond the admin panel | `admin-web-panel` exists for operators, but learner- and teacher-facing product web clients remain out of scope for this roadmap cycle. |
| Advanced analytics / payments / expanded admin reporting | Product decision not yet made. |
| Regional/provincial content subdivision | MVP covers Argentina nationally; province split deferred. |
| Rich math formula rendering | Markdown-only for MVP; LaTeX/MathML later if needed. |
| Progressive hints, photo submissions | Backlog item; not required for core loop. |
| Full badges/achievements system | `gamification-rewards` covers streaks; badges are follow-up. |
| Topic-scoped chatbot | High complexity; requires AI backend integration. Not in MVP. |
| Teacher theory overrides for official topics | Teacher can create custom content in own courses; overriding official content deferred. |
| Production observability (`CallLogging`, `StatusPages`, health checks) | Valuable but can be a standalone infrastructure slice after foundation. |

## Architecture Notes

### Keep `shared` as contracts only
- The `shared` module must remain platform-agnostic: DTOs, enums, and serializable request/response shapes.
- Do **not** move Compose UI code, SQLDelight schema, Ktor client wiring, or server routing logic into `shared`.
- If a new slice needs a model used by both app and backend, add it to `shared`; keep the implementation in the owning module.

### Do not implement the umbrella directly
- `plataforma-aprendizaje-matematica` is a product initiative, not an implementable change.
- Every piece of work must be a named slice with its own proposal, design, tasks, and verification.
- If a slice grows beyond ~400 changed lines, split it into chained PRs before apply.

### Cross-module discipline
- Contract-first: when a slice touches both client and backend, define the data contract in `shared` first.
- Validate backend first, then the app.
- Keep commits reviewable: contract/models → backend → client/UI.

## Dependency Graph (Simplified)

```
lesson-read-access-control (archived) ──► teacher-course-ownership ──► classroom-join-codes ──► teacher-content-assignment
onboarding-school-year ────────────────► exercise-practice-ui ───────► gamification-rewards
```

## Maintenance

- Update this roadmap when a slice is archived or when new backlog items are promoted.
- When a slice is started, create its own change folder under `openspec/changes/`; do not edit this file for slice-level details.
- Re-evaluate ordering after each archived slice based on product feedback.
