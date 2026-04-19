import {
  getEntries,
  parseWorklogText,
  mergeImport,
  entryToMarkdown
} from "../lib/entries-store.js";

const $ = (id) => document.getElementById(id);

let allEntries = [];

function formatDate(iso) {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso || "Unknown date";
  return d.toLocaleDateString(undefined, {
    weekday: "short", year: "numeric", month: "short", day: "numeric"
  }) + " " + d.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
}

function escapeHtml(s) {
  return (s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function escapeRe(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function highlight(text, query) {
  const safe = escapeHtml(text);
  if (!query) return safe;
  const re = new RegExp(`(${escapeRe(escapeHtml(query))})`, "gi");
  return safe.replace(re, "<mark>$1</mark>");
}

function sortNewestFirst(list) {
  return [...list].sort((a, b) => {
    const da = new Date(a.timestamp).getTime() || 0;
    const db = new Date(b.timestamp).getTime() || 0;
    return db - da;
  });
}

function setState(id) {
  for (const s of ["empty", "no-match"]) {
    $(`state-${s}`).hidden = s !== id;
  }
}

function setStatus(msg, kind) {
  const el = $("status");
  if (!msg) { el.hidden = true; el.textContent = ""; el.classList.remove("error"); return; }
  el.hidden = false;
  el.textContent = msg;
  el.classList.toggle("error", kind === "error");
}

function render(query = "") {
  const q = query.trim().toLowerCase();
  const list = $("entries");
  list.innerHTML = "";

  if (allEntries.length === 0) {
    setState("empty");
    $("count").textContent = "";
    return;
  }

  const filtered = q
    ? allEntries.filter(e =>
        (e.body || "").toLowerCase().includes(q) ||
        (e.title || "").toLowerCase().includes(q) ||
        formatDate(e.timestamp).toLowerCase().includes(q) ||
        (e.minutes ? `${e.minutes}m` : "").toLowerCase().includes(q)
      )
    : allEntries;

  if (filtered.length === 0) {
    setState("no-match");
    $("count").textContent = `0 of ${allEntries.length} entries`;
    return;
  }

  setState(null);
  $("count").textContent = q
    ? `${filtered.length} of ${allEntries.length} entries`
    : `${allEntries.length} entr${allEntries.length === 1 ? "y" : "ies"}`;

  for (const e of filtered) {
    const idx = allEntries.indexOf(e);
    const li = document.createElement("li");
    li.className = "entry";
    li.innerHTML = `
      <div class="entry-meta">
        <span class="entry-date">${formatDate(e.timestamp)}</span>
        ${e.minutes ? `<span class="entry-duration">${e.minutes}m</span>` : ""}
        <span class="entry-number">#${allEntries.length - idx}</span>
      </div>
      ${e.title ? `<div class="entry-title">${highlight(e.title, q)}</div>` : ""}
      <div class="entry-body">${highlight(e.body, q)}</div>`;
    list.appendChild(li);
  }
}

async function load() {
  const entries = await getEntries();
  allEntries = sortNewestFirst(entries);
  render($("search").value);
  $("subtitle").textContent = allEntries.length
    ? `${allEntries.length} entr${allEntries.length === 1 ? "y" : "ies"} stored in the extension`
    : "Stored in the extension";
}

async function importFromFile() {
  setStatus(null);
  if (!window.showOpenFilePicker) {
    setStatus("Your browser doesn't support the file picker.", "error");
    return;
  }
  try {
    const [handle] = await window.showOpenFilePicker({
      multiple: false,
      types: [
        { description: "Worklog", accept: { "text/plain": [".md", ".txt", ".log"] } }
      ]
    });
    const file = await handle.getFile();
    const text = await file.text();
    const parsed = parseWorklogText(text);
    if (parsed.length === 0) {
      setStatus("No worklog-style entries found in that file.", "error");
      return;
    }
    const { added, total } = await mergeImport(parsed);
    await load();
    setStatus(
      added === 0
        ? `No new entries — all ${parsed.length} already stored.`
        : `Imported ${added} new entr${added === 1 ? "y" : "ies"}. Total: ${total}.`
    );
  } catch (err) {
    if (err?.name !== "AbortError") setStatus(String(err?.message || err), "error");
  }
}

function exportAsMarkdown() {
  const md = sortNewestFirst(allEntries)
    .slice().reverse()  // oldest first on export
    .map(entryToMarkdown)
    .join("\n");
  const blob = new Blob([md || "# Worklog\n\n(no entries)\n"], { type: "text/markdown" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `worklog-${new Date().toISOString().slice(0, 10)}.md`;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

$("search").addEventListener("input", (e) => render(e.target.value));
$("import").addEventListener("click", importFromFile);
$("export").addEventListener("click", exportAsMarkdown);
$("open-settings").addEventListener("click", () => chrome.runtime.openOptionsPage());

// Reload if entries change in another context (e.g. after a save on locked page).
chrome.storage.onChanged.addListener((changes, area) => {
  if (area === "local" && changes.worklogEntries) load();
});

load();
