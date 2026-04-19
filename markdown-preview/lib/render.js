// Minimal Markdown → sanitized HTML renderer.
//
// Scope: covers the common subset of CommonMark + GFM we need for a preview
// pane (headings, emphasis, code, links, images, lists, blockquotes, hr,
// tables, paragraphs). It is NOT a spec-complete implementation — if a file
// renders oddly, prefer opening an issue over cramming edge cases in.
//
// Pipeline: tokenize blocks → render blocks → render inline spans →
// sanitize the resulting HTML via an allowlist before returning.

const ALLOWED_TAGS = new Set([
  "p", "br", "hr",
  "h1", "h2", "h3", "h4", "h5", "h6",
  "strong", "em", "del", "code", "pre",
  "a", "img",
  "ul", "ol", "li",
  "blockquote",
  "table", "thead", "tbody", "tr", "th", "td",
  "span", "div",
]);

const ALLOWED_ATTRS = {
  a: new Set(["href", "title"]),
  img: new Set(["src", "alt", "title"]),
  th: new Set(["align"]),
  td: new Set(["align"]),
  code: new Set(["class"]),     // for language-xxx
  span: new Set(["class"]),
  div: new Set(["class"]),
};

const SAFE_URL = /^(https?:|mailto:|#|\/|\.\/|\.\.\/)/i;
const SAFE_IMG = /^(https?:|data:image\/(?:png|jpe?g|gif|webp|svg\+xml);|\/|\.\/|\.\.\/)/i;

function escapeHtml(s) {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

// ── Inline ──────────────────────────────────────────────────────────────────
function renderInline(text) {
  const codeSpans = [];
  text = text.replace(/`([^`\n]+)`/g, (_, c) => {
    codeSpans.push(`<code>${escapeHtml(c)}</code>`);
    return `\u0000${codeSpans.length - 1}\u0000`;
  });

  text = escapeHtml(text);

  // Images first (so ![..](..) doesn't match as a link)
  text = text.replace(/!\[([^\]]*)\]\(([^)\s]+)(?:\s+&quot;([^&]*?)&quot;)?\)/g,
    (_, alt, url, title) => {
      if (!SAFE_IMG.test(url)) return escapeHtml(`![${alt}](${url})`);
      const t = title ? ` title="${title}"` : "";
      return `<img src="${url}" alt="${alt}"${t} />`;
    });

  text = text.replace(/\[([^\]]+)\]\(([^)\s]+)(?:\s+&quot;([^&]*?)&quot;)?\)/g,
    (_, label, url, title) => {
      if (!SAFE_URL.test(url)) return escapeHtml(`[${label}](${url})`);
      const t = title ? ` title="${title}"` : "";
      return `<a href="${url}"${t} rel="noopener noreferrer" target="_blank">${label}</a>`;
    });

  // Autolinks <https://...>
  text = text.replace(/&lt;((?:https?|mailto):[^\s&]+)&gt;/g,
    (_, url) => SAFE_URL.test(url)
      ? `<a href="${url}" rel="noopener noreferrer" target="_blank">${url}</a>`
      : escapeHtml(`<${url}>`));

  // Bold / italic / strike. Order matters: bold before italic.
  text = text.replace(/\*\*([^\s*][^*]*?[^\s*]|\S)\*\*/g, "<strong>$1</strong>");
  text = text.replace(/__([^\s_][^_]*?[^\s_]|\S)__/g, "<strong>$1</strong>");
  text = text.replace(/(^|[^*])\*([^\s*][^*]*?[^\s*]|\S)\*(?!\*)/g, "$1<em>$2</em>");
  text = text.replace(/(^|[^_\w])_([^\s_][^_]*?[^\s_]|\S)_(?!\w)/g, "$1<em>$2</em>");
  text = text.replace(/~~([^~]+)~~/g, "<del>$1</del>");

  // Hard line breaks (two trailing spaces before newline)
  text = text.replace(/  \n/g, "<br />\n");

  // Restore code spans
  text = text.replace(/\u0000(\d+)\u0000/g, (_, i) => codeSpans[Number(i)]);

  return text;
}

// ── Blocks ──────────────────────────────────────────────────────────────────
function renderBlocks(src) {
  // Normalize line endings and strip trailing spaces on blank lines
  const lines = src.replace(/\r\n?/g, "\n").split("\n");
  const out = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    // Blank → skip
    if (/^\s*$/.test(line)) { i++; continue; }

    // Fenced code block
    const fence = line.match(/^(\s{0,3})(`{3,}|~{3,})\s*([^\s`]*)\s*$/);
    if (fence) {
      const [, , marker, lang] = fence;
      i++;
      const buf = [];
      while (i < lines.length && !lines[i].startsWith(marker)) {
        buf.push(lines[i]); i++;
      }
      if (i < lines.length) i++; // consume closing fence
      const cls = lang ? ` class="language-${escapeHtml(lang)}"` : "";
      out.push(`<pre><code${cls}>${escapeHtml(buf.join("\n"))}\n</code></pre>`);
      continue;
    }

    // ATX heading
    const h = line.match(/^(#{1,6})\s+(.*?)\s*#*\s*$/);
    if (h) {
      const level = h[1].length;
      out.push(`<h${level}>${renderInline(h[2])}</h${level}>`);
      i++;
      continue;
    }

    // Horizontal rule
    if (/^\s{0,3}([-*_])(?:\s*\1){2,}\s*$/.test(line)) {
      out.push("<hr />");
      i++;
      continue;
    }

    // Blockquote (collect contiguous > lines)
    if (/^\s{0,3}>/.test(line)) {
      const buf = [];
      while (i < lines.length && /^\s{0,3}>/.test(lines[i])) {
        buf.push(lines[i].replace(/^\s{0,3}>\s?/, ""));
        i++;
      }
      out.push(`<blockquote>\n${renderBlocks(buf.join("\n"))}\n</blockquote>`);
      continue;
    }

    // Table (GFM): header | --- | --- | body…
    if (
      i + 1 < lines.length &&
      /\|/.test(line) &&
      /^\s*\|?\s*:?-{2,}:?(\s*\|\s*:?-{2,}:?)+\s*\|?\s*$/.test(lines[i + 1])
    ) {
      const rendered = renderTable(lines, i);
      if (rendered) {
        out.push(rendered.html);
        i = rendered.next;
        continue;
      }
    }

    // List (ordered / unordered, with simple nesting via indentation)
    if (/^\s*(?:[-*+]|\d+[.)])\s+/.test(line)) {
      const parsed = parseList(lines, i);
      out.push(parsed.html);
      i = parsed.next;
      continue;
    }

    // Paragraph: collect until blank line or another block starter
    const buf = [line];
    i++;
    while (i < lines.length && !isBlockBoundary(lines[i])) {
      buf.push(lines[i]); i++;
    }
    out.push(`<p>${renderInline(buf.join("\n"))}</p>`);
  }

  return out.join("\n");
}

function isBlockBoundary(line) {
  if (/^\s*$/.test(line)) return true;
  if (/^(\s{0,3})(`{3,}|~{3,})/.test(line)) return true;
  if (/^#{1,6}\s/.test(line)) return true;
  if (/^\s{0,3}>/.test(line)) return true;
  if (/^\s{0,3}([-*_])(?:\s*\1){2,}\s*$/.test(line)) return true;
  if (/^\s*(?:[-*+]|\d+[.)])\s+/.test(line)) return true;
  return false;
}

