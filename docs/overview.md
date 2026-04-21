# Repository overview

`free-repo` is a lightweight mono-repo for small, independent tools. Each tool
lives in its own top-level folder and has its own feature doc under
[`docs/features/`](./features/).

## Layout

```
free-repo/
├── README.md                  # Entry point, links to docs
├── docs/
│   ├── overview.md            # This file
│   └── features/
│       ├── worklog.md         # Per-feature docs
│       └── countdown-timer.md
├── worklog/                   # Chrome extension: pomodoro + worklog
└── countdown-timer/           # Android app (Kotlin): countdown timer
```

## Conventions

- One folder per feature at the repo root. Keep the folder self-contained so it
  can be used, copied, or moved without pulling in siblings.
- Each feature gets a matching doc at `docs/features/{feature-name}.md`. That
  doc covers what the feature does, how to run it, and any relevant caveats.
- Update [`README.md`](../README.md) and this file when adding a new feature so
  the index stays accurate.

## Features

| Feature | Folder | Doc |
| --- | --- | --- |
| Worklog Pomodoro | [`worklog/`](../worklog/) | [`features/worklog.md`](./features/worklog.md) |
| Countdown Timer (Android, Kotlin) | [`countdown-timer/`](../countdown-timer/) | [`features/countdown-timer.md`](./features/countdown-timer.md) |
