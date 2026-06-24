# Proposal: Admin Web Panel

## Intent

Provide a web-accessible admin interface for platform management. Today, admin actions (role changes, user inspection, course oversight) are only possible via direct API calls or the mobile app. A dedicated web panel gives operators a data-dense, keyboard-friendly workspace for day-to-day administration.

## Scope

### In Scope
- Standalone SPA (`admin-web/`) with login screen and navigation shell
- User listing with pagination and search
- Role promotion/demotion (STUDENT ↔ TEACHER, grant/revoke ADMIN)
- Course overview list (all courses, not just official-by-year)
- Bearer-token auth flow for browser sessions

### Out of Scope
- Analytics dashboards, charts, or aggregation endpoints
- Bulk user operations (batch import, mass delete)
- Audit logs and activity feeds
- Refresh-token logic or sliding sessions
- Mobile-responsive polish (desktop-first is acceptable for v1)

## Capabilities

### New Capabilities
- `admin-user-management`: list, search, and update user roles
- `admin-course-overview`: list all courses with creator and enrollment metadata

### Modified Capabilities
- `backend-auth-security`: add strictly admin-only route guards (`requireAdmin()`) beyond the current `requireSelfOrAdmin()`

## Approach

**Frontend**: React SPA (recommendation) in a new `admin-web/` folder at repo root. React offers the richest admin component ecosystem (shadcn/ui, TanStack Table) and the most open-source dashboard templates. Vue remains an acceptable alternative if the team prefers it.

**API Consumption**: REST + JWT bearer token. The SPA stores the token in `sessionStorage`, attaches it via `Authorization: Bearer <token>` on every request, and clears it on logout or 401.

**New Server Endpoints**:
- `GET /admin/users` — paginated user list (admin only)
- `GET /admin/courses` — unfiltered course list with creator info (admin only)
- `PUT /admin/users/{id}/role` — explicit role update endpoint (admin only)

**Auth Flow**: Admin logs in via existing `POST /auth/login`. If the user role is `ADMIN`, the SPA proceeds; otherwise it rejects and clears the token.

**Deployment**: Ktor serves the built static files from `admin-web/dist` under `/admin/*`, or the admin panel is deployed to a separate static host. CORS is already configured in Ktor; confirm the allowed origins list includes the admin panel's host.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `admin-web/` | New | React SPA source, build config, and static assets |
| `server/src/.../routes/` | Modified | New admin routes; enforce `requireAdmin()` |
| `server/src/.../service/` | Modified | New query methods for user listing and course aggregation |
| `shared/src/.../models/` | Modified | New admin DTOs if consumed by both app and backend |
| `server/src/.../plugins/Security.kt` | Modified | Verify CORS origins cover admin-web host |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Coarse auth model (only `ADMIN` gate) | Med | Scope v1 to super-admin only; defer fine-grained permissions |
| Missing aggregation endpoints add query load | Med | Add DB indexes for user/course listing; paginate aggressively |
| CORS mismatch between API and admin host | Low | Explicitly whitelist admin origin in Ktor CORS config |
| JWT 24h expiry frustrates web admins | Med | Document re-login expectation for v1; defer refresh tokens |
| Admin token in `sessionStorage` is vulnerable to XSS | Med | Keep token short-lived; use `HttpOnly` cookie in future iteration |

## Rollback Plan

1. Remove `admin-web/` folder and any static-file serving config from Ktor.
2. Delete new admin routes or revert the commit that introduced them.
3. Existing mobile clients and public API routes remain untouched; no data migration is required.

## Dependencies

- Node.js / pnpm (or npm) for the `admin-web/` build pipeline
- Existing Ktor CORS plugin must allow the admin panel origin

## Success Criteria

- [ ] Admin can log in via web browser and view a paginated list of all users
- [ ] Admin can change a user's role and see the change reflected immediately
- [ ] Admin can view all courses with creator names and enrollment counts
- [ ] Non-admin users hitting `/admin/*` routes receive 403
- [ ] `server:test` and `composeApp:jvmTest` still pass after backend changes
