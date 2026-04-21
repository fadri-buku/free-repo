# Countdown Timer (Android, Kotlin)

A single-screen native Android app written in Kotlin. Enter a duration
(hours / minutes / seconds), then **Start**, **Pause**, **Resume**, or
**Reset** the countdown. The remaining time is shown as `HH:MM:SS` and a
local notification fires when the timer finishes.

See [`docs/features/countdown-timer.md`](../docs/features/countdown-timer.md)
for the full write-up.

## Project layout

```
countdown-timer/
├── build.gradle.kts             # Project-level Gradle (Kotlin DSL)
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/              # gradle-wrapper.properties only
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/freerepo/countdown/
        │   │   ├── CountdownLogic.kt      # Pure-Kotlin state machine
        │   │   ├── CountdownViewModel.kt  # LiveData + coroutine tick loop
        │   │   ├── TimeFormatter.kt       # ms → HH:MM:SS
        │   │   ├── CountdownNotifier.kt   # Local notification
        │   │   └── MainActivity.kt        # UI
        │   └── res/…                      # Layout, strings, theme, icon
        └── test/java/com/freerepo/countdown/
            ├── CountdownLogicTest.kt
            └── TimeFormatterTest.kt
```

## Build and run

This folder is a standalone Android Studio project.

1. Open `countdown-timer/` in Android Studio (Giraffe / Koala or newer).
2. Let Gradle sync. Android Studio will populate the Gradle wrapper jar
   (`gradle/wrapper/gradle-wrapper.jar`) on first sync; this repo does not
   commit the wrapper binary.
3. Run the **app** configuration on an emulator or device (minSdk 26,
   Android 8.0+).

From the command line:

```
./gradlew :app:assembleDebug       # Build
./gradlew :app:test                # Run unit tests
./gradlew :app:installDebug        # Install on a connected device
```

## Architecture notes

- **State persistence on rotation** is handled the idiomatic way: the
  Activity is allowed to be recreated, but `CountdownViewModel` survives
  the `ViewModelStore` across configuration changes, so the running timer
  and its state transitions continue uninterrupted.
- **Timer logic is separated from Android** in `CountdownLogic`. It takes
  a `() -> Long` clock function so unit tests can drive it with a fake
  clock without Robolectric.
- **Remaining time is computed from a deadline**, not decremented from a
  counter. `deadlineMs = clock() + durationMs`, and every tick recomputes
  `deadlineMs - clock()`. This is robust to the UI thread stalling and to
  the tick interval drifting.
- **The tick loop runs as a `viewModelScope.launch`** with a 200 ms delay
  between updates — fine-grained enough to feel live, coarse enough to
  keep the main thread quiet.
- **Notification permission (Android 13+)** is requested on first launch.
  If the user denies it the app still works; `CountdownNotifier` silently
  skips posting the notification.
