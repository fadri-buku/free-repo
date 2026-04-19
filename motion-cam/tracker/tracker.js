const WORK_W = 160;
const WORK_H = 120;
const PIXEL_DIFF = 25;
const METER_MAX = 0.3;
const QUIET_FRAMES_TO_END = 15;
const MAX_EVENTS = 50;

const video = document.getElementById("video");
const overlay = document.getElementById("overlay");
const octx = overlay.getContext("2d");
const work = document.createElement("canvas");
work.width = WORK_W;
work.height = WORK_H;
const wctx = work.getContext("2d", { willReadFrequently: true });

const toggleBtn = document.getElementById("toggle");
const statusEl = document.getElementById("status");
const slider = document.getElementById("sensitivity");
const sliderVal = document.getElementById("sensitivity-val");
const meterFill = document.getElementById("meter-fill");
const meterThresh = document.getElementById("meter-threshold");
const logEl = document.getElementById("log");
const clearBtn = document.getElementById("clear");

let stream = null;
let rafId = 0;
let prev = null;
let currentEvent = null;
let sensitivity = 40;

function scoreThreshold() {
  return 0.2 - (sensitivity / 100) * 0.18;
}

function updateMeterThreshold() {
  meterThresh.style.left = (scoreThreshold() / METER_MAX) * 100 + "%";
}

slider.addEventListener("input", () => {
  sensitivity = Number(slider.value);
  sliderVal.textContent = String(sensitivity);
  updateMeterThreshold();
});

slider.addEventListener("change", async () => {
  await chrome.storage.local.set({ motionCamSensitivity: sensitivity });
});

chrome.storage.local
  .get(["motionCamSensitivity", "motionCamEvents"])
  .then((r) => {
    if (typeof r.motionCamSensitivity === "number") {
      sensitivity = r.motionCamSensitivity;
      slider.value = String(sensitivity);
      sliderVal.textContent = String(sensitivity);
    }
    updateMeterThreshold();
    renderLog(r.motionCamEvents ?? []);
  });

chrome.storage.onChanged.addListener((changes, area) => {
  if (area !== "local") return;
  if (changes.motionCamSensitivity?.newValue !== undefined) {
    sensitivity = changes.motionCamSensitivity.newValue;
    slider.value = String(sensitivity);
    sliderVal.textContent = String(sensitivity);
    updateMeterThreshold();
  }
  if (changes.motionCamEvents?.newValue) {
    renderLog(changes.motionCamEvents.newValue);
  }
});

function renderLog(events) {
  logEl.innerHTML = "";
  for (const e of [...events].reverse()) {
    const li = document.createElement("li");
    const d = new Date(e.time);
    const dur = (e.durationMs / 1000).toFixed(1);
    const peak = Math.round(e.peakScore * 100);
    li.textContent = `${d.toLocaleTimeString()} — ${dur}s (peak ${peak}%)`;
    logEl.appendChild(li);
  }
}

async function appendEvent(ev) {
  const r = await chrome.storage.local.get("motionCamEvents");
  const events = (r.motionCamEvents ?? []).concat(ev).slice(-MAX_EVENTS);
  await chrome.storage.local.set({ motionCamEvents: events });
}

toggleBtn.addEventListener("click", async () => {
  if (stream) stopCamera();
  else await startCamera();
});

clearBtn.addEventListener("click", async () => {
  await chrome.storage.local.set({ motionCamEvents: [] });
});

async function startCamera() {
  statusEl.textContent = "requesting camera…";
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: true,
      audio: false,
    });
    video.srcObject = stream;
    await video.play();
    overlay.width = video.videoWidth || 640;
    overlay.height = video.videoHeight || 480;
    toggleBtn.textContent = "Stop camera";
    statusEl.textContent = "live";
    statusEl.classList.add("active");
    prev = null;
    tick();
  } catch (e) {
    statusEl.textContent = `error: ${e.message}`;
    stream = null;
  }
}

function stopCamera() {
  if (rafId) cancelAnimationFrame(rafId);
  rafId = 0;
  stream?.getTracks().forEach((t) => t.stop());
  stream = null;
  video.srcObject = null;
  octx.clearRect(0, 0, overlay.width, overlay.height);
  toggleBtn.textContent = "Start camera";
  statusEl.textContent = "idle";
  statusEl.classList.remove("active");
  prev = null;
  if (currentEvent) endEvent();
  meterFill.style.width = "0%";
}

function tick() {
  rafId = requestAnimationFrame(tick);
  if (video.readyState < 2) return;
  wctx.drawImage(video, 0, 0, WORK_W, WORK_H);
  const cur = wctx.getImageData(0, 0, WORK_W, WORK_H);
  if (prev) analyze(prev, cur);
  prev = cur;
}

function analyze(prevImg, curImg) {
  const a = prevImg.data;
  const b = curImg.data;
  let count = 0;
  let minX = WORK_W;
  let minY = WORK_H;
  let maxX = -1;
  let maxY = -1;

  for (let y = 0; y < WORK_H; y++) {
    for (let x = 0; x < WORK_W; x++) {
      const i = (y * WORK_W + x) * 4;
      const pa = (a[i] + a[i + 1] + a[i + 2]) / 3;
      const pb = (b[i] + b[i + 1] + b[i + 2]) / 3;
      if (Math.abs(pa - pb) > PIXEL_DIFF) {
        count++;
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
      }
    }
  }

  const score = count / (WORK_W * WORK_H);
  const threshold = scoreThreshold();
  meterFill.style.width =
    Math.min(100, (score / METER_MAX) * 100).toFixed(1) + "%";

  octx.clearRect(0, 0, overlay.width, overlay.height);

  if (score > threshold && maxX >= 0) {
    const sx = overlay.width / WORK_W;
    const sy = overlay.height / WORK_H;
    const bx = minX * sx;
    const by = minY * sy;
    const bw = (maxX - minX + 1) * sx;
    const bh = (maxY - minY + 1) * sy;

    octx.strokeStyle = "rgba(224, 71, 107, 0.9)";
    octx.lineWidth = 3;
    octx.strokeRect(bx, by, bw, bh);
    octx.font = "14px system-ui, sans-serif";
    octx.fillStyle = "rgba(224, 71, 107, 0.95)";
    octx.fillText(`motion ${Math.round(score * 100)}%`, bx + 4, Math.max(14, by - 6));

    if (!currentEvent) {
      currentEvent = {
        startedAt: Date.now(),
        peakScore: score,
        quietFrames: 0,
      };
    } else {
      currentEvent.peakScore = Math.max(currentEvent.peakScore, score);
      currentEvent.quietFrames = 0;
    }
  } else if (currentEvent) {
    currentEvent.quietFrames++;
    if (currentEvent.quietFrames > QUIET_FRAMES_TO_END) {
      endEvent();
    }
  }
}

function endEvent() {
  const e = currentEvent;
  currentEvent = null;
  if (!e) return;
  appendEvent({
    time: e.startedAt,
    durationMs: Math.max(100, Date.now() - e.startedAt),
    peakScore: e.peakScore,
  });
}

window.addEventListener("beforeunload", () => {
  stream?.getTracks().forEach((t) => t.stop());
});
