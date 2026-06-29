# OpenSpec Backlog

## Candidate Changes

### Teacher course ownership and progress visibility

- Define teacher-owned courses with Google Classroom-style behavior.
- Allow teachers to create and manage their own courses.
- Allow teachers to view student progress only for courses they own.
- Clarify enrollment, ownership transfer, and admin override rules.

### Learning paths

- Reference brief: `openspec/learning-paths-brief.md`
- Introduce platform-curated learning paths as the default learner experience; do not treat them as teacher-created private courses.
- Use school year as the primary axis, with one default platform path per year.
- During onboarding, automatically recommend/assign the default path for the selected year with no extra confirmation.
- If the learner changes school year later, recalculate the recommended path but ask for confirmation before switching.
- Let learners manually switch paths, remember the last opened path, and keep access to other paths lightweight.
- Keep progression linear lesson-by-lesson without hard-locking future lessons; show the full ordered lesson list from the start and visually emphasize the next recommended lesson.
- Reuse the existing lesson/exercise completion model: lesson completion still derives from exercises, path progress is percentage-based, and completed lessons count across every path that includes them.
- Avoid special migration when switching paths; shared completed lessons should simply count.
- Model each path with a name, description, visible objective, and structured objective label; v1 structured objective type is only `grade-level`.
- Open a path on a summary screen first, then use a primary CTA such as "Start" to focus the path view on the first incomplete lesson instead of deep-linking straight into lesson content.
- Keep completed lessons accessible for review.
- Defer 100% completion rewards to `gamification-rewards`, not `learning-paths` v1.

### Production backend readiness

- Add basic observability for server runtime failures.
- Consider `CallLogging`, `StatusPages`, health checks, and consistent error responses.
- Document operational expectations for auth, database, and seed startup paths.

### Web admin panel

- Provide an administrator-facing panel accessible through the web.
- Use it for operational/admin workflows that do not belong in the student or teacher mobile experience.
- Technology stack is intentionally undecided; evaluate frontend/backend integration options when the slice is planned.

### ~~Versioned database migrations~~ ✅ (archived 2026-06-22)

- Replaced `SchemaUtils.create(...)` with Flyway programmatic migration on startup.
- Baseline V1 (`V1__baseline_current_schema.sql`) + guarded V2 (`V2__ensure_courses_school_year.sql`) in place.
- `openspec/backlog.md` documents the rule: every future server schema change needs a matching Flyway migration script.
- CI drift/checksum validation remains deferred.

### Configurable KMP API base URL

- Replace hardcoded `http://10.0.2.2:8080` with environment/build configuration.
- Support Android emulator, physical devices, iOS, desktop, staging, and production targets.

### Role naming cleanup

- Decide whether the product language should use `LEARNER` or `STUDENT`.
- Align shared models, specs, backend rules, UI copy, and documentation once decided.
