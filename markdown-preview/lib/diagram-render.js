// Post-processes the rendered preview DOM: finds fenced code blocks tagged
// `mermaid` or `nomnoml` and replaces them in-place with rendered SVG. All
// rendering is local — libraries live under `lib/vendor/` and are loaded
// lazily the first time a diagram of that kind appears.
//
// Libraries are NOT checked into git (too large). Follow
// `lib/vendor/README.md` to install them. If they're missing, diagrams fall
// back to a helpful placeholder pointing at that readme.

const MAX_CACHE = 64;
const renderCache = new Map();      // key: "lang:src" → svg string

// ── Lazy loaders ────────────────────────────────────────────────────────────
// Mermaid ships an ESM build (`mermaid.esm.min.mjs`). We dynamic-import it
// from the extension origin; the preview page runs in that same origin so
// no web_accessible_resources entry is required.
let mermaidPromise = null;
function loadMermaid() {
  if (!mermaidPromise) {
    const url = chrome.runtime.getURL("lib/vendor/mermaid.esm.min.mjs");
    mermaidPromise = import(url).then((mod) => {
      const m = mod.default || mod;
      m.initialize({
        startOnLoad: false,
        securityLevel: "strict",
        theme: matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "default",
      });
      return m;
    }).catch((err) => {
      mermaidPromise = null;
      throw new Error(`mermaid not installed (${err.message}). See lib/vendor/README.md`);
    });
  }
  return mermaidPromise;
}

// Nomnoml ships a UMD build that attaches to `window.nomnoml`. We inject a
// <script> tag rather than dynamic-importing, since dynamic import of a
// non-module file fails in strict mode.
let nomnomlPromise = null;
function loadNomnoml() {
  if (!nomnomlPromise) {
    nomnomlPromise = new Promise((resolve, reject) => {
      if (window.nomnoml) return resolve(window.nomnoml);
      const s = document.createElement("script");
      s.src = chrome.runtime.getURL("lib/vendor/nomnoml.js");
      s.onload = () => window.nomnoml
        ? resolve(window.nomnoml)
        : reject(new Error("nomnoml.js loaded but window.nomnoml is undefined"));
      s.onerror = () => reject(new Error("nomnoml not installed. See lib/vendor/README.md"));
      document.head.appendChild(s);
    }).catch((err) => { nomnomlPromise = null; throw err; });
  }
  return nomnomlPromise;
}

// ── Single-diagram render ──────────────────────────────────────────────────
async function renderOne(lang, src) {
  if (lang === "mermaid") {
    const m = await loadMermaid();
    const id = "mmd-" + Math.random().toString(36).slice(2, 10);
    const { svg } = await m.render(id, src);
    return svg;
  }
  if (lang === "nomnoml") {
    const n = await loadNomnoml();
    return n.renderSvg(src);
  }
  throw new Error(`unknown diagram format: ${lang}`);
}

function cacheGet(key) {
  if (!renderCache.has(key)) return null;
  const value = renderCache.get(key);
  renderCache.delete(key);
  renderCache.set(key, value);   // bump to MRU
  return value;
}

function cacheSet(key, value) {
  renderCache.set(key, value);
  if (renderCache.size > MAX_CACHE) {
    const oldest = renderCache.keys().next().value;
    renderCache.delete(oldest);
  }
}

function makeError(pre, lang, msg) {
  const wrap = document.createElement("div");
  wrap.className = "diagram diagram-error";
  wrap.textContent = `${lang} diagram error: ${msg}`;
  pre.replaceWith(wrap);
}

// ── Public pass ────────────────────────────────────────────────────────────
// Walk the rendered preview subtree and upgrade every `code.language-mermaid`
// or `code.language-nomnoml` to an inline SVG diagram. Idempotent per render
// since each call works on a freshly-rendered DOM.
export async function renderDiagramsInElement(root) {
  const nodes = root.querySelectorAll(
    "code.language-mermaid, code.language-nomnoml",
  );
  for (const code of nodes) {
    const lang = code.classList.contains("language-mermaid") ? "mermaid" : "nomnoml";
    const src = code.textContent.replace(/\n$/, "");
    const pre = code.parentElement;
    if (!pre || pre.tagName !== "PRE") continue;

    const key = `${lang}:${src}`;
    let svg = cacheGet(key);
    if (!svg) {
      try {
        svg = await renderOne(lang, src);
        cacheSet(key, svg);
      } catch (err) {
        makeError(pre, lang, err.message);
        continue;
      }
    }

    const wrap = document.createElement("div");
    wrap.className = `diagram diagram-${lang}`;
    wrap.innerHTML = svg;
    pre.replaceWith(wrap);
  }
}
