import {
  saveFileHandle,
  loadFileHandle,
  clearFileHandle,
  ensurePermission
} from "../lib/file-handle-store.js";

const $ = (id) => document.getElementById(id);

function setStatus({ error, success } = {}) {
  const e = $("error"), s = $("success");
  e.hidden = !error; e.textContent = error || "";
  s.hidden = !success; s.textContent = success || "";
}

async function render() {
  setStatus();
  const handle = await loadFileHandle().catch(() => null);
  $("current").textContent = handle
    ? `Selected file: ${handle.name}`
    : "No file selected.";
}

async function pickExisting() {
  if (!window.showOpenFilePicker) {
    setStatus({ error: "Your browser doesn't support the File System Access API." });
    return;
  }
  try {
    const [handle] = await window.showOpenFilePicker({
      multiple: false,
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

async function pickNew() {
  if (!window.showSaveFilePicker) {
    setStatus({ error: "Your browser doesn't support the File System Access API." });
    return;
  }
  try {
    const handle = await window.showSaveFilePicker({
      suggestedName: "worklog.md",
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
$("pick-new").addEventListener("click", pickNew);
$("clear").addEventListener("click", async () => {
  await clearFileHandle();
  await render();
  setStatus({ success: "Cleared." });
});

render();
