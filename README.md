# Studora

A pomodoro focus timer with a GitHub-style consistency heatmap — offline-first, single-user, and completely backend-free.

## Features

- **Focus timer** — 25-minute default sessions with custom durations from 5 to 180 minutes, plus pause/resume and discard.
- **Consistency heatmap** — a 16-week, Monday-first grid of study shades (0, 1, 2–3, 4–5, 6+ sessions) bucketed by start-day attribution.
- **Offline-first** — no account, no sync, no network. Everything lives on your device.

## Screenshots

Screenshots are not included yet. If you build the app locally with Android Studio, you can capture your own from the Timer and History tabs.

## Getting Started

Prerequisites:

- JDK 21
- Android SDK (with `local.properties` or `ANDROID_HOME` pointing at it)

Clone the repository and build a debug APK:

```sh
git clone https://github.com/LastElbow/Studora.git
cd Studora
./gradlew :app:assembleDebug
```

Then open the project in Android Studio (Giraffe or newer) and run it on a device or emulator running Android 8.1 (API 27) or higher.

## Architecture

- `domain/` — pure Kotlin logic: the FocusSession state machine and StudyHeat bucketing, with injected clock and time zone.
- `data/` — Room persistence for `completed_sessions` and `in_progress`, exposed through repositories.
- `ui/` — Jetpack Compose screens and Hilt-injected view models, with Timer and History bottom tabs.

The single `:app` module targets `com.bustedelbow.studora` on minSdk 27 / targetSdk 37, built with Kotlin, Jetpack Compose, AGP 9.4.1, Gradle 9.6.0, and JDK 21.

Design decisions are recorded under [`docs/adr/`](docs/adr/), including navigation ([ADR-0003](docs/adr/0003-navigation-tabs.md)).

## Testing

Run the unit test suite:

```sh
./gradlew :app:testDebugUnitTest
```

The domain layer is covered by 48 unit tests, all green as of the MVP release on 2026-10-06.

## Roadmap

The MVP intentionally leaves these out. They are candidates for future work:

- Subjects and per-subject tracking
- Break sessions
- Streaks
- Reminders and notifications
- Export / import
- Accounts and cross-device sync
- Home screen widgets

## License

Studora is released under the [MIT License](LICENSE).
