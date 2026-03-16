# Proposal: Plataforma de Aprendizaje Matematica

## Intent

Launch a mobile-first, Duolingo-like math platform for Argentina that serves individual learners and schools from day one. The change defines the first product slice needed to replace the current placeholder app with a gamified learning experience, province-aware progression, teacher-managed classes, and a Kotlin end-to-end architecture.

## Scope

### In Scope
- Student onboarding with province selection, grade/year path, and topic-based progression.
- Unit experience with always-available theory, interactive exercises, streaks/rewards, and child/teen-oriented presentation.
- Teacher workflows for class creation, code-based joining, and custom/default theory and exercise assignment.
- Kotlin backend direction with PostgreSQL persistence and SQLDelight-based data access contracts.

### Out of Scope
- Web experience beyond future compatibility planning.
- Advanced analytics, payments, and province-specific content authoring tooling.

## Approach

Define the change around three domains: learner progression, classroom management, and content authoring. Use shared Kotlin models in `composeApp` for mobile UX and plan a separate Kotlin backend module for auth, classes, content, and progression APIs backed by PostgreSQL; use SQLDelight for typed queries and schema coordination where it fits the stack.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/` | Modified | Replace sample UI with onboarding, map, unit, and class flows. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/` | Modified | Add progression, onboarding, and classroom behavior tests. |
| `build.gradle.kts` | Modified | Register product modules/dependencies for mobile and backend growth. |
| `settings.gradle.kts` | Modified | Include new shared/domain or backend modules as needed. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Scope is too broad for one delivery | High | Split follow-up specs by domain and ship MVP slices. |
| Province-specific curriculum becomes inconsistent | Med | Model curriculum as configurable data, not hardcoded screens. |
| SQLDelight/Postgres fit may be unclear on backend | Med | Validate in design phase before locking persistence implementation. |

## Rollback Plan

Keep the current app shell behind a feature boundary until the new flows stabilize; revert new modules and restore the placeholder `App()` entry if MVP integration fails.

## Dependencies

- Curriculum mapping by Argentine province and school year.
- Product decisions for gamification loop, teacher roles, and backend service boundaries.

## Success Criteria

- [ ] Specs define learner, teacher, class, and content requirements for the MVP.
- [ ] The app direction supports both self-serve learners and school-managed classrooms.
- [ ] Architecture decisions cover Kotlin mobile + Kotlin backend + PostgreSQL/SQLDelight viability.
