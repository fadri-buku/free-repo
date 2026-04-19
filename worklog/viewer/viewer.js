import { loadFileHandle, ensurePermission } from "../lib/file-handle-store.js";

const $ = (id) => document.getElementById(id);

let allEntries = [];
let currentHandle = null;

// ── parser ────────────────────────────────────────────────────────────────────

function parseEntries(raw) {
  const entries = [];
  // Each entry starts with a ## header: "## ISO_TIMESTAMP (Nm)" or "## ISO_TIMESTAMP"
  const sectionRe = /^## (.+?)(?:\s+\((\d+m)\))?\s*$/gm;
  let match;
  while ((match = sectionRe.exec(raw)) !== null) {
    const headerStart = match.index;
    const bodyStart = headerStart + match[0].length + 1; // skip newline
    const nextMatch = sectionRe.lastIndex;
    // Find where the next header begins (or end of string)
    const nextHeaderIdx = raw.indexOf("\n## ", bodyStart);
    const bodyEnd = nextHeaderIdx === -1 ? raw.length : nextHeaderIdx;
    const body = raw.slice(bodyStart, bodyEnd).trim();
    entries.push({
      raw: match[1].trim(),
      duration: match[2] || null,
      body,
      date: parseDate(match[1].trim())
    });
    sectionRe.lastIndex = nextMatch;
  }
  // Most recent first
  entries.sort((a, b) => (b.date?.getTime() || 0) - (a.date?.getTime() || 0));
  return entries;
}

function parseDate(s) {
  const d = new Date(s);
  return isNaN(d.getTime()) ? null : d;
}

function formatDate(date) {
  if (!date) return "Unknown date";
  return date.toLocaleDateString(undefined, {
    weekday: "short", year: "numeric", month: "short", day: "numeric"
  }) + " " + date.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
}

// ── highlight ─────────────────────────────────────────────────────────────────

function highlight(text, query) {
  if (!query) return escapeHtml(text);
  const escaped = escapeHtml(text);
  const re = new RegExp(`(${escapeRe(escapeHtml(query))})`, "gi");
  return escaped.replace(re, "<mark>$1</mark>");
}

function escapeHtml(s) {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function escapeRe(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

// ── render ────────────────────────────────────────────────────────────────────

function render(query = "") {
  const q = query.trim().toLowerCase();
  const list = $("entries");
  list.innerHTML = "";

  const filtered = q
    ? allEntries.filter(e =>
        e.body.toLowerCase().includes(q) ||
        formatDate(e.date).toLowerCase().includes(q) ||
        (e.duration || "").toLowerCase().includes(q)
      )
    : allEntries;

  const toolbar = $("toolbar");
  toolbar.hidden = allEntries.length === 0;

  setState(
    allEntries.length === 0 ? "no-entries" :
    filtered.length === 0 ? "no-match" :
    null
  );

  if (filtered.length > 0) {
    $("count").textContent = q
      ? `${filtered.length} of ${allEntries.length} entries`
      : `${allEntries.length} entr${allEntries.length === 1 ? "y" : "ies"}`;

    for (let i = 0; i < filtered.length; i++) {
      const e = filtered[i];
      const li = document.createElement("li");
      li.className = "entry";
      li.innerHTML = `
        <div class="entry-meta">
          <span class="entry-date">${formatDate(e.date)}</span>
          ${e.duration ? `<span class="entry-duration">${e.duration}</span>` : ""}
          <span class="entry-number">#${allEntries.length - allEntries.indexOf(e)}</span>
        </div>
        <div class="entry-body">${highlight(e.body, q)}</div>`;
      list.appendChild(li);
    }
  }
}

function setState(id) {
  for (const s of ["empty", "no-entries", "no-match", "error"]) {
    $(`state-${s}`).hidden = s !== id;
  }
}

// ── load ──────────────────────────────────────────────────────────────────────

async function load() {
  setState(null);
  $("toolbar").hidden = true;
  $("entries").innerHTML = "";
  $("file-name").textContent = "";

  const handle = await loadFileHandle().catch(() => null);
  if (!handle) {
    setState("empty");
    return;
  }
  currentHandle = handle;
  $("file-name").textContent = handle.name;

  const ok = await ensurePermission(handle, "readonly").catch(() => false);
  if (!ok) {
    $("error-msg").textContent = "Permission to read the worklog file was denied. Click retry to try again.";
    setState("error");
    return;
  }

  try {
    const file = await handle.getFile();
    const text = await file.text();
    allEntries = parseEntries(text);
    render($("search").value);
  } catch (err) {
    $("error-msg").textContent = `Could not read file: ${err?.message || err}`;
    setState("error");
  }
}

// ── events ────────────────────────────────────────────────────────────────────

$("search").addEventListener("input", (e) => render(e.target.value));
$("refresh").addEventListener("click", load);
$("retry").addEventListener("click", load);

$("go-settings").addEventListener("click", () => chrome.runtime.openOptionsPage());
$("open-settings").addEventListener("click", () => chrome.runtime.openOptionsPage());

// ── init ──────────────────────────────────────────────────────────────────────

load();
