This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

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

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
### Run Backend

The backend reads its database configuration from environment variables, with local defaults for development.

Supported variables:

- `DB_URL` default: `jdbc:postgresql://localhost:5432/MathimApp`
- `DB_DRIVER` default: `org.postgresql.Driver`
- `DB_USER` default: `postgres`
- `JWT_SECRET` or JVM property `jwt.secret` (required, no default)
- `ADMIN_SEED_ID` or JVM property `seed.admin.id` (required when seed data is enabled)
- `ADMIN_SEED_NAME` or JVM property `seed.admin.name` (required when seed data is enabled)
- `ADMIN_SEED_EMAIL` or JVM property `seed.admin.email` (required when seed data is enabled)
- `ADMIN_SEED_PASSWORD` or JVM property `seed.admin.password` (required when seed data is enabled)

Example on Windows PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/MathimApp"
$env:DB_USER="postgres"
$env:DB_PASSWORD="<set-local-db-password>"
$env:JWT_SECRET="<set-long-random-jwt-secret>"
$env:ADMIN_SEED_ID="admin-1"
$env:ADMIN_SEED_NAME="Admin"
$env:ADMIN_SEED_EMAIL="admin@example.com"
$env:ADMIN_SEED_PASSWORD="<set-admin-seed-password>"
.\gradlew.bat :server:run
```

Example on macOS/Linux:

```shell
export DB_URL="jdbc:postgresql://localhost:5432/MathimApp"
export DB_USER="postgres"
export DB_PASSWORD="<set-local-db-password>"
export JWT_SECRET="<set-long-random-jwt-secret>"
export ADMIN_SEED_ID="admin-1"
export ADMIN_SEED_NAME="Admin"
export ADMIN_SEED_EMAIL="admin@example.com"
export ADMIN_SEED_PASSWORD="<set-admin-seed-password>"
./gradlew :server:run
```

If you prefer JVM properties instead of environment variables, pass `-Djwt.secret=...` and `-Dseed.admin.*=...` to the Gradle run task or your server process.
