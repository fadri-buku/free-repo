# Worklog Pomodoro

A Manifest V3 Chrome extension that combines a pomodoro-style countdown timer
with a forced "what did you just do?" worklog entry written to a file of your
choice.

Source: [`worklog/`](../../worklog/)

## What it does

1. You pick a worklog file once in the extension's options (Markdown or text).
2. You open the extension popup, type a duration in minutes, and hit **Start**.
3. The timer runs in the background even if you close the popup.
4. When the timer ends, the extension:
   - Fires a system notification that requires interaction.
   - Opens a dedicated "locked" tab with a red alarm curtain and a looping
     beep until you mute or acknowledge.
   - Reopens the locked tab automatically if you close it before
     acknowledging — so the worklog step can't be silently skipped.
5. You type free-text into the form (notes, links, anything), hit **Save and
   acknowledge**, and the entry is appended to your worklog file with a
   timestamp header.

## Install (unpacked)

1. Open `chrome://extensions` in Chrome (or any Chromium browser that supports
   Manifest V3 and the File System Access API).
2. Enable **Developer mode** (top-right toggle).
3. Click **Load unpacked** and select the [`worklog/`](../../worklog/) folder.
4. Pin the extension to the toolbar so the popup is easy to reach.

## First-run setup

Open the extension's **Options** page (right-click the toolbar icon → Options)
and either:

- **Choose file…** — point at an existing `.md`/`.txt`/`.log` file you already
  keep a worklog in.
- **Create new file…** — picks a new file via the save dialog (defaults to
  `worklog.md`).

Chrome will ask you to grant read/write access to the file. This is a browser
security prompt, not something the extension can skip.

## Daily use

- Click the toolbar icon.
- Enter a duration (default is 25 minutes).
- Click **Start**.
- Go do the work. You can close the popup; the timer runs in the background
  service worker.
- When the alarm fires, fill in the worklog form and hit **Save and
  acknowledge**.

Worklog entries are appended in this format:

```
## 2026-04-19T14:32:11.204Z (25m)
Reviewed PR #42, drafted design doc, https://example.com/notes
```

## Folder layout

```
worklog/
├── manifest.json               # MV3 manifest
├── background/
│   └── service-worker.js       # chrome.alarms timer + lock-tab + notification logic
├── popup/                      # Toolbar popup: duration input, live countdown
│   ├── popup.html
│   ├── popup.css
│   └── popup.js
├── locked/                     # Full-screen lock page with alarm + worklog form
│   ├── locked.html
│   ├── locked.css
│   └── locked.js
├── options/                    # Settings page for picking the worklog file
│   ├── options.html
│   ├── options.css
│   └── options.js
├── lib/
│   └── file-handle-store.js    # IndexedDB persistence + permission + append helpers
└── icons/                      # 16/48/128 PNG icons
```

## How it works

- **Timer.** `chrome.alarms.create({ when: endTime })` fires once at the
  target time. Works while the service worker is asleep — Chrome wakes it.
- **Lock.** On alarm, the service worker creates an interaction-required
  notification and opens `locked/locked.html` in a new focused tab. A
  `chrome.tabs.onRemoved` listener reopens the locked tab if it is closed
  before the user acknowledges.
- **Alarm sound.** The locked page generates a looping beep with the Web
  Audio API (no audio files shipped). A **Mute alarm** button silences it
  without acknowledging.
- **File writes.** The worklog file is persisted as a `FileSystemFileHandle`
  in IndexedDB. On save, the extension re-checks `readwrite` permission
  (Chrome may require a one-click re-grant after browser restart), reads the
  existing contents, and writes `existing + newEntry` back to the same handle.

## Permissions

From [`worklog/manifest.json`](../../worklog/manifest.json):

| Permission | Why |
| --- | --- |
| `alarms` | Fire a single alarm at the end of the countdown. |
| `notifications` | Show the "time's up" notification. |
| `storage` | Persist timer state (running, endTime, awaitingAck) in `chrome.storage.local`. |
| `tabs` | Open and focus the locked tab; detect when it's closed. |

The File System Access API (`showOpenFilePicker`, `showSaveFilePicker`, and
the handle permission flow) does not require an extension permission — it
uses per-file user consent instead.

## Caveats and known limits

- **File re-grant after restart.** File System Access handles survive restart
  in IndexedDB, but Chrome will ask the user to re-confirm access the first
  time the extension tries to write after the browser restarts. The options
  page and the locked page both handle this by calling `requestPermission`.
- **Append is read-then-write.** `FileSystemWritableFileStream` has no append
  mode, so each save reads the full file and writes it back. This is fine for
  worklog-sized text files; if a file grows very large (hundreds of MB) this
  will get slow.
- **Lock scope.** The current lock is a single focused tab that reopens if
  closed. It does not overlay every other open tab — the user can still
  switch tabs while the alarm is playing. If you want a stricter all-tabs
  overlay, that's a different content-script-driven approach and a host
  permission on `<all_urls>`.
- **Autoplay.** Most Chromium builds allow autoplay on extension pages, so
  the alarm beep starts automatically. If a build blocks it, the first click
  on the page (mute or save) will resume the AudioContext.

## Not included

- No build step, bundler, or TypeScript — the extension is plain JS modules.
- No tests. The feature is small enough to validate by loading it and
  running through the flow once.
- No published store listing; this is a load-unpacked developer extension.
