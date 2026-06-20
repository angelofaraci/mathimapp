# Design: SQLDelight Merge Security & Contract Corrections

## Technical Approach

Apply a small corrective patch across `server`, `shared`, and `composeApp` without changing app flows. The backend remains Ktor + Exposed, the shared module owns only cross-module DTOs, and the app uses the existing singleton `httpClient`. The code currently names the student role `LEARNER`; this design treats `LEARNER` as the student role for this slice to avoid a broad enum migration.

## Architecture Decisions

| Decision | Alternatives considered | Rationale |
|---|---|---|
| Centralize JWT claim and role checks in `Security.kt` | Inline checks in each route | Keeps route changes small and prevents RBAC drift. |
| Keep routes under existing `authenticate("auth-jwt")` blocks | Create separate route groups per role | Lower review cost; helper functions provide 401/403 semantics inside current route structure. |
| Move only `CompleteLessonRequest` to `shared` | Move all progress DTOs or keep server-local | It is the only request shape consumed by both client and server. |
| Memory-only token holder in `NetworkClient.kt` | Persistent secure storage | User-approved scope allows memory-only; avoids platform-specific storage work. |
| Use Exposed `reference(..., onDelete = ReferenceOption.CASCADE)` only for approved relationships | Manual cleanup in routes | Database constraints enforce integrity even outside current route paths. |
| Hide answers by mapping learner responses to `Exercise.copy(correctAnswer = "")` | Add a new learner DTO | Smaller diff and no shared contract churn, but tasks should verify clients tolerate blank answer. |

## Data Flow

```text
Login/Register -> AuthResponse(token) -> TokenHolder.accessToken
                                      -> httpClient.defaultRequest
                                      -> Authorization: Bearer <token>
                                      -> Ktor authenticate("auth-jwt")
                                      -> Security helpers -> route handler
```

```text
Delete Course -> FK cascade -> Lessons + Exercises + EnrolledCourses + Progress links
Delete Lesson -> FK cascade -> Exercises + CompletedLessons
Delete User(LEARNER) -> FK cascade -> UserProgress + CompletedLessons + EnrolledCourses
```

## File Changes

| File | Action | Description |
|---|---|---|
| `server/src/main/kotlin/com/example/proyectofinal/plugins/Security.kt` | Modify | Read JWT secret from env/config fallback, expose principal helpers such as `currentUserId()`, `currentRole()`, `requireSelfOrAdmin(id)`, `requireAdmin()`. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/authRoutes.kt` | Modify | Reject public `ADMIN` registration; allow only `TEACHER` and `LEARNER` as student equivalent. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/userRoutes.kt` | Modify | Enforce self/admin checks for user and progress routes; use shared `CompleteLessonRequest`. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/courseRoutes.kt` | Modify | Validate authenticated identity on creator/enrolled/join/create paths; keep reads authenticated. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/lessonRoutes.kt` | Modify | Hide correct answers in lesson detail learner payloads. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/exerciseRoutes.kt` | Modify | Hide correct answers in learner exercise list/detail responses. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` | Modify | Convert relation ID columns to Exposed references with approved cascades; leave course creator non-cascading. |
| `server/src/main/kotlin/com/example/proyectofinal/seed/SeedData.kt` | Modify | Load admin email/password/name/id from env/config, hash password with BCrypt, remove credential logging. |
| `server/src/main/kotlin/com/example/proyectofinal/models/ProgressDto.kt` | Modify | Remove server-local `CompleteLessonRequest` after moving it to shared. |
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modify | Add `@Serializable data class CompleteLessonRequest(userId, lessonId, score)`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/NetworkClient.kt` | Modify | Add `object TokenHolder` and default `Authorization` header injection when token exists. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/UserApi.kt` | Modify | Send `CompleteLessonRequest` instead of full `UserProgress` for lesson completion. |
| `server/src/test/kotlin/com/example/proyectofinal/ServerIntegrationTest.kt` | Modify | Add 403/401, admin registration rejection, answer hiding, and cascade assertions. |

## Interfaces / Contracts

```kotlin
@Serializable
data class CompleteLessonRequest(
    val userId: String,
    val lessonId: String,
    val score: Int = 0
)
```

Client token contract: `TokenHolder.accessToken: String?`; when non-null, `NetworkClient` sends `Authorization: Bearer <token>`.

Security helper contract: route handlers must call self/admin helpers before accepting path/body `userId` values.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit/serialization | Shared `CompleteLessonRequest` shape | Compile app/server; add focused client API test only if existing fixtures make it small. |
| Integration | Auth rejection, role registration, progress visibility, answer hiding | Extend `ServerIntegrationTest` with Ktor `testApplication` and H2. |
| Database | FK cascades | Insert parent/child rows in H2, delete parent, assert dependents are gone and teacher-owned courses remain. |

## Migration / Rollout

No data migration in this slice. Existing deployments should recreate schema in dev/test; production rollout would require a separate migration plan before applying FK constraints to live data.

## Open Questions

- [ ] Should the product rename `LEARNER` to `STUDENT`, or keep `LEARNER` as the persisted student role? This design keeps `LEARNER` to stay within review budget.
