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

### Develop with Android Studio, a USB device, and a WSL backend

Use this workflow to run the Android app from Android Studio on one physical USB-connected device
while PostgreSQL and Ktor run inside WSL. The app reaches the local backend without exposing it on
the LAN.

#### One-time prerequisites

- Set the root Gradle property in `gradle.properties`:

  ```properties
  apiBaseUrl=http://127.0.0.1:8080
  ```

  Keep this loopback URL for the physical-device workflow; do not replace it with a WSL or LAN IP.
- Install Android Studio and its Android SDK Platform-Tools. Ensure Windows can run `adb.exe`
  (from `PATH`, `ANDROID_SDK_ROOT`, `ANDROID_HOME`, or the default Android SDK location).
- On the phone, enable **Developer options** and **USB debugging**. Connect it over USB, unlock it,
  and accept the computer's RSA-debugging prompt.
- Keep exactly one USB-connected, authorized Android device while running this workflow. The helper
  intentionally rejects zero, offline, unauthorized, or multiple devices.
- Have Docker Compose available in WSL and the local `server/.env` development configuration in
  place. Do not add its contents to this document or commit it.

#### Daily workflow

1. In a WSL terminal at the repository root, start the backend and device connectivity setup:

   ```shell
   ./scripts/run-server-dev.sh --android-device
   ```

2. Approve the Windows UAC prompt. It is required to update the loopback-only Windows port proxy.
3. Wait for PostgreSQL and Ktor to start, then select the USB device in Android Studio and run the
   Android configuration normally.
4. Use the app. Requests to `http://127.0.0.1:8080` from the phone are routed to the Ktor server in
   WSL.

To stop the backend, press `Ctrl+C` in the WSL terminal. This stops Ktor; PostgreSQL remains running
in Docker for faster subsequent starts. Stop it separately when needed:

```shell
docker compose stop postgres
```

#### Recover ADB or device connectivity

If the device is not detected, is `offline`, or is `unauthorized`:

1. Disconnect and reconnect the USB cable, unlock the phone, and accept the RSA-debugging prompt.
2. In Windows, verify that ADB sees one device:

   ```powershell
   adb devices
   ```

   If necessary, restart the Windows ADB server with `adb kill-server` followed by `adb start-server`,
   then reconnect and authorize the device.
3. Run the daily command again. This recreates both the Windows port proxy and `adb reverse` mapping:

   ```shell
   ./scripts/run-server-dev.sh --android-device
   ```

Run the command again after WSL restarts as well: WSL receives a new NAT IP, so the previous proxy
target may no longer be valid.

#### Run the Windows helper directly

`scripts/configure-android-wsl-portproxy.ps1` is the Windows-side helper used by
`run-server-dev.sh --android-device`. Run it directly from an elevated Windows PowerShell session
when Ktor is already running and only the Windows-to-WSL proxy or ADB reverse mapping needs repair:

```powershell
.\scripts\configure-android-wsl-portproxy.ps1
```

If your WSL distribution is not the helper's configured default, pass its exact name:

```powershell
.\scripts\configure-android-wsl-portproxy.ps1 -DistroName <your-wsl-distribution-name>
```

Prefer `./scripts/run-server-dev.sh --android-device` for the normal daily path because it also
starts PostgreSQL, waits for it to be ready, and starts Ktor.

#### Why this routing is required

WSL uses NAT, so its internal IP can change after a restart and is not the phone's stable route to
the backend. The helper builds a private, three-step path:

```text
Android app:127.0.0.1:8080
  -> adb reverse
Windows:127.0.0.1:8080
  -> loopback-only netsh portproxy
WSL current NAT IP:8080
  -> Ktor
```

`adb reverse` makes the phone's loopback port reach Windows ADB, while the loopback-only Windows
`portproxy` forwards that traffic to WSL's current NAT address. This is why the app can retain
`apiBaseUrl=http://127.0.0.1:8080` and why UAC approval plus a fresh setup are required after a WSL
restart.

---
