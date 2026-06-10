import { useState, useEffect, useMemo } from 'react'
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet'
import { Link } from 'react-router-dom'
import L from 'leaflet'
import { fetchGalleries } from '../api/client.js'
import './MapPage.css'

const DARK_TILES = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
const DARK_TILES_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors ' +
  '&copy; <a href="https://carto.com/attributions">CARTO</a>'

function galleryIcon() {
  return L.divIcon({
    className: '',
    html: '<div class="gallery-pin"></div>',
    iconSize: [18, 18],
    iconAnchor: [9, 9],
    popupAnchor: [0, -12],
  })
}

export default function MapPage() {
  const [galleries, setGalleries] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchGalleries()
      .then(setGalleries)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  const icon = useMemo(() => galleryIcon(), [])
  const mapped = galleries.filter(g => g.averageGps)

  if (loading) return <div className="page-placeholder">Loading galleries…</div>
  if (error)   return <div className="page-placeholder">Could not load galleries: {error}</div>

  return (
    <div className="map-page">
      <header className="map-header">
        <span className="map-title">Mappics</span>
        {galleries.length > 0 && (
          <span className="map-subtitle">
            {mapped.length} / {galleries.length} galleries on the map
          </span>
        )}
      </header>

      <MapContainer
        center={[30, 10]}
        zoom={2}
        minZoom={2}
        worldCopyJump
        className="leaflet-map"
      >
        <TileLayer
          url={DARK_TILES}
          attribution={DARK_TILES_ATTRIBUTION}
          subdomains="abcd"
          maxZoom={20}
        />

        {mapped.map(gallery => (
          <Marker
            key={gallery.id}
            position={[gallery.averageGps.latitude, gallery.averageGps.longitude]}
            icon={icon}
          >
            <Popup>
              <div className="gallery-popup">
                <strong>{gallery.name}</strong>
                <span className="gallery-popup-count">{gallery.pictureCount} photos</span>
                <Link to={`/gallery/${gallery.id}`} className="gallery-popup-link">
                  View gallery →
                </Link>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      {galleries.length > 0 && mapped.length === 0 && (
        <div className="map-empty-notice">
          Galleries found but no GPS data yet — run the import first.
        </div>
      )}
    </div>
  )
}
