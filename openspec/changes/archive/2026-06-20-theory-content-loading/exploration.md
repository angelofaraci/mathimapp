# Exploration: Theory Content Loading (Backend-First Slice)

## Context

This exploration is the first implementation slice for the `plataforma-aprendizaje-matematica` initiative. It focuses specifically on how theory content is loaded and served from the backend, then consumed by the frontend. The broader initiative covers learner progression, classroom management, and content authoring; this slice isolates the theory-loading vertical to keep delivery reviewable and safe.

## Current State

### Architecture Overview

The project is a Kotlin Multiplatform monorepo with three modules:

- `shared/` — Cross-module `@Serializable` contracts (`User`, `Course`, `Lesson`, `Exercise`, enums)
- `server/` — Ktor JVM backend with Exposed + PostgreSQL, JWT auth, role-based access (`ADMIN`, `TEACHER`, `LEARNER`)
- `composeApp/` — Compose Multiplatform app with Ktor client, SQLDelight local cache, repository pattern

### Existing Data Model

```
Course (id, title, description, creatorId, isOfficial, joinCode)
  └── Lesson (id, courseId, title, theoryContent, orderIndex)
        └── Exercise (id, lessonId, question, options, correctAnswer, type)
```

- `Lessons` already stores `theoryContent: TEXT` inline as Markdown.
- Official courses are flagged with `isOfficial = true`.
- The seed data creates one official course ("Basic Arithmetic") with four lessons (Addition, Subtraction, Multiplication, Division), each containing theory and exercises.
- Auth uses JWT with `userId` and `role` claims. Existing authorization helpers: `requireSelfOrAdmin()`, `requireAdmin()`.

### Existing Theory Flow

1. Admin seed creates official courses + lessons with `theoryContent`.
2. `GET /courses/{courseId}/lessons` returns lessons (theory included for all authenticated users).
3. `GET /lessons/{id}` returns a single lesson with exercises (answers hidden for `LEARNER`).
4. App fetches via `LessonApi` → `KtorLessonRepository`, caches in SQLDelight `LessonEntity`.
5. There is **no** endpoint dedicated to theory; it travels inline with lesson payloads.

### Gaps Relative to Intent

| Gap | Current | Required |
|-----|---------|----------|
| Topic abstraction | `Lesson` doubles as topic + exercise container | Theory should be topic-scoped, not exercise/level-scoped |
| School-year filter | No school-year field exists | MVP must differentiate content by school year |
| Admin theory loading | Admin seeds via hardcoded Kotlin | Admin needs runtime endpoint to load/update theory |
| Teacher theory scope | Any course creator can edit any lesson | Teachers should edit theory **only** inside courses they created |
| Content authoring separation | Theory is inline in `Lessons` table | Default vs custom theory authoring is not modeled |

## Affected Areas

- `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` — add `schoolYear` to `Courses`; optionally split `theoryContent` out
- `server/src/main/kotlin/com/example/proyectofinal/models/CourseDto.kt` — add `schoolYear` to course request DTOs
- `server/src/main/kotlin/com/example/proyectofinal/routes/lessonRoutes.kt` — add admin/teacher authorization gates
- `server/src/main/kotlin/com/example/proyectofinal/service/LessonService.kt` — add theory CRUD with role checks
- `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` — add `schoolYear` to `Course`; possibly add `TheoryContent` model
- `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq` — add `schoolYear` column; update queries
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/LessonApi.kt` — add theory endpoints
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorLessonRepository.kt` — cache theory content

## Approaches

### Approach A: Minimal Extension (Add `schoolYear`, Keep Theory Inline)

- Add `schoolYear: Int` to `Courses` table and `Course` model.
- Add `GET /courses/official?schoolYear={year}` filtered endpoint.
- Add `PUT /lessons/{id}/theory` restricted to:
  - `ADMIN` for any lesson in official courses
  - `TEACHER` for lessons in courses where they are the creator
- Frontend consumes theory through existing `Lesson` payload; no dedicated theory screen needed yet.

**Pros:**
- Minimal schema change (one new column)
- Reuses existing `theoryContent` field
- Fits comfortably within 400-line PR budget
- Backend-first delivery is straightforward

**Cons:**
- `Lesson` implicitly equals `Topic`; may confuse future refactor
- Theory remains inline in `Lessons` table (scales poorly if media-rich)
- No clean separation between default platform theory and teacher custom theory

**Effort:** Low

### Approach B: Introduce `Topic` Entity Above `Lesson`

