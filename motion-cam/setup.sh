#!/usr/bin/env bash
# Download MediaPipe Tasks Vision runtime + Pose Landmarker Lite model.
# Run once from the motion-cam/ folder. Re-run to refresh.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENDOR_DIR="$HERE/vendor/mediapipe"
MODEL_DIR="$HERE/models"

# Pin the MediaPipe tasks-vision version. Bump here if you want a newer one.
TASKS_VERSION="0.10.14"
TASKS_ROOT="https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@${TASKS_VERSION}"

MODEL_URL="https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task"

mkdir -p "$VENDOR_DIR" "$MODEL_DIR"

fetch() {
  local url="$1"
  local dest="$2"
  echo "  $url"
  curl -sSfL --retry 3 --retry-delay 2 -o "$dest" "$url"
}

# The ESM entry lives at the package root; the WASM files live under /wasm/.
# We flatten everything into vendor/mediapipe/ so FilesetResolver.forVisionTasks
# only needs one path.
echo "Downloading MediaPipe tasks-vision ${TASKS_VERSION} into vendor/mediapipe/..."
fetch "${TASKS_ROOT}/vision_bundle.mjs" "$VENDOR_DIR/vision_bundle.mjs"
for f in vision_wasm_internal.js \
         vision_wasm_internal.wasm \
         vision_wasm_nosimd_internal.js \
         vision_wasm_nosimd_internal.wasm; do
  fetch "${TASKS_ROOT}/wasm/${f}" "$VENDOR_DIR/${f}"
done

echo "Downloading Pose Landmarker Lite model into models/..."
fetch "$MODEL_URL" "$MODEL_DIR/pose_landmarker_lite.task"

echo
echo "Done. Reload the extension in chrome://extensions to pick up the new files."
