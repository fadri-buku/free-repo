# Motion Cam

A Chrome extension that uses your webcam to detect and log movement. The
tracker page shows the live camera feed with a bounding box around anything
that's moving, a motion intensity meter, and a log of timestamped motion
events. People counting is an optional add-on powered by MediaPipe Pose
Landmarker running entirely on-device.

## Folder

```
motion-cam/
├── manifest.json
├── setup.sh                       # one-shot downloader for pose assets
├── .gitignore                     # vendor/ and models/ are not committed
├── popup/
│   ├── popup.html
│   ├── popup.css
│   └── popup.js
├── tracker/
│   ├── tracker.html
│   ├── tracker.css
│   ├── tracker.js
│   └── pose.js                    # MediaPipe loader + helpers
├── vendor/mediapipe/              # created by setup.sh (WASM + ESM bundle)
└── models/                        # created by setup.sh (pose_landmarker_lite.task)
```

## Install (dev)

1. `chrome://extensions` → enable Developer mode.
2. **Load unpacked** → select the `motion-cam/` folder.
3. Click the toolbar icon → **Open tracker**. The tracker opens in a new tab.
4. Press **Start camera**. Chrome will prompt for camera permission on this
   extension's origin. Grant it once and it's remembered.

Motion detection works immediately. To also show a live **people count** and
per-person boxes:

```bash
cd motion-cam
./setup.sh           # downloads ~10 MB of WASM + model, one-time
```

Then reload the extension in `chrome://extensions`. If the files aren't
present, the tracker shows a banner and keeps working with motion-only.

## UI

- **Popup** — launches the tracker, holds the persisted sensitivity slider,
  and shows the count of stored motion events.
- **Tracker tab** — live camera feed with a motion overlay (pink boxes), a
  motion intensity meter with a threshold marker, and — when pose detection
  is enabled — a per-person overlay (blue boxes) plus a live people count
  chip in the header.

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

## How people counting works

When `vendor/mediapipe/` and `models/pose_landmarker_lite.task` are present,
`tracker/pose.js` dynamically imports the MediaPipe `vision_bundle.mjs` via
`chrome.runtime.getURL` and creates a `PoseLandmarker` in `VIDEO` running
mode with `numPoses: 6`. Each frame:

1. `detectForVideo(video, performance.now())` returns up to 6 sets of 33
   landmarks (normalized 0–1 coords) with per-landmark visibility.
2. `boundingBoxesFromPoses` drops landmarks with visibility < 0.3 and
   computes min/max x,y per pose, discarding poses with fewer than 3 visible
   landmarks.
3. The raw count is pushed through a 5-frame median smoothing window
   (`smoothCount`) before it's shown in the UI, so brief flicker doesn't
   jitter the displayed number.
4. The peak people count during a motion event is saved alongside the event
   (`peakPeople` field) and rendered in the log.

### Why it has to be vendored

MV3 disallows remote code. `setup.sh` downloads the MediaPipe ESM bundle,
its SIMD + non-SIMD WASM, and the Pose Landmarker Lite `.task` model
(~10 MB total) into `vendor/` and `models/`, both of which are gitignored.
WASM requires `'wasm-unsafe-eval'` in the `script-src` CSP — that's declared
in `manifest.json`'s `content_security_policy.extension_pages`.

### Graceful degradation

`loadPoseDetector` does a `HEAD` fetch on `vision_bundle.mjs` before
importing it. If the asset is missing it throws `missing-assets`, the
tracker hides the people chip, shows a banner with a link to a help dialog,
and continues running the motion detector as normal. A runtime failure
(e.g. GPU unavailable, out of memory) shows the underlying error instead.

## Storage

All state lives in `chrome.storage.local`:

| Key | Shape | Written by |
| --- | --- | --- |
| `motionCamSensitivity` | `number` (1–100) | popup + tracker |
| `motionCamEvents` | `Array<{ time, durationMs, peakScore, peakPeople? }>` | tracker |

The event list is capped at 50 entries (oldest dropped). `time` is a
`Date.now()` millisecond timestamp of when the motion started; `peakScore`
is the highest fraction of changed pixels seen during the event;
`peakPeople` is present only when pose detection was active.

## Permissions

Only `storage` is declared in the manifest. Camera access is requested at
runtime via `getUserMedia`; Chrome scopes that grant to the extension's
origin, so the user is prompted once on first start. The extension has no
host permissions and does not open content scripts in pages. WASM execution
is allowed via the `'wasm-unsafe-eval'` CSP directive scoped to extension
pages only.

## Caveats

- The tracker only runs while its tab is open and visible enough for the
  browser to keep serving `requestAnimationFrame`. If the tab is backgrounded
  for long periods, frame rate will drop to ~1 Hz or stop — this is a Chrome
  throttling behaviour, not a bug.
- Motion detection is a naive frame-diff. It reacts to lighting changes,
  auto-exposure, and camera noise, not just "real" movement. Raise
  sensitivity if false positives are an issue.
- The motion bounding box is the full extent of all changed pixels, not
  per-object — two things moving on opposite sides of the frame will show as
  one wide box. Use the pose overlay for per-person boxes.
- `numPoses` is a hard cap of 6. If you expect a larger crowd, raise
  `POSE_NUM` in `tracker.js` — but every additional pose costs frame time.
- Pose detection runs on the main thread via the MediaPipe GPU delegate.
  On low-end hardware you may see the live video drop below 15 fps; the
  motion meter will still update but feel less responsive.
