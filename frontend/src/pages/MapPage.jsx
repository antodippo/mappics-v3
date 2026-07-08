import { useState, useEffect, useMemo } from 'react'
import { MapContainer, Marker, Tooltip, ZoomControl, useMap } from 'react-leaflet'
import { useNavigate } from 'react-router-dom'
import L from 'leaflet'
import { fetchGalleries } from '../api/client.js'
import BasemapLayer from '../components/BasemapLayer.jsx'
import AppHeader from '../components/AppHeader.jsx'
import useDocumentTitle from '../useDocumentTitle.js'
import './MapPage.css'

function FitBounds({ galleries }) {
  const map = useMap()
  useEffect(() => {
    const coords = galleries
      .filter(g => g.averageGps)
      .map(g => [g.averageGps.latitude, g.averageGps.longitude])
    if (coords.length > 0) map.fitBounds(coords, { padding: [50, 50], maxZoom: 10 })
  }, [galleries, map])
  return null
}

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
  const navigate = useNavigate()
  const [galleries, setGalleries] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useDocumentTitle('Mappics')

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
      <AppHeader />

      <MapContainer
        center={[30, 10]}
        zoom={2}
        minZoom={2}
        worldCopyJump
        zoomControl={false}
        className="leaflet-map"
      >
        <BasemapLayer />
        <ZoomControl position="bottomright" />
        <FitBounds galleries={mapped} />

        {mapped.map(gallery => (
          <Marker
            key={gallery.id}
            position={[gallery.averageGps.latitude, gallery.averageGps.longitude]}
            icon={icon}
            eventHandlers={{ click: () => navigate(`/gallery/${gallery.id}`) }}
          >
            <Tooltip direction="top" offset={[0, -10]}>{gallery.name}</Tooltip>
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
