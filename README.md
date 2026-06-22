# MathimApp

MathimApp is a personal Kotlin Multiplatform project for learning and practicing mathematics through structured courses, lessons, and exercises.

The goal of the project is to explore how a real educational app can be built across mobile and backend layers: a shared Compose Multiplatform client, a Ktor API, reusable Kotlin serialization contracts, and persistent learning data.

- Kotlin Multiplatform app architecture with shared UI and logic.
- Compose Multiplatform interface for Android and iOS.
- Ktor HTTP client integration from the shared app layer.
- Ktor backend with REST-style routes for authentication, users, courses, lessons, exercises, and progress.
- Shared serializable models in a dedicated `shared` module.
- PostgreSQL persistence with Exposed on the server.
- SQLDelight configuration for local app persistence.
- Gradle version catalog usage for dependency management.

## Core Features

- Course catalog with official learning paths.
- Lesson structure with theory content and ordered progression.
- Exercise model supporting multiple choice, true/false, and input-value questions.
- User roles for admins, teachers, and learners.
- Course enrollment through join codes.
- Progress tracking for completed lessons and total score.
- Seed data for local development.

## Tech Stack

| Area | Technology |
| --- | --- |
| Mobile/UI | Kotlin Multiplatform, Compose Multiplatform, Material 3 |
| Networking | Ktor Client |
| Backend | Ktor Server, Netty |
| Database | PostgreSQL, Exposed |
| Local persistence | SQLDelight |
| Serialization | Kotlinx Serialization |
| Build | Gradle, Version Catalogs |
| Targets | Android, iOS, JVM backend |

## Repository Structure

```text
composeApp/   Shared Compose Multiplatform app, UI state, client repositories, Ktor client code
shared/       Platform-agnostic serializable models shared by app and backend
server/       Ktor backend, routes, authentication, database tables, seed data
iosApp/       Native iOS entrypoint and Xcode project
gradle/       Version catalog and Gradle configuration
```

## Architecture Notes

MathimApp keeps contracts, client behavior, and backend behavior in separate modules:

- `shared` contains cross-platform DTOs and enums only.
- `composeApp` owns UI, client repositories, and platform app code.
- `server` owns API routes, authentication, persistence, and seed data.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---
