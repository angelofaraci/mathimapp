# Delta for API Base URL Configuration

## Purpose

Define how the app resolves its API base URL across platforms and build configurations while keeping existing client behavior intact.

## Requirements

### Requirement: Platform Default Base URL Resolution

The system MUST resolve a platform-appropriate default API base URL without requiring a single hardcoded emulator-only value. The default MUST preserve local development convenience on supported targets.

#### Scenario: Default works without source edits

- GIVEN no build-time override is provided
- WHEN the app starts on a supported platform
- THEN it uses that platform's local-development-friendly API base URL
- AND no source code change is required to run locally

#### Scenario: Base URL stays fixed during the session

- GIVEN the app has started and resolved its API base URL
- WHEN network requests are made later in the same run
- THEN the resolved base URL remains unchanged
- AND runtime URL switching is not required for this change

### Requirement: Build-Time API Base URL Override

The system MUST allow a developer to provide an alternate API base URL at build time without source edits. The override MAY point to a LAN IP or other reachable host for physical-device testing.

#### Scenario: Developer overrides the base URL

- GIVEN a build-time override is configured
- WHEN the app starts
- THEN the app uses the overridden API base URL
- AND requests are sent to that host

#### Scenario: LAN IP testing is supported

- GIVEN a developer wants to test on a physical device
- WHEN the build-time override targets a LAN IP address
- THEN the app uses that LAN IP as the API base URL
- AND no source edit is needed to change the host

### Requirement: Existing API Client Behavior Is Preserved

The system MUST keep existing API client behavior unchanged except for the source of the base URL. Auth headers, serialization, and request construction MUST remain the same.

#### Scenario: Requests still look the same

- GIVEN the same authenticated user and request payload
- WHEN the base URL changes through configuration
- THEN the client still sends the same headers and serialized body
- AND only the destination base URL changes

#### Scenario: Platform defaults do not alter client semantics

- GIVEN the app uses a platform default base URL
- WHEN an API client performs a request
- THEN the request semantics match the existing client behavior
- AND the change remains limited to base URL resolution
