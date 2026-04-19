const BUNDLE_PATH = "vendor/mediapipe/vision_bundle.mjs";
const WASM_DIR = "vendor/mediapipe";
const MODEL_PATH = "models/pose_landmarker_lite.task";

export async function loadPoseDetector({ numPoses = 6 } = {}) {
  const bundleUrl = chrome.runtime.getURL(BUNDLE_PATH);
  const wasmDir = chrome.runtime.getURL(WASM_DIR);
  const modelUrl = chrome.runtime.getURL(MODEL_PATH);

  const head = await fetch(bundleUrl, { method: "HEAD" }).catch(() => null);
  if (!head || !head.ok) {
    throw new Error("missing-assets");
  }

  const mod = await import(bundleUrl);
  const { PoseLandmarker, FilesetResolver } = mod;

  const fileset = await FilesetResolver.forVisionTasks(wasmDir);
  return await PoseLandmarker.createFromOptions(fileset, {
    baseOptions: { modelAssetPath: modelUrl, delegate: "GPU" },
    runningMode: "VIDEO",
    numPoses,
  });
}

export function boundingBoxesFromPoses(posesLandmarks) {
  const boxes = [];
  for (const landmarks of posesLandmarks) {
    let minX = 1;
    let minY = 1;
    let maxX = 0;
    let maxY = 0;
    let visible = 0;
    for (const lm of landmarks) {
      if ((lm.visibility ?? 1) < 0.3) continue;
      visible++;
      if (lm.x < minX) minX = lm.x;
      if (lm.y < minY) minY = lm.y;
      if (lm.x > maxX) maxX = lm.x;
      if (lm.y > maxY) maxY = lm.y;
    }
    if (visible < 3) continue;
    boxes.push({ minX, minY, maxX, maxY });
  }
  return boxes;
}

export function smoothCount(history, next, windowSize = 5) {
  history.push(next);
  while (history.length > windowSize) history.shift();
  const sorted = [...history].sort((a, b) => a - b);
  return sorted[Math.floor(sorted.length / 2)];
}