function parseList(lines, start) {
  const firstIndent = lines[start].match(/^(\s*)/)[1].length;
  const ordered = /^\s*\d+[.)]\s+/.test(lines[start]);
  const items = [];
  let i = start;
  let current = null;

  while (i < lines.length) {
    const line = lines[i];
    const itemMatch = line.match(/^(\s*)(?:[-*+]|\d+[.)])\s+(.*)$/);
    if (itemMatch && itemMatch[1].length === firstIndent) {
      if (current) items.push(current);
      current = { text: itemMatch[2], children: [] };
      i++;
    } else if (/^\s*$/.test(line)) {
      // Blank line: tentatively end list unless next line continues at same indent
      const next = lines[i + 1];
      if (next && new RegExp(`^\\s{${firstIndent}}(?:[-*+]|\\d+[.)])\\s+`).test(next)) {
        i++;
      } else {
        break;
      }
    } else if (current && line.match(/^\s+\S/)) {
      current.children.push(line.replace(new RegExp(`^\\s{0,${firstIndent + 2}}`), ""));
      i++;
    } else {
      break;
    }
  }
  if (current) items.push(current);

  const tag = ordered ? "ol" : "ul";
  const body = items.map(it => {
    const inner = it.children.length
      ? `${renderInline(it.text)}\n${renderBlocks(it.children.join("\n"))}`
      : renderInline(it.text);
    return `<li>${inner}</li>`;
  }).join("\n");
  return { html: `<${tag}>\n${body}\n</${tag}>`, next: i };
}

