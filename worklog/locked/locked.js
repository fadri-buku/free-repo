import {
  loadFileHandle,
  ensurePermission,
  appendToFile
} from "../lib/file-handle-store.js";

const $ = (id) => document.getElementById(id);

let audioCtx = null;
let alarmTimer = null;

function startAlarm() {
  try {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
  } catch {
    return;
  }
  const beep = () => {
    const now = audioCtx.currentTime;
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = "sine";
    osc.frequency.setValueAtTime(880, now);
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.exponentialRampToValueAtTime(0.25, now + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.35);
    osc.connect(gain).connect(audioCtx.destination);
    osc.start(now);
    osc.stop(now + 0.4);
  };
  beep();
  alarmTimer = setInterval(beep, 900);
}

function stopAlarm() {
  if (alarmTimer) clearInterval(alarmTimer);
  alarmTimer = null;
  if (audioCtx) {
    try { audioCtx.close(); } catch {}
    audioCtx = null;
  }
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
  // Autoplay in extension pages is permitted; fall back to user gesture if blocked.
  startAlarm();
  document.addEventListener("visibilitychange", () => {
    // Keep alarm going until acknowledged; nothing to do here.
  });

  const handle = await loadFileHandle().catch(() => null);
  $("file-info").textContent = handle
    ? `Will append to: ${handle.name}`
    : "No worklog file set. Open settings to pick one \u2014 your entry won't save until you do.";
}

$("mute").addEventListener("click", () => {
  stopAlarm();
  $("mute").disabled = true;
  $("mute").textContent = "Alarm muted";
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
