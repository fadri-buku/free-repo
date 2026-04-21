# Countdown Timer (Android, Kotlin)

A small native Android app that implements a countdown timer with start,
pause, resume, and reset, surviving screen rotation and firing a local
notification when the timer runs out.

Source: [`countdown-timer/`](../../countdown-timer/)

## What it does

1. Launch the app and enter a duration using three fields: hours,
   minutes, seconds. Defaults to `00:00:10`.
2. Tap **Start** — the large `HH:MM:SS` readout begins counting down in
   real time (updates every 200 ms).
3. Tap **Pause** at any point to freeze the remaining time. The button
   label flips to **Resume**; tap it to continue from exactly where you
   paused.
4. Tap **Reset** to return the readout to the initial duration you
   entered. The state drops back to *Ready*.
5. When the timer reaches `00:00:00` it enters the *Finished* state and a
   local notification is posted (channel: "Countdown Timer", category:
   alarm). Tapping the notification re-opens the app.

## Screens

One `MainActivity` with a single XML layout
(`app/src/main/res/layout/activity_main.xml`):

- Title
- `HH:MM:SS` readout (monospace, 72sp)
- State label (Ready / Running / Paused / Finished)
- Three number inputs (hours / minutes / seconds) — disabled while a
  timer is running or paused
- Three buttons: Start, Pause/Resume, Reset

## Architecture

Four source files under `app/src/main/java/com/freerepo/countdown/`:

| File | Role |
| --- | --- |
| `CountdownLogic.kt` | Pure-Kotlin state machine. Holds `state`, `initialDurationMs`, and a `deadlineMs`. `remainingMs()` is computed from a clock function so tests can drive it deterministically. |
| `CountdownViewModel.kt` | Wraps `CountdownLogic`, exposes `LiveData<Long>` for remaining time and `LiveData<State>` for state. Runs a `viewModelScope` coroutine that polls every 200 ms while the timer is running. |
| `TimeFormatter.kt` | Pure function, `format(millis): String` → `"HH:MM:SS"`. Rounds sub-second remainders **up** so a 10-second timer shows `00:10` the instant Start is pressed, not `00:09`. |
| `CountdownNotifier.kt` | Creates (once) a high-importance notification channel and posts a notification when the timer finishes. No-op if notification permission is denied. |
| `MainActivity.kt` | Inflates the layout via ViewBinding, reads input fields, wires buttons to the ViewModel, observes LiveData, and requests `POST_NOTIFICATIONS` on Android 13+. |

### State persistence (rotation)

`CountdownViewModel` is scoped to the Activity's `ViewModelStore`, which
survives `Activity` recreation on configuration change. On rotation the
Activity is destroyed and re-created, re-subscribes to the same ViewModel,
and immediately renders the current state — the running timer continues
uninterrupted because the deadline is kept in ViewModel memory.

We deliberately do **not** set `android:configChanges` on the Activity.
Relying on ViewModel (rather than suppressing recreation) is the
recommended Android pattern and handles other config changes (theme,
locale, font scale) the same way.

### Why a deadline, not a counter

`CountdownLogic` stores `deadlineMs = clock() + durationMs` rather than
decrementing a counter. On every tick it recomputes
`remaining = max(deadlineMs - clock(), 0)`. If the UI thread stalls, if
the tick interval jitters, or if the device pauses and resumes the
coroutine, the remaining time stays honest.

On pause, `pausedRemainingMs` is snapshotted from the clock. On resume,
`deadlineMs` is rebuilt as `clock() + pausedRemainingMs`.

## Permissions

| Permission | Reason |
| --- | --- |
| `POST_NOTIFICATIONS` | Local notification when the timer finishes. Requested at runtime on Android 13 (API 33) and newer. If denied the timer still works — the notification is silently skipped. |

No internet, storage, or background-service permissions. When the app is
backgrounded the coroutine tick loop continues while the process is
alive; the timer is not a foreground service, so Android may suspend it
if the process is killed. For a longer-running timer that survives
process death, a foreground service or `AlarmManager` would be the next
step — intentionally out of scope for this exercise.

## Build and run

The folder is a standalone Android Studio project — Kotlin DSL Gradle,
Android Gradle Plugin 8.2, Kotlin 1.9, Material 3 theme, `minSdk = 26`,
`targetSdk = 34`.

- Android Studio: open `countdown-timer/`, let Gradle sync (this
  populates the wrapper jar, which is not committed), then run the
  `app` configuration.
- CLI: `./gradlew :app:assembleDebug` / `:app:installDebug`.

### Unit tests

Unit tests use plain JUnit 4 (no Robolectric, no instrumentation):

```
./gradlew :app:test
```

`CountdownLogicTest` drives the state machine through start → pause →
resume → reset transitions using a `FakeClock` and asserts against
`remainingMs()` at every step. `TimeFormatterTest` covers zero,
negative, sub-second round-up, and the hours/minutes/seconds composition.

## Caveats

- This is a single-Activity demo, not a productivity tool like `worklog/`.
  There is no persistence of the last entered duration, no preset chips,
  and the timer does not survive the process being killed.
- On Android 12 and below the notification fires without a runtime
  permission prompt (older permission model). On Android 13+ the app
  requests `POST_NOTIFICATIONS` on first launch.
- No foreground service: if you lock the device and the process is
  reclaimed, the timer will have disappeared when you return. This is a
  deliberate simplification.