function renderTable(lines, start) {
  const header = splitRow(lines[start]);
  const aligns = splitRow(lines[start + 1]).map(cell => {
    const l = cell.startsWith(":"), r = cell.endsWith(":");
    if (l && r) return "center";
    if (r) return "right";
    if (l) return "left";
    return null;
  });
  if (header.length !== aligns.length) return null;

  const body = [];
  let i = start + 2;
  while (i < lines.length && /\|/.test(lines[i]) && lines[i].trim() !== "") {
    body.push(splitRow(lines[i]));
    i++;
  }

  const th = header.map((h, idx) => {
    const a = aligns[idx] ? ` align="${aligns[idx]}"` : "";
    return `<th${a}>${renderInline(h)}</th>`;
  }).join("");
  const rows = body.map(row => {
    const tds = row.map((c, idx) => {
      const a = aligns[idx] ? ` align="${aligns[idx]}"` : "";
      return `<td${a}>${renderInline(c)}</td>`;
    }).join("");
    return `<tr>${tds}</tr>`;
  }).join("\n");

  return {
    html: `<table>\n<thead><tr>${th}</tr></thead>\n<tbody>\n${rows}\n</tbody>\n</table>`,
    next: i,
  };
}

function splitRow(line) {
  return line
    .replace(/^\s*\|?/, "")
    .replace(/\|?\s*$/, "")
    .split(/(?<!\\)\|/)
    .map(c => c.trim().replace(/\\\|/g, "|"));
}

// ── Sanitizer ───────────────────────────────────────────────────────────────
// Parse the generated HTML into a detached document and walk the tree,
// dropping any tag / attribute that isn't on the allowlist. Using the DOM is
// both safer and simpler than regex stripping.
function sanitize(html) {
  const doc = new DOMParser().parseFromString(`<div id="root">${html}</div>`, "text/html");
  const root = doc.getElementById("root");
  walk(root);
  return root.innerHTML;
}

function walk(node) {
  const children = Array.from(node.childNodes);
  for (const child of children) {
    if (child.nodeType === Node.ELEMENT_NODE) {
      const tag = child.tagName.toLowerCase();
      if (!ALLOWED_TAGS.has(tag)) {
        // Unwrap: replace element with its text content so we don't execute/render it.
        const text = child.ownerDocument.createTextNode(child.textContent || "");
        child.replaceWith(text);
        continue;
      }
      // Strip disallowed attrs
      const allowed = ALLOWED_ATTRS[tag] || new Set();
      for (const attr of Array.from(child.attributes)) {
        const name = attr.name.toLowerCase();
        if (!allowed.has(name) || /^on/i.test(name)) {
          child.removeAttribute(attr.name);
          continue;
        }
        if ((name === "href" && !SAFE_URL.test(attr.value)) ||
            (name === "src"  && !SAFE_IMG.test(attr.value))) {
          child.removeAttribute(attr.name);
        }
      }
      walk(child);
    }
  }
}

// ── Public API ──────────────────────────────────────────────────────────────
export function renderMarkdown(src) {
  if (typeof src !== "string") return "";
  const html = renderBlocks(src);
  return sanitize(html);
}
