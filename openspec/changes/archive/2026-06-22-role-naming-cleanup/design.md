# Design: Rename LEARNER to STUDENT with Backward Compatibility

## Technical Approach

Rename the shared role contract from `LEARNER` to `STUDENT`, then route all string-to-role reads through a compatibility parser that maps both legacy `LEARNER` and new `STUDENT` to `UserRole.STUDENT`. Writes remain canonical: server DB writes, JWT claims, API payloads, and SQLDelight local rows emit `STUDENT`. No access-rule behavior changes.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|----------|--------|--------------------------|-----------|
| Parser ownership | Add a platform-agnostic parser on `UserRole` in `shared/src/commonMain/.../Models.kt`, e.g. `UserRole.fromStorageValue(value: String): UserRole?` or throwing `parse(...)`. | Duplicate parsers in `server` and `composeApp`; put SQLDelight/Ktor adapters in `shared`. | Role value compatibility is part of the shared contract. The parser has no Exposed, Ktor, SQLDelight, or platform dependency, so `shared` stays contract-focused while both modules reuse one mapping. Framework adapters stay in owning modules. |
| Server read/write boundary | Use the shared parser in `ServiceMappers.kt`, `AuthService.AuthUserRecord.toUser()`, and `Security.currentRole()`. Keep writes as `role.name` after enum rename. | Keep `valueOf`; migrate all DB rows immediately. | `valueOf` would crash on legacy rows/tokens. Immediate migration is out of scope until versioned migrations exist. |
| App local persistence | Replace `EnumColumnAdapter<UserRole>` for `UserEntity.role` in `AppModule.kt` with a custom `ColumnAdapter<UserRole, String>` that decodes via shared parser and encodes `value.name`. | Keep `EnumColumnAdapter`; move SQLDelight adapter to `shared`. | `EnumColumnAdapter` cannot decode removed legacy values. SQLDelight adapter is app infrastructure and belongs in `composeApp`. |
| Legacy JSON request-body compatibility | Do not add a custom kotlinx serializer for `LEARNER` request bodies in this slice. | Accept both enum spellings in request JSON via custom serializer. | The approved compatibility scope covers persisted rows, local cache rows, and JWT claims only. Broadening request-body parsing would add API-surface behavior that the spec does not require. |

## Data Flow

Legacy reads:

```text
DB row/JWT/local row "LEARNER"
        └─→ shared UserRole parser ──→ UserRole.STUDENT ──→ existing student behavior
```

New writes:

```text
Register/update/login/local cache
        └─→ UserRole.STUDENT.name ──→ "STUDENT"
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modify | Rename enum constant/default to `STUDENT`; add compatibility parser on `UserRole`. |
| `server/src/main/kotlin/.../database/Tables.kt` | Modify | Change `Users.role` default to `"STUDENT"`. |
| `server/src/main/kotlin/.../models/UserDto.kt` | Modify | Change registration default to `UserRole.STUDENT`. |
| `server/src/main/kotlin/.../service/AuthService.kt` | Modify | Parse stored role via shared parser; create users with `STUDENT` when defaulted. |
| `server/src/main/kotlin/.../service/ServiceMappers.kt` | Modify | Parse `Users.role` via shared parser. |
| `server/src/main/kotlin/.../plugins/Security.kt` | Modify | Parse JWT role claims via shared parser; new tokens continue using canonical `.name`. |
| `server/src/main/kotlin/.../{routes,service}/*.kt` | Modify | Rename `UserRole.LEARNER` checks to `UserRole.STUDENT`; keep authorization semantics unchanged. |
| `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq` | Modify | Change `UserEntity.role` default to `'STUDENT'`. |
| `composeApp/src/commonMain/kotlin/.../di/AppModule.kt` | Modify | Add `UserRole` column adapter accepting `LEARNER`/`STUDENT`, encoding `STUDENT`. |
| `composeApp/src/commonMain/kotlin/.../data/KtorUserRepository.kt` | Modify | Change fallback role to `UserRole.STUDENT`. |
| `server/src/test/**`, `composeApp/src/commonTest/**` | Modify | Update fixtures/assertions and add legacy compatibility tests. |
| `openspec/specs/backend-auth-security/spec.md` | Modify | On archive, merge STUDENT terminology and compatibility requirement. |

## Interfaces / Contracts

```kotlin
enum class UserRole {
    ADMIN, TEACHER, STUDENT;

    companion object {
        fun parse(value: String): UserRole? = when (value.trim().uppercase()) {
            "LEARNER", "STUDENT" -> STUDENT
            "TEACHER" -> TEACHER
            "ADMIN" -> ADMIN
            else -> null
        }
    }
}
```

Call sites that need nullable auth behavior (`currentRole`) should keep invalid values as `null`; persistence mappers should fail loudly if an unknown stored value appears.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `UserRole.parse` maps `LEARNER` and `STUDENT` to `STUDENT`; unknown values fail predictably. | Shared/common tests or nearest existing module tests. |
| Server | Legacy DB roles and legacy JWT claims authenticate/hydrate as students; new registration/login tokens emit `STUDENT`; role guards behave unchanged. | Update `:server:test` integration/service tests. |
| App | SQLDelight adapter decodes legacy `LEARNER` rows and encodes new `STUDENT` rows; repository fallback is `STUDENT`. | Update `:composeApp:jvmTest` tests with SQLite driver. |
| Build | SQLDelight generated interfaces reflect schema default/adapter. | Run SQLDelight generation/build validation. |

## Migration / Rollout

No data rewrite in this slice. Existing rows/tokens remain valid through the parser. Remove legacy `LEARNER` support only after `versioned-db-migrations` introduces a durable migration.

Verification commands:

```bash
./gradlew :server:test
./gradlew :composeApp:jvmTest
./gradlew :composeApp:generateSqlDelightInterface
```

## Resolved Scope Note

Legacy `LEARNER` JSON request-body compatibility is intentionally out of scope. The implemented change preserves compatibility only for persisted rows, local cache rows, and JWT claims, and does not add a custom kotlinx serializer.
