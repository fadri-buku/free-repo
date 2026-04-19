import { loadFileHandle } from "../lib/file-handle-store.js";

const $ = (id) => document.getElementById(id);
const RING_CIRC = 2 * Math.PI * 54;
let tickTimer = null;
let currentTotalMs = null;

function show(section) {
  for (const id of ["idle", "running", "awaiting"]) {
    $(id).hidden = id !== section;
  }
}

function showError(msg) {
  const el = $("error");
  if (!msg) { el.hidden = true; el.textContent = ""; return; }
  el.hidden = false;
  el.textContent = msg;
}

function formatRemaining(ms) {
  if (ms < 0) ms = 0;
  const total = Math.ceil(ms / 1000);
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

function setRingProgress(remaining, total) {
  const ratio = total > 0 ? Math.max(0, Math.min(1, remaining / total)) : 0;
  $("ring-progress").style.strokeDashoffset = String(RING_CIRC * (1 - ratio));
}

function startTick(endTime, totalMs) {
  stopTick();
  currentTotalMs = totalMs;
  const update = () => {
    const remaining = endTime - Date.now();
    $("remaining").textContent = formatRemaining(remaining);
    setRingProgress(remaining, totalMs);
    if (remaining <= 0) {
      stopTick();
      render();
    }
  };
  update();
  tickTimer = setInterval(update, 500);
}

function stopTick() {
  if (tickTimer) clearInterval(tickTimer);
  tickTimer = null;
}

async function startWith(minutes) {
  showError(null);
  if (!Number.isFinite(minutes) || minutes <= 0) {
    showError("Enter a positive number of minutes.");
    return;
  }
  const handle = await loadFileHandle().catch(() => null);
  if (!handle) {
    showError("Pick a worklog file in settings first.");
    return;
  }
  const res = await chrome.runtime.sendMessage({ type: "START_TIMER", minutes });
  if (!res?.ok) {
    showError(res?.error || "Failed to start timer.");
    return;
  }
  render();
}

async function render() {
  showError(null);
  const state = await chrome.runtime.sendMessage({ type: "GET_STATE" });
  if (state?.awaitingAck) {
    show("awaiting");
    return;
  }
  if (state?.running && state.endTime && state.endTime > Date.now()) {
    const totalMs = (state.minutes || 25) * 60 * 1000;
    $("running-total").textContent = `of ${state.minutes}m`;
    show("running");
    startTick(state.endTime, totalMs);
    return;
  }
  show("idle");
  const handle = await loadFileHandle().catch(() => null);
  if (handle) {
    $("file-status").textContent = `Worklog file: ${handle.name}`;
  } else {
    const { worklogPrefs } = await chrome.storage.local.get("worklogPrefs");
    const folder = { documents: "~/Documents", desktop: "~/Desktop", downloads: "~/Downloads" }[worklogPrefs?.folder] || "~/Documents";
    const name = (worklogPrefs?.name || "worklog.md").trim();
    $("file-status").textContent = `No file set — open settings to use default: ${folder}/${name}`;
  }
}

for (const btn of document.querySelectorAll(".preset")) {
  btn.addEventListener("click", () => startWith(Number(btn.dataset.minutes)));
}

$("start").addEventListener("click", () => startWith(Number($("minutes").value)));

$("minutes").addEventListener("keydown", (e) => {
  if (e.key === "Enter") startWith(Number($("minutes").value));
});

$("cancel").addEventListener("click", async () => {
  await chrome.runtime.sendMessage({ type: "CANCEL_TIMER" });
  render();
});

$("open-locked").addEventListener("click", async () => {
  const url = chrome.runtime.getURL("locked/locked.html");
  const [existing] = await chrome.tabs.query({ url });
  if (existing) {
    await chrome.tabs.update(existing.id, { active: true });
    if (existing.windowId != null) {
      try { await chrome.windows.update(existing.windowId, { focused: true }); } catch {}
    }
  } else {
    await chrome.tabs.create({ url, active: true });
  }
  window.close();
});

$("view-link").addEventListener("click", (e) => {
  e.preventDefault();
  const url = chrome.runtime.getURL("viewer/viewer.html");
  chrome.tabs.create({ url });
  window.close();
});

$("options-link").addEventListener("click", (e) => {
  e.preventDefault();
  chrome.runtime.openOptionsPage();
});

render();
