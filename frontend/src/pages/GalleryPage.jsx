import { useState, useEffect, useCallback, useMemo } from 'react'
import { Link, useParams } from 'react-router-dom'
import { MapContainer, TileLayer, Marker, useMap } from 'react-leaflet'
import L from 'leaflet'
import { fetchGallery } from '../api/client.js'
import PictureOverlay from '../components/PictureOverlay.jsx'
import './GalleryPage.css'

const DARK_TILES = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
const DARK_TILES_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors ' +
  '&copy; <a href="https://carto.com/attributions">CARTO</a>'

function makeIcon(thumbnailUrl, selected) {
  if (thumbnailUrl) {
    const size = selected ? 68 : 54
    return L.divIcon({
      className: '',
      html: `<div class="thumb-marker${selected ? ' selected' : ''}">
               <img src="${thumbnailUrl}" alt="" />
             </div>`,
      iconSize:   [size, size],
      iconAnchor: [size / 2, size / 2],
    })
  }
  // Fallback dot for pictures without a thumbnail
  return L.divIcon({
    className: '',
    html: `<div class="picture-pin${selected ? ' selected' : ''}"></div>`,
    iconSize:   [14, 14],
    iconAnchor: [7, 7],
  })
}

function FitBounds({ pictures }) {
  const map = useMap()
  useEffect(() => {
    const coords = pictures.filter(p => p.gps).map(p => [p.gps.latitude, p.gps.longitude])
    if (coords.length > 0) map.fitBounds(coords, { padding: [50, 50], maxZoom: 14 })
  }, [pictures, map])
  return null
}

export default function GalleryPage() {
  const { id } = useParams()
  const [gallery, setGallery]       = useState(null)
  const [loading, setLoading]       = useState(true)
  const [error, setError]           = useState(null)
  const [selectedIdx, setSelectedIdx] = useState(null)

  useEffect(() => {
    fetchGallery(id)
      .then(setGallery)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  const close = useCallback(() => setSelectedIdx(null), [])
  const prev  = useCallback(() => setSelectedIdx(i => Math.max(0, i - 1)), [])
  const next  = useCallback(() =>
    setSelectedIdx(i => Math.min(gallery.pictures.length - 1, i + 1)), [gallery])

  // Recreate icons only when gallery data or the selected index changes
  const icons = useMemo(() => {
    if (!gallery) return []
    return gallery.pictures.map((pic, i) => makeIcon(pic.thumbnailUrl, i === selectedIdx))
  }, [gallery, selectedIdx])

  if (loading) return <div className="page-placeholder">Loading gallery…</div>
  if (error)   return <div className="page-placeholder">Error: {error}</div>
  if (!gallery) return null

  const pictures = gallery.pictures ?? []
  const mappable = pictures.filter(p => p.gps)

  return (
    <div className="gallery-page">
      <header className="gallery-header">
        <Link to="/" className="back-link">← Mappics</Link>
        <h1 className="gallery-title">{gallery.name}</h1>
        <span className="gallery-count">{pictures.length} photos</span>
      </header>

      <section className="gallery-map-section">
        {mappable.length > 0 ? (
          <MapContainer
            center={[mappable[0].gps.latitude, mappable[0].gps.longitude]}
            zoom={8}
            className="gallery-leaflet-map"
          >
            <TileLayer url={DARK_TILES} attribution={DARK_TILES_ATTRIBUTION}
                       subdomains="abcd" maxZoom={20} />
            <FitBounds pictures={pictures} />
            {pictures.map((pic, i) =>
              pic.gps ? (
                <Marker
                  key={pic.id}
                  position={[pic.gps.latitude, pic.gps.longitude]}
                  icon={icons[i]}
                  eventHandlers={{ click: () => setSelectedIdx(i) }}
                />
              ) : null
            )}
          </MapContainer>
        ) : (
          <div className="gallery-map-empty">No GPS data for this gallery yet.</div>
        )}
      </section>

      <section className="thumbnail-strip-section">
        <div className="thumbnail-strip">
          {pictures.map((pic, i) => (
            <button
              key={pic.id}
              className={`thumbnail-btn${i === selectedIdx ? ' active' : ''}`}
              onClick={() => setSelectedIdx(i)}
            >
              {pic.thumbnailUrl
                ? <img src={pic.thumbnailUrl} alt="" loading="lazy" />
                : <div className="thumbnail-placeholder" />}
            </button>
          ))}
        </div>
      </section>

      {selectedIdx !== null && (
        <PictureOverlay
          picture={pictures[selectedIdx]}
          index={selectedIdx}
          total={pictures.length}
          onClose={close}
          onPrev={selectedIdx > 0 ? prev : null}
          onNext={selectedIdx < pictures.length - 1 ? next : null}
        />
      )}
    </div>
  )
}
