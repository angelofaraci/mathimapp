# Course Empty State Specification

## Purpose
Define how `CourseList` should present successful course data when the list is empty versus populated.

## Requirements

### Requirement: CourseList empty-state rendering
The system MUST render a centered empty-state message when `CourseList` receives an empty list, and MUST render the existing list UI when the list contains one or more courses.

#### Scenario: Empty course list
- GIVEN the app has loaded successfully and `CourseList` receives an empty list
- WHEN the screen is displayed
- THEN a centered empty-state message is shown instead of a blank list area
- AND the existing course rows are not shown

#### Scenario: Non-empty course list
- GIVEN the app has loaded successfully and `CourseList` receives one or more courses
- WHEN the screen is displayed
- THEN the existing course list UI is rendered
- AND no empty-state message is shown

### Requirement: Empty-state preview availability
The system SHOULD provide a preview for the empty `CourseList` state.

#### Scenario: Empty-state preview
- GIVEN the developer opens the Compose preview for `CourseList`
- WHEN the empty-list preview is rendered
- THEN the empty-state layout is visible
- AND the preview does not display course rows
