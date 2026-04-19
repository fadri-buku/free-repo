import { loadFileHandle } from "../lib/file-handle-store.js";

const $ = (id) => document.getElementById(id);
const RING_CIRC = 2 * Math.PI * 54;
let tickTimer = null;

// ── Helpers ───────────────────────────────────────────────────────────────

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

function fmt(ms) {
  if (ms < 0) ms = 0;
  const total = Math.ceil(ms / 1000);
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

function setRing(remaining, total, isBreak) {
  const ratio = total > 0 ? Math.max(0, Math.min(1, remaining / total)) : 0;
  $("ring-progress").style.strokeDashoffset = String(RING_CIRC * (1 - ratio));
  $("ring-svg").classList.toggle("ring--break", isBreak);
}

function renderDots(cycles, isBreak) {
  const filled   = cycles % 4;
  const header   = $("session-header");
  const dotsEl   = $("cycle-dots");
  header.classList.toggle("break-mode", isBreak);
  dotsEl.innerHTML = Array.from({ length: 4 }, (_, i) =>
    `<span class="dot${i < filled ? " filled" : ""}" title="Session ${i + 1}"></span>`
  ).join("");
}

// ── Tick ──────────────────────────────────────────────────────────────────

function startTick(state) {
  stopTick();
  const { endTime, minutes, paused, remainingMs, phase } = state;
  const totalMs = (minutes || 25) * 60 * 1000;
  const isBreak = phase !== "work";

  const update = () => {
    const remaining = paused ? remainingMs : Math.max(0, endTime - Date.now());
    $("remaining").textContent = fmt(remaining);
    setRing(remaining, totalMs, isBreak);
    if (!paused && remaining <= 0) { stopTick(); render(); }
  };
  update();
  if (!paused) tickTimer = setInterval(update, 500);
}

function stopTick() {
  if (tickTimer) clearInterval(tickTimer);
  tickTimer = null;
}

// ── Render ────────────────────────────────────────────────────────────────

async function render() {
  showError(null);
  const state = await chrome.runtime.sendMessage({ type: "GET_STATE" });
  const cycles = state?.cycles ?? 0;
  const phase  = state?.phase;
  const active = state?.running || state?.paused;

  if (state?.awaitingAck) {
    show("awaiting");
    return;
  }

  if (active && phase) {
    const isBreak  = phase !== "work";
    const isPaused = !!state.paused;

    // Phase label
    const labels = { work: isPaused ? "Paused" : "Focus", break: "Short break", "long-break": "Long break" };
    $("phase-label").textContent = labels[phase] || "";

    // Cycle dots
    renderDots(cycles, isBreak);

    // Session title (work only)
    const titleEl = $("running-title");
    titleEl.hidden  = isBreak || !state.title;
    titleEl.textContent = state.title || "";

    // Total label
    $("running-total").textContent = isBreak ? "" : `of ${state.minutes}m`;

    // Buttons
    $("pause-resume").textContent = isPaused ? "Resume" : "Pause";
    $("cancel-skip").textContent  = isBreak  ? "Skip break" : "Cancel";

    $("silent-badge").hidden = !state.silent;

    show("running");
    startTick(state);
    return;
  }

  // Idle
  stopTick();
  show("idle");

  // Restore silent-mode preference
  const { worklogSilent } = await chrome.storage.local.get("worklogSilent");
  $("silent-mode").checked = !!worklogSilent;

  // Cycle status hint
  const sessionInCycle = (cycles % 4) + 1;
  const next = sessionInCycle < 4 ? "short break follows" : "long break follows";
  $("cycle-status").textContent = cycles > 0
    ? `Session ${sessionInCycle} of 4 — ${next}`
    : "";

  const handle = await loadFileHandle().catch(() => null);
  if (handle) {
    $("file-status").textContent = `Worklog: ${handle.name}`;
  } else {
    const { worklogPrefs } = await chrome.storage.local.get("worklogPrefs");
    const folder = { documents: "~/Documents", desktop: "~/Desktop", downloads: "~/Downloads" }[worklogPrefs?.folder] || "~/Documents";
    const name = (worklogPrefs?.name || "worklog.md").trim();
    $("file-status").textContent = `No file set — default: ${folder}/${name}`;
  }
}

// ── Start ─────────────────────────────────────────────────────────────────

async function startWith(minutes) {
  showError(null);
  if (!Number.isFinite(minutes) || minutes <= 0) {
    showError("Enter a positive number of minutes."); return;
  }
  const handle = await loadFileHandle().catch(() => null);
  if (!handle) { showError("Pick a worklog file in settings first."); return; }
  const title  = $("title").value.trim();
  const silent = $("silent-mode").checked;
  await chrome.storage.local.set({ worklogSilent: silent });
  const res = await chrome.runtime.sendMessage({ type: "START_TIMER", minutes, title, silent });
  if (!res?.ok) { showError(res?.error || "Failed to start timer."); return; }
  render();
}

// ── Events ────────────────────────────────────────────────────────────────

for (const btn of document.querySelectorAll(".preset")) {
  btn.addEventListener("click", () => startWith(Number(btn.dataset.minutes)));
}

$("start").addEventListener("click", () => startWith(Number($("minutes").value)));
$("minutes").addEventListener("keydown", (e) => { if (e.key === "Enter") startWith(Number($("minutes").value)); });

$("pause-resume").addEventListener("click", async () => {
  const state = await chrome.runtime.sendMessage({ type: "GET_STATE" });
  const msg = state?.paused ? "RESUME_TIMER" : "PAUSE_TIMER";
  await chrome.runtime.sendMessage({ type: msg });
  render();
});

$("cancel-skip").addEventListener("click", async () => {
  const state = await chrome.runtime.sendMessage({ type: "GET_STATE" });
  const msg = state?.phase !== "work" ? "SKIP_BREAK" : "CANCEL_TIMER";
  await chrome.runtime.sendMessage({ type: msg });
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
  chrome.tabs.create({ url: chrome.runtime.getURL("viewer/viewer.html") });
  window.close();
});

$("options-link").addEventListener("click", (e) => {
  e.preventDefault();
  chrome.runtime.openOptionsPage();
});

render();
