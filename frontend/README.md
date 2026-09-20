# React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.

## Basemaps

The four map styles in the switcher are defined in `src/basemaps.js` and rendered by
`src/components/BasemapLayer.jsx`.

| Key | Base service | Overlay | Native zoom |
|---|---|---|---|
| `dark` | `Canvas/World_Dark_Gray_Base` | Dark Gray Reference | 16 |
| `light` | `Canvas/World_Light_Gray_Base` | Light Gray Reference | 16 |
| `satellite` | `World_Imagery` | Boundaries & Places, Transportation | 19 |
| `streets` | `World_Street_Map` | — | 19 |

All of them are keyless Esri services on `server.arcgisonline.com`, so there is no API key,
no secret and no build-time configuration. Two consequences worth knowing:

- Esri splits its Canvas basemaps into a label-free base plus a separate reference layer,
  which is why `dark` and `light` each composite an overlay. The same mechanism gives
  `satellite` its borders, place names and roads.
- `maxNativeZoom` sits below `maxZoom` so Leaflet upscales tiles past the deepest level a
  service publishes, rather than rendering blank tiles.

Esri has announced that these legacy basemap services begin retiring in **March 2028**.

### Tile health check

A provider can change what it serves without changing its URL — in August 2026 CARTO
started stamping "API KEY REQUIRED" into keyless tiles while still returning `200 OK` with
a valid PNG, which no status code, uptime check or Leaflet `tileerror` handler can see.

`scripts/check-basemaps.mjs` fetches one tile per layer and compares its SHA-256 against
`scripts/basemap-baseline.json`:

```bash
make basemaps            # verify against the baseline
make basemaps-baseline   # re-record it, then commit the result
```

`.github/workflows/basemap-health.yml` runs the check every Monday and opens (or comments
on) a `basemap-health` issue when it fails. Because providers do legitimately re-render
their tiles, a mismatch means "look at this", not always "broken": open the reported URL,
and if the tile is fine, re-record the baseline — the workflow can do it for you via
`workflow_dispatch` with `update_baseline` checked.
