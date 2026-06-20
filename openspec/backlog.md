# OpenSpec Backlog

## Candidate Changes

### Teacher course ownership and progress visibility

- Define teacher-owned courses with Google Classroom-style behavior.
- Allow teachers to create and manage their own courses.
- Allow teachers to view student progress only for courses they own.
- Clarify enrollment, ownership transfer, and admin override rules.

### Production backend readiness

- Add basic observability for server runtime failures.
- Consider `CallLogging`, `StatusPages`, health checks, and consistent error responses.
- Document operational expectations for auth, database, and seed startup paths.

### Versioned database migrations

- Replace reliance on `SchemaUtils.create(...)` with a versioned migration strategy.
- Define rollback or fix-forward expectations before using real production data.

### Configurable KMP API base URL

- Replace hardcoded `http://10.0.2.2:8080` with environment/build configuration.
- Support Android emulator, physical devices, iOS, desktop, staging, and production targets.

### Role naming cleanup

- Decide whether the product language should use `LEARNER` or `STUDENT`.
- Align shared models, specs, backend rules, UI copy, and documentation once decided.
