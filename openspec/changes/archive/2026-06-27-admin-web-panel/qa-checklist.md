# Manual QA Checklist: Admin Web Panel SPA

This checklist is the repository-backed manual verification procedure for the PR 2 SPA slice.

## Preconditions

- Backend branch or merged state includes the admin backend slice.
- Local backend can run on `http://localhost:8080`.
- Local SPA can run from `admin-web/`.

## Checklist

1. Start the backend with `./gradlew.bat :server:run` from the repository root.
2. Start the SPA with `npm run dev` from `admin-web/`.
3. Log in as an admin user, for example `admin@test.com` with the seeded password.
4. Verify the app redirects to `/users` and the paginated user table loads.
5. Search for a user and verify the list filters to matching results.
6. Change a user's role from the dropdown and verify the UI reflects the successful update.
7. Navigate to `/courses` and verify the table shows title, creator name, enrollment count, official flag, and school year.
8. Log out and verify the app redirects back to `/login`.
9. Log in as a non-admin user and verify the UI shows `Access denied — ADMIN role required`.
10. Access `/users` without a token and verify the app redirects to `/login`.

## Scope Notes

- This checklist documents the sanctioned v1 verification path for SPA behavior because no JS test runner exists in the repository for this slice.
- The checklist is documentation only in the archive; execution evidence remains in `verify-report.md`.
