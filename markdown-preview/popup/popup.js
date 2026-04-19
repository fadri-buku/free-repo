import { loadFileHandle } from "../lib/file-handle-store.js";

const openBtn = document.getElementById("open-preview");
const statusEl = document.getElementById("file-status");

openBtn.addEventListener("click", async () => {
  const url = chrome.runtime.getURL("preview/preview.html");
  await chrome.tabs.create({ url });
  window.close();
});

(async function init() {
  try {
    const handle = await loadFileHandle();
    statusEl.textContent = handle ? `Last opened: ${handle.name}` : "";
  } catch {
    // Ignore — the preview page will handle any real errors.
  }
})();
