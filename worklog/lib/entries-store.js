const KEY = "worklogEntries";

export async function getEntries() {
  const { [KEY]: list } = await chrome.storage.local.get(KEY);
  return Array.isArray(list) ? list : [];
}

export async function appendEntry(entry) {
  const list = await getEntries();
  list.push(entry);
  await chrome.storage.local.set({ [KEY]: list });
}

export async function setEntries(list) {
  await chrome.storage.local.set({ [KEY]: list });
}

// Parse a worklog Markdown file's text into entry objects.
// Header format: "## ISO_TIMESTAMP (Nm) — Title" (duration and title optional)
export function parseWorklogText(raw) {
  const entries = [];
  const sectionRe = /^## (.+?)(?:\s+\((\d+m)\))?(?:\s+—\s+(.+))?\s*$/gm;
  let match;
  while ((match = sectionRe.exec(raw)) !== null) {
    const bodyStart = match.index + match[0].length + 1;
    const nextIdx = raw.indexOf("\n## ", bodyStart);
    const bodyEnd = nextIdx === -1 ? raw.length : nextIdx;
    const body = raw.slice(bodyStart, bodyEnd).trim();
    const ts = match[1].trim();
    const d = new Date(ts);
    entries.push({
      timestamp: isNaN(d.getTime()) ? ts : d.toISOString(),
      minutes: match[2] ? parseInt(match[2], 10) : null,
      title: match[3] ? match[3].trim() : "",
      body
    });
    sectionRe.lastIndex = match.index + match[0].length;
  }
  return entries;
}

// Merge imported entries into existing list, skipping ones already present
// (matched by timestamp + title + body).
export async function mergeImport(imported) {
  const current = await getEntries();
  const key = (e) => `${e.timestamp}|${e.title}|${e.body}`;
  const seen = new Set(current.map(key));
  let added = 0;
  for (const e of imported) {
    if (!seen.has(key(e))) {
      current.push(e);
      seen.add(key(e));
      added++;
    }
  }
  await setEntries(current);
  return { added, total: current.length };
}

export function entryToMarkdown({ timestamp, minutes, title, body }) {
  const duration = minutes ? ` (${minutes}m)` : "";
  const titlePart = title ? ` — ${title}` : "";
  return `## ${timestamp}${duration}${titlePart}\n${body}\n`;
}
