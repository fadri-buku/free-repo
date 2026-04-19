import { loadFileHandle } from "../lib/file-handle-store.js";

const $ = (id) => document.getElementById(id);

let handle = null;
let rawText = "";
let allEntries = [];

// Auto-refresh when the locked page broadcasts a save.
const channel = new BroadcastChannel("worklog");
channel.addEventListener("message", (e) => {
  if (e.data?.type === "saved") readAndRender();
});

// ── Parser ────────────────────────────────────────────────────────────────

function parseEntries(raw) {
  const entries = [];
  const re = /^## (.+?)(?:\s+\((\d+m)\))?(?:\s+—\s+(.+))?\s*$/gm;
  let m;
  while ((m = re.exec(raw)) !== null) {
    const bodyStart = m.index + m[0].length + 1;
    const nextIdx = raw.indexOf("\n## ", bodyStart);
    const body = raw.slice(bodyStart, nextIdx === -1 ? raw.length : nextIdx).trim();
    const d = new Date(m[1].trim());
    entries.push({
      timestamp: m[1].trim(),
      date: isNaN(d.getTime()) ? null : d,
      minutes: m[2] ? parseInt(m[2], 10) : null,
      title: m[3]?.trim() || "",
      body
    });
    re.lastIndex = m.index + m[0].length;
  }
  return entries.sort((a, b) => (b.date?.getTime() || 0) - (a.date?.getTime() || 0));
}

// ── Helpers ───────────────────────────────────────────────────────────────

function formatDate(d) {
  if (!d) return "Unknown date";
  return d.toLocaleDateString(undefined, {
    weekday: "short", year: "numeric", month: "short", day: "numeric"
  }) + " " + d.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
}

function esc(s) {
  return (s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function highlight(text, q) {
  const safe = esc(text);
  if (!q) return safe;
  const re = new RegExp(`(${q.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")})`, "gi");
  return safe.replace(re, "<mark>$1</mark>");
}

// ── Render ────────────────────────────────────────────────────────────────

function renderEntries(query = "") {
  const q = esc(query.trim().toLowerCase());
  const list = $("entries");
  list.innerHTML = "";

  const filtered = q
    ? allEntries.filter(e =>
        (e.body + e.title + formatDate(e.date) + (e.minutes ? `${e.minutes}m` : ""))
          .toLowerCase().includes(query.trim().toLowerCase())
      )
    : allEntries;

  $("state-empty").hidden   = allEntries.length > 0;
  $("state-no-match").hidden = !(allEntries.length > 0 && filtered.length === 0);

  $("count").textContent = q
    ? `${filtered.length} of ${allEntries.length} entries`
    : `${allEntries.length} entr${allEntries.length === 1 ? "y" : "ies"}`;

  for (const e of filtered) {
    const n = allEntries.length - allEntries.indexOf(e);
    const li = document.createElement("li");
    li.className = "entry";
    li.innerHTML = `
      <div class="entry-meta">
        <span class="entry-date">${formatDate(e.date)}</span>
        ${e.minutes ? `<span class="entry-duration">${e.minutes}m</span>` : ""}
        <span class="entry-number">#${n}</span>
      </div>
      ${e.title ? `<div class="entry-title">${highlight(e.title, q)}</div>` : ""}
      <div class="entry-body">${highlight(e.body, q)}</div>`;
    list.appendChild(li);
  }
}

// ── File I/O ──────────────────────────────────────────────────────────────

async function readAndRender() {
  try {
    const file = await handle.getFile();
    rawText = await file.text();
  } catch (err) {
    console.warn("Could not re-read file:", err);
    return;
  }
  allEntries = parseEntries(rawText);
  renderEntries($("search").value);
}

// ── Main flow ─────────────────────────────────────────────────────────────

async function init() {
  handle = await loadFileHandle().catch(() => null);

  if (!handle) {
    $("no-file").hidden = false;
    return;
  }

  $("file-name").textContent = handle.name;
  $("gate-file").textContent = handle.name;

  // Try to read without asking — succeeds when permission is already held.
  const perm = await handle.queryPermission({ mode: "readonly" }).catch(() => "denied");
  if (perm === "granted") {
    await readAndRender();
    $("view").hidden = false;
    return;
  }

  // Permission not yet granted — show the gate screen.
  // A real user click is required before requestPermission can be called.
  $("gate").hidden = false;
}

// ── Gate screen ───────────────────────────────────────────────────────────

$("grant-btn").addEventListener("click", async () => {
  $("gate-error").hidden = true;
  let granted;
  try {
    granted = await handle.requestPermission({ mode: "readonly" });
  } catch (err) {
    $("gate-error").textContent = `Could not request permission: ${err?.message || err}`;
    $("gate-error").hidden = false;
    return;
  }
  if (granted !== "granted") {
    $("gate-error").textContent = "Access was denied. Please try again or re-select the file in Settings.";
    $("gate-error").hidden = false;
    return;
  }
  await readAndRender();
  $("gate").hidden = true;
  $("view").hidden = false;
});

// ── In-view controls ──────────────────────────────────────────────────────

$("search").addEventListener("input", (e) => renderEntries(e.target.value));

$("refresh").addEventListener("click", readAndRender);

$("export").addEventListener("click", () => {
  if (!rawText) return;
  const blob = new Blob([rawText], { type: "text/markdown" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = handle?.name || "worklog.md";
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
});

$("open-settings").addEventListener("click", () => chrome.runtime.openOptionsPage());
$("gate-settings").addEventListener("click", () => chrome.runtime.openOptionsPage());
$("go-settings").addEventListener("click", () => chrome.runtime.openOptionsPage());

init();
