# admin-course-overview Specification

## Purpose

Provide an admin-only REST endpoint that returns all courses without school-year filtering, including creator metadata and enrollment counts.

## Requirements

### Requirement: All-Courses Listing

The server MUST expose `GET /admin/courses` returning every course regardless of `schoolYear` or creator, with creator name and enrollment count. The endpoint MUST be guarded by `requireAdmin()`.

#### Scenario: Admin views all courses

- GIVEN a valid ADMIN JWT
- WHEN the admin calls `GET /admin/courses`
- THEN the server SHALL return all courses with creator name and enrollment count, unfiltered by schoolYear

#### Scenario: Non-admin receives 403

- GIVEN a valid non-ADMIN JWT
- WHEN the request targets `GET /admin/courses`
- THEN the server SHALL reject with 403

#### Scenario: Empty course list

- GIVEN the platform has zero courses
- WHEN an admin calls `GET /admin/courses`
- THEN the server SHALL return an empty array with HTTP 200
