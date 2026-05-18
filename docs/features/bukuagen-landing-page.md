# Bukuagen Landing Page

A static, single-page marketing site for the **Bukuagen K1**, a fictional
titanium everyday-carry (EDC) multi-tool. Lives at
[`bukuagen-landing-page/`](../../bukuagen-landing-page/) and is independent of
the rest of the repo.

## What it is

Plain HTML, CSS, and a tiny bit of JavaScript — no build step, no bundler, no
framework. Open `index.html` in a browser and it works.

The page is intentionally a complete sales template you can fork: hero, social
proof strip, feature grid, spec card, three-tier pricing, FAQ, CTA band, and
footer.

## File layout

```
bukuagen-landing-page/
├── index.html      # Single page, all sections
├── styles.css      # All styles (CSS custom properties at :root)
├── script.js       # Footer year + "Pre-order" click stub
└── assets/         # (Empty — drop product photos / OG image here)
```

## Running it locally

Just open the file:

```
open bukuagen-landing-page/index.html
```

Or serve it (recommended if you start adding images / fetch calls):

```
cd bukuagen-landing-page
python3 -m http.server 8000
# → http://localhost:8000
```

No install step, no dependencies. Google Fonts (Inter + Space Grotesk) are
loaded from a CDN — if you need fully offline rendering, self-host the fonts.

## Customizing

- **Brand name / product copy** — edit `index.html` directly. Hero, features,
  specs, pricing, and FAQ are each their own `<section>` and labelled.
- **Color & type** — change the CSS variables at the top of `styles.css`
  (`--accent`, `--bg`, `--font-display`, etc.). The whole palette is driven
  from those tokens.
- **Pre-order checkout** — `script.js` currently shows an `alert()`. Replace
  the handler with a real checkout (Stripe Checkout session, Shopify Buy
  Button, a `<form>` POST, etc.). Each pricing card already carries a
  `data-buy="single|duo|crew"` attribute.
- **Hero product art** — the device illustration is pure CSS in
  `.device-body`. Swap the whole `.hero-art` block for an `<img>` once you
  have real photography.

## Caveats

- Nothing here is wired to a real payment processor. The pricing CTAs are
  stubs.
- Inventory / "612 left" copy is static; it's there to demonstrate scarcity
  framing, not as a live counter.
- The product itself ("Bukuagen K1") is fictional — copy is illustrative.
  Update specs, materials, country of origin, and warranty wording before
  shipping a real campaign.
- Accessibility: nav, FAQ `<details>`, and CTA buttons are keyboard-friendly,
  but no formal a11y audit has been done. If you ship it, run axe or Lighthouse.
