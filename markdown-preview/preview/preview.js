import { renderMarkdown } from "../lib/render.js";
import { renderDiagramsInElement } from "../lib/diagram-render.js";
import {
  saveFileHandle, loadFileHandle, clearFileHandle,
  ensurePermission, readFile,
} from "../lib/file-handle-store.js";

const DRAFT_KEY = "markdownDraft";
const RENDER_DEBOUNCE_MS = 80;

const $ = (id) => document.getElementById(id);
const sourceEl = $("source");
const renderedEl = $("rendered");
const errorEl = $("error");
const fileNameEl = $("file-name");
const openBtn = $("open-file");
const reloadBtn = $("reload-file");
const clearBtn = $("clear-file");
const syncScrollEl = $("sync-scroll");

let currentHandle = null;
let renderTimer = null;

function showError(msg) {
  if (!msg) { errorEl.hidden = true; errorEl.textContent = ""; return; }
  errorEl.hidden = false;
  errorEl.textContent = msg;
}

let renderToken = 0;
function renderNow() {
  renderedEl.innerHTML = renderMarkdown(sourceEl.value);
  const token = ++renderToken;
  // Upgrade fenced mermaid/nomnoml blocks to SVG. Token guards against a
  // slow diagram render clobbering a newer DOM if the user kept typing.
  renderDiagramsInElement(renderedEl).then(() => {
    if (token !== renderToken) return;
  }).catch((err) => showError(err.message));
}

function scheduleRender() {
  clearTimeout(renderTimer);
  renderTimer = setTimeout(renderNow, RENDER_DEBOUNCE_MS);
}

function setFileUi(name) {
  if (name) {
    fileNameEl.textContent = name;
    reloadBtn.hidden = false;
    clearBtn.hidden = false;
  } else {
    fileNameEl.textContent = "";
    reloadBtn.hidden = true;
    clearBtn.hidden = true;
  }
}

async function loadFromHandle(handle, { userGesture = false } = {}) {
  try {
    const ok = await ensurePermission(handle, "read");
    if (!ok) {
      if (userGesture) showError("Permission to read the file was denied.");
      return false;
    }
    const text = await readFile(handle);
    sourceEl.value = text;
    renderNow();
    setFileUi(handle.name);
    showError(null);
    return true;
  } catch (err) {
    showError(`Couldn't read file: ${err.message}`);
    return false;
  }
}

// ── Source edits ────────────────────────────────────────────────────────────
sourceEl.addEventListener("input", () => {
  scheduleRender();
  chrome.storage.local.set({ [DRAFT_KEY]: sourceEl.value });
});

// ── File open ───────────────────────────────────────────────────────────────
openBtn.addEventListener("click", async () => {
  if (!window.showOpenFilePicker) {
    showError("Your browser doesn't support the File System Access API.");
    return;
  }
  try {
    const [handle] = await window.showOpenFilePicker({
      types: [{
        description: "Markdown / text",
        accept: { "text/markdown": [".md", ".markdown"], "text/plain": [".txt"] },
      }],
      multiple: false,
    });
    currentHandle = handle;
    await saveFileHandle(handle);
    await loadFromHandle(handle, { userGesture: true });
  } catch (err) {
    if (err.name !== "AbortError") showError(err.message);
  }
});

reloadBtn.addEventListener("click", async () => {
  if (!currentHandle) return;
  await loadFromHandle(currentHandle, { userGesture: true });
});

clearBtn.addEventListener("click", async () => {
  currentHandle = null;
  await clearFileHandle();
  setFileUi(null);
});

// ── Scroll sync: naive proportional mapping, good enough for a preview ─────
const editor = sourceEl;
const preview = renderedEl.parentElement;
let syncing = false;
function mirror(from, to) {
  if (!syncScrollEl.checked || syncing) return;
  syncing = true;
  const ratio = from.scrollTop / Math.max(1, from.scrollHeight - from.clientHeight);
  to.scrollTop = ratio * Math.max(0, to.scrollHeight - to.clientHeight);
  requestAnimationFrame(() => { syncing = false; });
}
editor.addEventListener("scroll", () => mirror(editor, preview));
preview.addEventListener("scroll", () => mirror(preview, editor));

// ── Startup ─────────────────────────────────────────────────────────────────
(async function init() {
  const { [DRAFT_KEY]: draft } = await chrome.storage.local.get(DRAFT_KEY);
  if (typeof draft === "string") sourceEl.value = draft;

  const handle = await loadFileHandle();
  if (handle) {
    currentHandle = handle;
    setFileUi(handle.name);
    // Don't auto-read — Chrome requires a user gesture for requestPermission
    // after a restart. User clicks Reload to re-read.
  }

  renderNow();
})();
