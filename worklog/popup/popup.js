import { loadFileHandle } from "../lib/file-handle-store.js";

const $ = (id) => document.getElementById(id);
let tickTimer = null;

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

function startTick(endTime) {
  stopTick();
  const update = () => {
    const remaining = endTime - Date.now();
    $("remaining").textContent = formatRemaining(remaining);
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

async function render() {
  showError(null);
  const state = await chrome.runtime.sendMessage({ type: "GET_STATE" });
  if (state?.awaitingAck) {
    show("awaiting");
    return;
  }
  if (state?.running && state.endTime && state.endTime > Date.now()) {
    show("running");
    startTick(state.endTime);
    return;
  }
  show("idle");
  const handle = await loadFileHandle().catch(() => null);
  $("file-status").textContent = handle
    ? `Worklog file: ${handle.name}`
    : "No worklog file set \u2014 configure one first.";
}

$("start").addEventListener("click", async () => {
  showError(null);
  const minutes = Number($("minutes").value);
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

$("options-link").addEventListener("click", (e) => {
  e.preventDefault();
  chrome.runtime.openOptionsPage();
});

render();
