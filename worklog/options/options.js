import {
  saveFileHandle,
  loadFileHandle,
  clearFileHandle,
  ensurePermission
} from "../lib/file-handle-store.js";

const $ = (id) => document.getElementById(id);
const audio = $("alarm-audio");

const FOLDER_LABELS = {
  documents: "~/Documents",
  desktop: "~/Desktop",
  downloads: "~/Downloads"
};
const DEFAULT_PREFS = { folder: "documents", name: "worklog.md" };

function setStatus({ error, success } = {}) {
  const e = $("error"), s = $("success");
  e.hidden = !error; e.textContent = error || "";
  s.hidden = !success; s.textContent = success || "";
}

async function loadPrefs() {
  const { worklogPrefs } = await chrome.storage.local.get("worklogPrefs");
  return { ...DEFAULT_PREFS, ...(worklogPrefs || {}) };
}

async function savePrefs(patch) {
  const current = await loadPrefs();
  const next = { ...current, ...patch };
  await chrome.storage.local.set({ worklogPrefs: next });
  return next;
}

function sanitizeName(name) {
  const trimmed = (name || "").trim();
  if (!trimmed) return "worklog.md";
  if (!/\.[a-z0-9]+$/i.test(trimmed)) return `${trimmed}.md`;
  return trimmed;
}

function formatDefaultPath(prefs) {
  return `${FOLDER_LABELS[prefs.folder] || "~/Documents"}/${sanitizeName(prefs.name)}`;
}

async function render() {
  setStatus();
  const [handle, prefs] = await Promise.all([
    loadFileHandle().catch(() => null),
    loadPrefs()
  ]);

  $("default-folder").value = prefs.folder;
  $("default-name").value = prefs.name;
  $("default-preview").textContent = `Default target: ${formatDefaultPath(prefs)}`;

  $("current").textContent = handle
    ? `Selected file: ${handle.name}`
    : `No file selected. Default target: ${formatDefaultPath(prefs)}`;
}

async function pickExisting() {
  if (!window.showOpenFilePicker) {
    setStatus({ error: "Your browser doesn't support the File System Access API." });
    return;
  }
  try {
    const prefs = await loadPrefs();
    const [handle] = await window.showOpenFilePicker({
      multiple: false,
      startIn: prefs.folder,
      types: [
        { description: "Text/Markdown", accept: { "text/plain": [".txt", ".md", ".log"] } }
      ]
    });
    if (!(await ensurePermission(handle, "readwrite"))) {
      setStatus({ error: "Read/write permission was not granted." });
      return;
    }
    await saveFileHandle(handle);
    await render();
    setStatus({ success: `Saved. Entries will be appended to ${handle.name}.` });
  } catch (err) {
    if (err?.name !== "AbortError") setStatus({ error: String(err?.message || err) });
  }
}

async function pickNewAt(prefs) {
  if (!window.showSaveFilePicker) {
    setStatus({ error: "Your browser doesn't support the File System Access API." });
    return;
  }
  try {
    const handle = await window.showSaveFilePicker({
      suggestedName: sanitizeName(prefs.name),
      startIn: prefs.folder,
      types: [
        { description: "Markdown", accept: { "text/markdown": [".md"] } },
        { description: "Text", accept: { "text/plain": [".txt", ".log"] } }
      ]
    });
    if (!(await ensurePermission(handle, "readwrite"))) {
      setStatus({ error: "Read/write permission was not granted." });
      return;
    }
    await saveFileHandle(handle);
    await render();
    setStatus({ success: `Saved. Entries will be appended to ${handle.name}.` });
  } catch (err) {
    if (err?.name !== "AbortError") setStatus({ error: String(err?.message || err) });
  }
}

$("pick").addEventListener("click", pickExisting);
$("pick-new").addEventListener("click", async () => pickNewAt(await loadPrefs()));
$("use-default").addEventListener("click", async () => pickNewAt(await loadPrefs()));

$("clear").addEventListener("click", async () => {
  await clearFileHandle();
  await render();
  setStatus({ success: "Cleared." });
});

$("default-folder").addEventListener("change", async (e) => {
  await savePrefs({ folder: e.target.value });
  await render();
});

$("default-name").addEventListener("change", async (e) => {
  await savePrefs({ name: e.target.value });
  await render();
});

$("volume").addEventListener("input", (e) => {
  audio.volume = Number(e.target.value) / 100;
});

$("test-alarm").addEventListener("click", async () => {
  audio.volume = Number($("volume").value) / 100;
  try {
    audio.currentTime = 0;
    await audio.play();
    $("stop-alarm").disabled = false;
  } catch (err) {
    setStatus({ error: `Couldn't play alarm: ${err?.message || err}` });
  }
});

$("stop-alarm").addEventListener("click", () => {
  audio.pause();
  audio.currentTime = 0;
  $("stop-alarm").disabled = true;
});

audio.addEventListener("ended", () => { $("stop-alarm").disabled = true; });

render();
