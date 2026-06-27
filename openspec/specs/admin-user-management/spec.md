# admin-user-management Specification

## Purpose

Provide admin-only capabilities for listing, searching, and updating user accounts and roles through REST endpoints and a dedicated admin SPA login flow.

## Requirements

### Requirement: Paginated User Listing

The server MUST expose `GET /admin/users` with optional `query`, `page`, and `size` parameters, and MUST return a paginated list with `id`, `name`, `email`, and `role` (the `Users` table has no `createdAt` column; audit timestamps are deferred to a future change). The endpoint MUST be guarded by `requireAdmin()`.

#### Scenario: Admin retrieves first page

- GIVEN a request with a valid ADMIN JWT
- WHEN the admin calls `GET /admin/users?page=0&size=20`
- THEN the server SHALL return up to 20 users with total count and page metadata

#### Scenario: Non-admin receives 403

- GIVEN a valid non-ADMIN JWT (STUDENT or TEACHER)
- WHEN the request targets `GET /admin/users`
- THEN the server SHALL reject with 403

#### Scenario: Empty search returns no results

- GIVEN no users match the query parameter
- WHEN the admin calls `GET /admin/users?query=nonexistent`
- THEN the server SHALL return an empty page with count 0

### Requirement: Role Update

The server MUST expose `PUT /admin/users/{id}/role` accepting a new role value, MUST persist the change, and MUST return the updated user. The endpoint MUST be guarded by `requireAdmin()`.

#### Scenario: Admin promotes user to TEACHER

- GIVEN an ADMIN JWT and a target user with role STUDENT
- WHEN the admin calls `PUT /admin/users/{id}/role` with `{"role":"TEACHER"}`
- THEN the server SHALL update the role and return the user with role TEACHER

#### Scenario: Non-existent user returns 404

- GIVEN an ADMIN JWT
- WHEN the admin calls `PUT /admin/users/{nonexistentId}/role`
- THEN the server SHALL return 404

#### Scenario: Invalid role value returns 400

- GIVEN an ADMIN JWT
- WHEN the admin sends an invalid role string
- THEN the server SHALL return 400

### Requirement: Admin SPA Login Gate

The admin SPA MUST authenticate via the existing `POST /auth/login` endpoint and MUST proceed only when the returned user role is ADMIN. Non-ADMIN responses MUST clear the token and display an access-denied message.

#### Scenario: Admin login navigates to dashboard

- GIVEN a user with role ADMIN enters valid credentials on the admin SPA login page
- WHEN the SPA calls `POST /auth/login`
- THEN the SPA SHALL store the bearer token and navigate to the admin dashboard

#### Scenario: Non-admin login is blocked at the SPA

- GIVEN a user with role STUDENT enters valid credentials on the admin SPA login page
- WHEN the SPA calls `POST /auth/login`
- THEN the SPA SHALL clear the token and display "Access denied — ADMIN role required"
