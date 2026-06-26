# Delta for theory-management

## MODIFIED Requirements

### Requirement: Theory Content Read Access

The system MUST allow authenticated users to load lesson theory content based on visibility tiers: official courses (any authenticated user), enrolled courses (enrolled students only), course owner (TEACHER creator), and ADMIN (any lesson).
(Previously: Generic "users they can access" without specifying visibility tiers)

#### Scenario: Lesson theory is returned

- GIVEN an authenticated user requests a lesson within their visibility scope
- WHEN the request is processed
- THEN the response includes lesson theory content

#### Scenario: Inaccessible lesson is blocked

- GIVEN an authenticated user requests a lesson outside their visibility scope
- WHEN the request is processed
- THEN the system returns Forbidden

#### Scenario: Non-existent lesson returns NotFound

- GIVEN no lesson exists with the requested ID
- WHEN any authenticated user requests the lesson
- THEN the system returns NotFound
