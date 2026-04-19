# Motion Cam

A minimal Chrome extension that uses your webcam to detect and log movement.
The tracker page shows the live camera feed with a bounding box drawn around
anything that's moving, a motion intensity meter, and a log of timestamped
motion events.

## Folder

```
motion-cam/
├── manifest.json
├── popup/
│   ├── popup.html
│   ├── popup.css
│   └── popup.js
└── tracker/
    ├── tracker.html
    ├── tracker.css
    └── tracker.js
```

## Install (dev)

1. `chrome://extensions` → enable Developer mode.
2. **Load unpacked** → select the `motion-cam/` folder.
3. Click the toolbar icon → **Open tracker**. The tracker opens in a new tab.
4. Press **Start camera**. Chrome will prompt for camera permission on this
   extension's origin. Grant it once and it's remembered.

## UI

- **Popup** — launches the tracker, holds the persisted sensitivity slider,
  and shows the count of stored motion events.
- **Tracker tab** — live camera feed with a motion overlay, an intensity
  meter (with a threshold marker that moves with the sensitivity slider), and
  an event log.

## How motion detection works

Every animation frame the tracker:

1. Draws the current `<video>` frame onto a downscaled 160×120 working
   canvas.
2. Compares each pixel's average brightness to the previous frame and counts
   pixels where the difference exceeds `PIXEL_DIFF` (25 / 255).
3. Computes a **motion score** = changed-pixels / total-pixels (i.e. the
   fraction of the frame that changed) and the bounding box of the changed
   pixels.
4. If the score exceeds the sensitivity-derived threshold, draws the bbox on
   an overlay canvas sized to the real video resolution, and starts (or
   extends) a motion event.
5. When the score stays below the threshold for `QUIET_FRAMES_TO_END` frames
   (~0.25 s at 60 fps), the event ends and is appended to storage.

Sensitivity slider → threshold mapping:
`threshold = 0.2 − (sensitivity / 100) × 0.18`
so sensitivity 1 triggers only on ~20% of pixels changing and sensitivity 100
triggers on ~2%.

The downscale to 160×120 is deliberate — per-pixel diff on a full 1280×720
frame every animation tick is both slow and noisier than it needs to be.

## Storage

All state lives in `chrome.storage.local`:

| Key | Shape | Written by |
| --- | --- | --- |
| `motionCamSensitivity` | `number` (1–100) | popup + tracker |
| `motionCamEvents` | `Array<{ time, durationMs, peakScore }>` | tracker |

The event list is capped at 50 entries (oldest dropped). `time` is a
`Date.now()` millisecond timestamp of when the motion started; `peakScore` is
the highest fraction of changed pixels seen during the event.

## Permissions

Only `storage` is declared in the manifest. Camera access is requested at
runtime via `getUserMedia`; Chrome scopes that grant to the extension's
origin, so the user is prompted once on first start. The extension has no
host permissions and does not open content scripts in pages.

## Caveats

- The tracker only runs while its tab is open and visible enough for the
  browser to keep serving `requestAnimationFrame`. If the tab is backgrounded
  for long periods, frame rate will drop to ~1 Hz or stop — this is a Chrome
  throttling behaviour, not a bug.
- Motion detection is a naive frame-diff. It reacts to lighting changes,
  auto-exposure, and camera noise, not just "real" movement. Raise
  sensitivity if false positives are an issue.
- The bounding box is the full extent of all changed pixels, not per-object —
  two things moving on opposite sides of the frame will show as one wide box.
