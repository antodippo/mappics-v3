import { useState, useEffect } from 'react'
import { createPortal } from 'react-dom'
import { TileLayer, useMap } from 'react-leaflet'
import L from 'leaflet'
import { BASEMAPS, DEFAULT_BASEMAP } from '../basemaps.js'

const STORAGE_KEY = 'mappics.basemap'

export default function BasemapLayer({
  compact = false,
  defaultKey = DEFAULT_BASEMAP,
  storageKey = STORAGE_KEY,
}) {
  const [activeKey, setActiveKey] = useState(
    () => localStorage.getItem(storageKey) || defaultKey)

  const select = key => {
    setActiveKey(key)
    localStorage.setItem(storageKey, key)
  }

  const base = BASEMAPS.find(b => b.key === activeKey) ?? BASEMAPS[0]

  return (
    <>
      {/* key forces a remount on switch so attribution/maxZoom/subdomains update cleanly */}
      <TileLayer
        key={base.key}
        url={base.url}
        attribution={base.attribution}
        subdomains={base.subdomains ?? 'abc'}
        maxZoom={base.maxZoom}
        maxNativeZoom={base.maxNativeZoom}
        zIndex={1}
      />
      {/* Labels, borders and roads composited over the base. No attribution here:
          Leaflet concatenates attribution strings without deduplicating, so the
          shared Esri credit would print once per layer. */}
      {(base.overlays ?? []).map((overlay, i) => (
        <TileLayer
          key={`${base.key}:${overlay.url}`}
          url={overlay.url}
          maxZoom={base.maxZoom}
          maxNativeZoom={overlay.maxNativeZoom}
          zIndex={2 + i}
        />
      ))}
      <BasemapControl activeKey={base.key} onSelect={select} compact={compact} />
    </>
  )
}

function BasemapControl({ activeKey, onSelect, compact }) {
  const map = useMap()
  const [container] = useState(() =>
    L.DomUtil.create('div', 'basemap-switcher' + (compact ? ' compact' : '')))

  useEffect(() => {
    const ctrl = L.control({ position: 'bottomright' })
    ctrl.onAdd = () => container
    ctrl.addTo(map)
    L.DomEvent.disableClickPropagation(container)
    L.DomEvent.disableScrollPropagation(container)
    return () => ctrl.remove()
  }, [map, container])

  return createPortal(
    BASEMAPS.map(b => (
      <button
        key={b.key}
        type="button"
        title={b.label}
        className={'basemap-btn' + (b.key === activeKey ? ' active' : '')}
        onClick={() => onSelect(b.key)}
      >
        {compact ? b.short : b.label}
      </button>
    )),
    container
  )
}
