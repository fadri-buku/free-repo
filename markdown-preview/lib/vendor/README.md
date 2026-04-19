# Vendored diagram libraries

The Markdown preview renders fenced `mermaid` / `nomnoml` code blocks as SVG
diagrams, fully locally. The rendering libraries live in this folder and are
**committed to the repo** so every clone / every `Load unpacked` on a new
device just works.

Currently vendored:

- `mermaid.esm.min.mjs` — flowchart, sequence, class, ER, state, gantt, pie,
  gitGraph, journey.
- `nomnoml.js` — class / component / sequence diagrams, simpler syntax.

## Installing / updating

Run from the repo root:

```sh
# Mermaid
curl -L \
  -o markdown-preview/lib/vendor/mermaid.esm.min.mjs \
  https://cdn.jsdelivr.net/npm/mermaid@11.4.1/dist/mermaid.esm.min.mjs

# Nomnoml
curl -L \
  -o markdown-preview/lib/vendor/nomnoml.js \
  https://cdn.jsdelivr.net/npm/nomnoml@1.6.2/dist/nomnoml.js
```

Pin to an exact version (not a range like `mermaid@11`) so the committed
file is reproducible. After downloading, verify the files look sane
(first lines should be valid JS, not an HTML error page) and commit.

## Security notes

Vendoring a third-party library means it executes inside the extension's
origin, with access to `chrome.storage.local` and the preview DOM. Treat
updates with some care:

- Pin a specific version and diff the file against the previous vendored
  copy on upgrade, rather than blindly replacing it.
- Check npm/jsdelivr downloads counts and GitHub activity; both of these
  libraries are mainstream and maintained, but a compromised release is
  always theoretically possible.
- Mermaid is initialized with `securityLevel: "strict"` so any HTML /
  script in rendered SVG is removed.
- Neither library needs network; the extension declares no host
  permissions, so a library call-out would fail CSP.
