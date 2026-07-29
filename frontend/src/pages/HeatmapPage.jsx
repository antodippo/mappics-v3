import { useState, useEffect, useMemo } from 'react'
import { MapContainer, ZoomControl, useMap } from 'react-leaflet'
import { useNavigate } from 'react-router-dom'
import L from 'leaflet'
import '../leafletPlugins.js'
import { fetchPictures } from '../api/client.js'
import BasemapLayer from '../components/BasemapLayer.jsx'
import AppHeader from '../components/AppHeader.jsx'
import useDocumentTitle from '../useDocumentTitle.js'
import { ZOOM_OPTIONS } from '../mapOptions.js'
import './HeatmapPage.css'

// Below this zoom the world is a heatmap; at/above it, individual (clustered)
// thumbnails appear.
const THUMBNAIL_ZOOM = 7

function thumbIcon(thumbnailUrl) {
  if (thumbnailUrl) {
    const size = 48
    return L.divIcon({
      className: '',
      html: `<div class="thumb-marker"><img src="${thumbnailUrl}" alt="" loading="lazy" /></div>`,
      iconSize:   [size, size],
      iconAnchor: [size / 2, size / 2],
    })
  }
  return L.divIcon({
    className: '',
    html: '<div class="picture-pin"></div>',
    iconSize:   [14, 14],
    iconAnchor: [7, 7],
  })
}

// Swaps between a canvas heatmap (low zoom) and clustered thumbnail markers
// (high zoom). Both layers are built imperatively — neither plugin has a
// react-leaflet wrapper. Clustering is essential: the dataset can be thousands
// of pictures, far too many raw DOM markers to render at once.
function PictureLayers({ points, navigate }) {
  const map = useMap()
  const [zoom, setZoom] = useState(() => map.getZoom())
  const showThumbnails = zoom >= THUMBNAIL_ZOOM

  useEffect(() => {
    const onZoom = () => setZoom(map.getZoom())
    map.on('zoomend', onZoom)
    return () => map.off('zoomend', onZoom)
  }, [map])

  // Heatmap — only while zoomed out.
  useEffect(() => {
    if (showThumbnails) return
    const heat = L.heatLayer(
      points.map(p => [p.gps.latitude, p.gps.longitude]),
      { radius: 22, blur: 18, maxZoom: THUMBNAIL_ZOOM },
    )
    heat.addTo(map)
    return () => heat.remove()
  }, [map, points, showThumbnails])

  // Clustered thumbnails — only while zoomed in.
  useEffect(() => {
    if (!showThumbnails) return
    const cluster = L.markerClusterGroup({ chunkedLoading: true })
    points.forEach(p => {
      const marker = L.marker([p.gps.latitude, p.gps.longitude], { icon: thumbIcon(p.thumbnailUrl) })
      marker.on('click', () => navigate(`/gallery/${p.galleryId}`, { state: { from: '/heatmap', fromLabel: 'Heatmap' } }))
      cluster.addLayer(marker)
    })
    cluster.addTo(map)
    return () => cluster.remove()
  }, [map, points, showThumbnails, navigate])

  return null
}

export default function HeatmapPage() {
  const navigate = useNavigate()
  const [pictures, setPictures] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useDocumentTitle('Heatmap · Mappics')

  useEffect(() => {
    fetchPictures()
      .then(setPictures)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  const points = useMemo(() => pictures.filter(p => p.gps), [pictures])

  if (loading) return <div className="page-placeholder">Loading pictures…</div>
  if (error)   return <div className="page-placeholder">Could not load pictures: {error}</div>

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
        {...ZOOM_OPTIONS}
      >
        <BasemapLayer defaultKey="dark" storageKey="mappics.basemap.heatmap" />
        <ZoomControl position="bottomright" />
        <PictureLayers points={points} navigate={navigate} />
      </MapContainer>

      {pictures.length > 0 && points.length === 0 && (
        <div className="map-empty-notice">
          Pictures found but no GPS data yet — run the import first.
        </div>
      )}
      {pictures.length === 0 && (
        <div className="map-empty-notice">
          No pictures yet — run the import first.
        </div>
      )}
    </div>
  )
}
