# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository shape

`free-repo` is a lightweight mono-repo of small, self-contained productivity tools. Each tool lives in its own top-level folder and is independent — no shared build system, no package manager, no cross-folder imports.

Current tools:
- `worklog/` — Manifest V3 Chrome extension (pomodoro + forced worklog entries).

Convention (from `docs/overview.md`): one folder per feature at the repo root, each with a matching doc at `docs/features/{name}.md`. When adding a new feature, also update `README.md` and `docs/overview.md` so the index stays accurate.

## Build / test / lint

There is no build step, bundler, linter, or test suite anywhere in this repo. The Chrome extension is plain ES modules loaded directly by the browser. "Testing" means loading the extension unpacked and running through the flow.

To load `worklog/` for development:
1. `chrome://extensions` → enable Developer mode.
2. **Load unpacked** → select the `worklog/` folder.
3. After editing files, click the reload icon on the extension card (or reload via `chrome://extensions`).

`worklog.crx` and `worklog.pem` at the repo root are a packaged build + signing key; they are gitignored in practice and should not be committed.

## worklog/ architecture

The extension has four execution contexts that communicate via `chrome.runtime.sendMessage` and `chrome.storage.local`. Understanding the state flow matters more than any one file:

- **`background/service-worker.js`** — the single source of truth for timer state. Owns three `chrome.alarms`:
  - `worklog-work-end` — fires once at end of a focus session.
  - `worklog-break-end` — fires once at end of a break (short 5m, long 15m every 4th cycle).
  - `worklog-badge-tick` — 1-minute periodic alarm that refreshes the toolbar badge countdown.

  State is persisted under `worklogState` in `chrome.storage.local` with fields `{ running, paused, phase: "work"|"break"|"long-break"|null, endTime, minutes, title, remainingMs, awaitingAck, lockedTabId }`. Completed work cycles increment `worklogCycles`. All UI surfaces reconstruct their view from this state on load.

- **`popup/`** — toolbar popup. Reads state via `GET_STATE`, sends `START_TIMER`/`PAUSE_TIMER`/`RESUME_TIMER`/`CANCEL_TIMER`/`SKIP_BREAK`. Renders a circular SVG progress ring and cycle dots; the popup itself does not keep timers — it ticks a local `setInterval` only for display and re-reads state when reopened.

- **`locked/`** — full-screen "lock" page opened in a new tab when the work alarm fires. Plays `assets/alarm.wav` on loop (with a Web Audio triad-chime fallback if autoplay/WAV fails), shows the worklog form, and on **Save and acknowledge** does: append to the configured file (best-effort), append to `chrome.storage.local.worklogEntries`, `postMessage` on `BroadcastChannel("worklog")` so an open viewer refreshes, then send `ACKNOWLEDGED` to the service worker which arms the next break.

  The lock is enforced by `chrome.tabs.onRemoved` in the service worker: if the locked tab is closed while `awaitingAck` is true, it is reopened. Scope is a single focused tab — it does **not** overlay other tabs.

- **`options/`** — settings page. Picks/creates the worklog file via `showOpenFilePicker`/`showSaveFilePicker` and stores the `FileSystemFileHandle` in IndexedDB.

- **`viewer/`** — standalone page (reachable by URL) that reads `chrome.storage.local.worklogEntries` directly with no file permission required. Supports live search, Import (merge unseen entries from a `.md`/`.txt` file), and Export (Markdown Blob download).

### Dual-write storage model (important)

Every acknowledged entry goes to **two** places:
1. `chrome.storage.local` under `worklogEntries` — authoritative for the viewer and always works.
2. The user's chosen file on disk — best-effort Markdown append via the File System Access API.

If the file write fails (permission revoked, handle lost on restart, etc.), the entry is still safe in storage. This asymmetry is deliberate — do not "simplify" it by making the viewer read from the file, and do not fail `ACKNOWLEDGED` if the file write fails.

Entry format: `## <ISO timestamp> (<N>m) — <title>\n<body>\n`. The regex in `lib/entries-store.js#parseWorklogText` is the canonical parser; any change to the write format must keep the parser in sync, otherwise Import stops finding older entries.

### File append is read-then-write

`FileSystemWritableFileStream` has no append mode, so `lib/file-handle-store.js#appendToFile` reads the full file, concatenates, and rewrites with `keepExistingData: false`. This is intentional — do not try to "optimize" with `keepExistingData: true` + seek, it will corrupt short-to-long rewrites.

### Handles across browser restarts

`FileSystemFileHandle`s persist in IndexedDB but Chrome requires `requestPermission` (user gesture) after a restart. Both `options/` and `locked/` call `ensurePermission(handle, "readwrite")` before writing. The viewer uses `ensurePermission(..., "read")` for Import.

## When modifying the extension

- Keep the state shape in `service-worker.js` as the contract. If you add a field, update every consumer (popup, locked, viewer) — they all destructure it.
- Message types are plain strings; the handler in `service-worker.js` is a single `switch`. Add new cases there, don't fan out to multiple listeners.
- The manifest permissions list is minimal (`alarms`, `notifications`, `storage`, `tabs`). Adding host permissions or `<all_urls>` should be a deliberate choice — the current lock model intentionally avoids it.
- No TypeScript, no bundler, no transpile. Use native ES modules with relative paths ending in `.js` (required by Chrome for MV3 module workers and module scripts).

## Documentation is part of the change

Any code change or feature update **must** come with matching updates under `docs/`:

- Update `docs/overview.md` whenever the set of features changes, the repo layout shifts, or a convention is added/revised (keep the features table and layout diagram accurate).
- Update the relevant `docs/features/{feature}.md` for any behavior, flow, permission, file-format, or caveat change within that feature. If you add a new feature folder, create a new `docs/features/{feature}.md` alongside it.
- Also update the top-level `README.md` feature list when adding or removing a feature.

A PR that changes code without the corresponding doc update is considered incomplete.
