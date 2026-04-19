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
  const beep = () => {
    if (!fallbackCtx) return;
    const now = fallbackCtx.currentTime;
    const osc = fallbackCtx.createOscillator();
    const gain = fallbackCtx.createGain();
    osc.type = "sine";
    osc.frequency.setValueAtTime(880, now);
    const vol = Number($("volume").value) / 100 * 0.3;
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.exponentialRampToValueAtTime(Math.max(0.0001, vol), now + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.32);
    osc.connect(gain).connect(fallbackCtx.destination);
    osc.start(now);
    osc.stop(now + 0.4);
  };
  beep();
  fallbackTimer = setInterval(beep, 900);
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
  const duration = state?.minutes ? `${state.minutes}m` : "";
  const header = duration ? `## ${when} (${duration})` : `## ${when}`;
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