- Create `Topics` table: `id`, `title`, `schoolYear`, `theoryContent`, `isOfficial`
- Repurpose `Lesson` as a practice level referencing `topicId`
- `Course` → `Topic` → `Lesson` → `Exercise`
- Admin authors `Topic` theory; teachers create `Course`s that reference `Topic`s and may provide custom theory overrides.

**Pros:**
- Clean separation of theory (Topic) from practice (Lesson/Level)
- Supports future province-specific topic mappings
- Aligns with product language (topic-based progression)

**Cons:**
- Large schema refactor
- Requires seed data migration and cascade changes across all layers
- Likely exceeds 400-line budget for a single PR
- Higher risk; more tests needed

**Effort:** High

### Approach C: Extract `TheoryContent` Table (Authoring Versions)

- Keep `Lessons` structure mostly as-is.
- Create `TheoryContents` table: `id`, `lessonId`, `content`, `authorId`, `isDefault`
- A lesson can have multiple theory versions (one default + teacher overrides).
- Fetching a course resolves the correct theory version based on user context.

**Pros:**
- Supports default + custom theory without duplicating lesson structure
- Content-authoring friendly
- Teachers only override theory, not recreate lessons

**Cons:**
- New table, join logic, and resolution rules
- More complex than MVP likely needs
- Resolution logic must be tested thoroughly

**Effort:** Medium

## Recommendation

**Adopt Approach A (Minimal Extension) for the first slice**, with a documented migration path to Approach B or C in a follow-up phase.

Rationale:
1. The existing `Lesson` already groups theory + exercises, which satisfies the MVP requirement that theory not be exercise-scoped.
2. Adding `schoolYear` is the smallest viable step toward curriculum differentiation.
3. The 400-line review budget is a hard constraint; Approach A leaves room for tests and frontend wiring.
4. Approach B is architecturally cleaner but should be deferred until the team confirms the `Course → Topic → Lesson → Exercise` hierarchy is the long-term model.
5. The active `plataforma-aprendizaje-matematica` change is intentionally broad; creating this focused child change protects reviewability.

## Migration Path (Documented for Future Phases)

If the team later adopts Approach B:
1. Rename `Lessons` → `Topics`, keep `theoryContent`
2. Create new `Lessons` table as practice levels with `topicId` FK
3. Migrate existing exercises to reference new `Lessons`
4. Update all DTOs, routes, and repositories

## Risks

1. **Terminology debt**: Keeping `Lesson` as the topic container may confuse future developers when a true `Topic` entity is introduced.
2. **Inline content scaling**: `theoryContent` stored as `TEXT` in `Lessons` is fine for Markdown text, but if images or LaTeX rendering are added later, a separate storage strategy (CDN/blob) will be needed.
3. **Authorization gaps**: Existing `requireSelfOrAdmin` checks creatorId against userId, but does not explicitly check `role == ADMIN` for official courses. The new theory endpoints must validate both role and ownership.
4. **Markdown math rendering**: Secondary-school math may need formulas. Markdown alone handles basic text; for formulas the frontend will eventually need a math renderer (e.g., KaTeX/MathML). This is acceptable for MVP but should be flagged.
5. **Spec divergence**: If the parent `plataforma-aprendizaje-matematica` spec later mandates a `Topic` entity, this slice’s work will require refactoring.

## Open Product Questions

1. **Entity naming**: Is the current `Lesson` equivalent to the desired `Topic`? Should the team rename `Lesson` → `Topic` now to avoid later confusion?
2. **School year values**: What are the valid school-year values? Argentine secondary is typically 1st–6th year; should the platform support primary years too?
3. **Teacher autonomy**: Can teachers create entirely new topics with custom theory, or may they only override theory for existing official topics?
4. **Theory media**: Is pure Markdown text sufficient for the full secondary curriculum, or should the MVP plan for image/formula embedding from the start?
5. **Province deferral**: The MVP explicitly defers province-specific content. Should the schema include a nullable `province` column now to avoid a later migration, or is YAGNI?

## Decision: Active Change vs Child Change

**Create a child change** `theory-content-loading` under `openspec/changes/theory-content-loading/`.

The parent `plataforma-aprendizaje-matematica` remains the strategic initiative. This child change isolates the theory-loading vertical slice, making it reviewable, testable, and deliverable without blocking on undecided platform-wide questions.

## Ready for Proposal

**Yes.** The orchestrator should proceed to `sdd-propose` for the child change `theory-content-loading`. The proposal should:
- Reference the parent `plataforma-aprendizaje-matematica` initiative
- Define the slice scope: backend theory CRUD with role-based auth, school-year filtering, and frontend consumption
- Include a rollback plan: revert to seed-based theory if the new endpoints fail
- Keep the review budget under 400 lines by scoping out `Topic` entity creation and rich formula rendering
