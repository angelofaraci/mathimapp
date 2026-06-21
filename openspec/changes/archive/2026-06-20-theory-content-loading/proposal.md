# Proposal: Theory Content Loading

## Intent

Enable runtime loading and editing of lesson theory content with school-year filtering. Replace hardcoded seed-based theory with admin-managed default content and teacher-managed custom content within their own courses.

## Scope

### In Scope
- Add `schoolYear` to `Course` model, database, and local cache.
- `GET /courses/official?schoolYear={year}` filtered endpoint.
- `PUT /lessons/{id}/theory` restricted by role (ADMIN any official lesson; TEACHER only their own courses).
- Frontend consumption of theory through existing lesson payloads.
- Update SQLDelight schema and repository to cache new fields.

### Out of Scope
- Separate `Topic` entity; `Lesson` remains the topic container for MVP.
- Rich math formula rendering (Markdown only).
- Extracted/versioned `TheoryContent` table.
- Teacher theory overrides for official topics.
- Province-aware filtering or admin content panel UI.

## Capabilities

### New Capabilities
- `theory-management`: Role-gated endpoints to load and update lesson theory content.
- `school-year-filtering`: Filter official courses and lessons by school year.

### Modified Capabilities
- `client-server-contract`: Shared `Course` model adds `schoolYear`; new theory update request shape.
- `backend-auth-security`: Authorization rules extended for theory mutation scope.

## Approach

Adopt **Approach A (Minimal Extension)** from exploration. Add `schoolYear` to the existing `Courses` table and reuse the inline `theoryContent` field in `Lessons`. New endpoints enforce role-based auth without introducing new entities. Frontend consumes theory through existing lesson flows.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `server/.../database/Tables.kt` | Modified | Add `schoolYear` to `Courses`. |
| `server/.../routes/lessonRoutes.kt` | Modified | Add `GET /courses/official` and `PUT /lessons/{id}/theory`. |
| `server/.../service/LessonService.kt` | Modified | Theory CRUD with role checks. |
| `server/.../models/CourseDto.kt` | Modified | Add `schoolYear` to course request DTOs. |
| `shared/.../models/Models.kt` | Modified | Add `schoolYear` to `Course`. |
| `composeApp/.../sqldelight/AppDatabase.sq` | Modified | Add `schoolYear` column; update queries. |
| `composeApp/.../data/LessonApi.kt` | Modified | Add theory and filtered course endpoints. |
| `composeApp/.../data/KtorLessonRepository.kt` | Modified | Cache theory and school year. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `Lesson` as topic container causes confusion | Med | Document terminology debt; plan `Topic` migration. |
| Inline TEXT column limits future media | Med | Defer blob/CDN until formula/media needs confirmed. |
| Auth gaps on official course checks | Low | Explicit role + ownership validation in new endpoints. |

## Rollback Plan

- Revert schema migration (drop `schoolYear`).
- Remove new routes and service methods.
- Restore seed-based theory loading as sole content source.
- Frontend falls back to existing lesson list behavior.

## Dependencies

- Parent initiative `plataforma-aprendizaje-matematica` architecture decisions.
- Existing JWT auth and role system.

## Success Criteria

- [ ] Admin can update theory for any official course lesson.
- [ ] Teacher can update theory only for lessons in courses they created.
- [ ] Learner can browse official courses filtered by school year.
- [ ] Frontend caches and displays theory through existing lesson flows.
- [ ] Delivery stays within 400-line review budget.
