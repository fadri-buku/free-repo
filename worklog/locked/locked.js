import {
  loadFileHandle,
  ensurePermission,
  appendToFile
} from "../lib/file-handle-store.js";

const $ = (id) => document.getElementById(id);
const audio = $("alarm-audio");
const indicator = $("alarm-indicator");

let fallbackCtx = null;
let fallbackTimer = null;

async function playAudio() {
  try {
    audio.currentTime = 0;
    await audio.play();
    return true;
  } catch {
    return false;
  }
}

function startFallbackBeep() {
  try {
    fallbackCtx = new (window.AudioContext || window.webkitAudioContext)();
  } catch {
    return;
  }
  // Soft triad chime (C5, E5, G5) as a loop. Each tick plays the full arpeggio.
  const chime = () => {
    if (!fallbackCtx) return;
    const now = fallbackCtx.currentTime;
    const vol = Number($("volume").value) / 100 * 0.25;
    const notes = [523.25, 659.25, 783.99];
    notes.forEach((freq, i) => {
      const start = now + i * 0.22;
      const osc = fallbackCtx.createOscillator();
      const gain = fallbackCtx.createGain();
      osc.type = "sine";
      osc.frequency.setValueAtTime(freq, start);
      gain.gain.setValueAtTime(0.0001, start);
      gain.gain.exponentialRampToValueAtTime(Math.max(0.0001, vol), start + 0.015);
      gain.gain.exponentialRampToValueAtTime(0.0001, start + 1.2);
      osc.connect(gain).connect(fallbackCtx.destination);
      osc.start(start);
      osc.stop(start + 1.3);
    });
  };
  chime();
  fallbackTimer = setInterval(chime, 2500);
}

async function startAlarm() {
  audio.volume = Number($("volume").value) / 100;
  const ok = await playAudio();
  if (!ok) startFallbackBeep();
}

function stopAlarm() {
  audio.pause();
  audio.currentTime = 0;
  if (fallbackTimer) clearInterval(fallbackTimer);
  fallbackTimer = null;
  if (fallbackCtx) {
    try { fallbackCtx.close(); } catch {}
    fallbackCtx = null;
  }
  indicator.classList.add("muted");
}

function flashTitle() {
  const orig = document.title;
  let on = false;
  setInterval(() => {
    on = !on;
    document.title = on ? "\u23F0 Worklog \u2014 time's up" : orig;
  }, 800);
}

function showError(msg) {
  const el = $("error");
  if (!msg) { el.hidden = true; el.textContent = ""; return; }
  el.hidden = false;
  el.textContent = msg;
}

async function init() {
  flashTitle();
  await startAlarm();

  // If autoplay was blocked, resume on first user interaction.
  document.addEventListener("click", async function onClick() {
    if (audio.paused && !fallbackTimer) {
      await startAlarm();
    }
    document.removeEventListener("click", onClick);
  }, { once: true });

  const state = await chrome.runtime.sendMessage({ type: "GET_STATE" }).catch(() => null);
  if (state?.title) {
    const el = $("session-title");
    el.textContent = state.title;
    el.hidden = false;
    $("entry").placeholder = `What did you accomplish on "${state.title}"?`;
  }

  const handle = await loadFileHandle().catch(() => null);
  $("file-info").textContent = handle
    ? `Will append to: ${handle.name}`
    : "No worklog file set. Open settings to pick one \u2014 your entry won't save until you do.";
}

$("volume").addEventListener("input", (e) => {
  audio.volume = Number(e.target.value) / 100;
});

$("mute").addEventListener("click", () => {
  stopAlarm();
  $("mute").disabled = true;
  $("mute-label").textContent = "Alarm muted";
});

$("save").addEventListener("click", async () => {
  showError(null);
  const text = $("entry").value.trim();
  if (!text) {
    showError("Write something before saving.");
    return;
  }
  const handle = await loadFileHandle().catch(() => null);
  if (!handle) {
    showError("No worklog file configured. Open the extension options to pick one.");
    return;
  }
  const ok = await ensurePermission(handle, "readwrite").catch(() => false);
  if (!ok) {
    showError("Permission to write the worklog file was denied.");
    return;
  }

  const state = await chrome.runtime.sendMessage({ type: "GET_STATE" });
  const when = new Date().toISOString();
  const duration = state?.minutes ? `(${state.minutes}m)` : "";
  const titlePart = state?.title ? ` — ${state.title}` : "";
  const header = `## ${when}${duration ? ` ${duration}` : ""}${titlePart}`;
  const entry = `${header}\n${text}\n`;

  try {
    await appendToFile(handle, entry);
  } catch (err) {
    showError(`Failed to write file: ${err?.message || err}`);
    return;
  }

  stopAlarm();
  await chrome.runtime.sendMessage({ type: "ACKNOWLEDGED" });
  try {
    const tab = await chrome.tabs.getCurrent();
    if (tab?.id != null) await chrome.tabs.remove(tab.id);
    else window.close();
  } catch {
    window.close();
  }
});

init();
