import '@testing-library/jest-dom'
import { afterEach, vi } from 'vitest'

// ── react-leaflet ───────────────────────────────────────────────────────────
// Leaflet needs real DOM measurements; in jsdom every element is zero-size, so
// a real map would make fitBounds/setView throw NaN. We replace the map
// primitives with inert stand-ins and stub the useMap hook. App logic (data
// fetching, keyboard nav, overlays, basemap persistence) is what we actually
// assert on — the map itself is out of scope for the jsdom tier.
vi.mock('react-leaflet', async () => {
  const React = await import('react')
  const box = (testid) => ({ children }) =>
    React.createElement('div', { 'data-testid': testid }, children)
  return {
    MapContainer: box('map-container'),
    TileLayer: box('tile-layer'),
    Marker: box('marker'),
    Tooltip: box('tooltip'),
    useMap: () => ({
      fitBounds: () => {},
      setView: () => {},
      getZoom: () => 8,
      addControl: () => {},
      removeControl: () => {},
    }),
  }
})

// ── leaflet ─────────────────────────────────────────────────────────────────
// Just enough of the imperative API the components touch: divIcon (icons),
// DomUtil.create (a real node for the BasemapControl portal target), a control
// whose addTo/remove attach the container to the document so its portalled
// buttons are queryable, and no-op DomEvent helpers.
vi.mock('leaflet', () => {
  const L = {
    divIcon: (options) => ({ options }),
    control: () => {
      const ctrl = {
        onAdd: null,
        _el: null,
        addTo() {
          if (typeof ctrl.onAdd === 'function') {
            ctrl._el = ctrl.onAdd()
            if (ctrl._el) document.body.appendChild(ctrl._el)
          }
          return ctrl
        },
        remove() {
          if (ctrl._el?.parentNode) ctrl._el.parentNode.removeChild(ctrl._el)
          return ctrl
        },
      }
      return ctrl
    },
    DomUtil: {
      create: (tag, className) => {
        const el = document.createElement(tag)
        if (className) el.className = className
        return el
      },
    },
    DomEvent: {
      disableClickPropagation: () => {},
      disableScrollPropagation: () => {},
    },
  }
  return { default: L }
})

afterEach(() => {
  localStorage.clear()
  vi.unstubAllGlobals()
  vi.clearAllMocks()
})
