import { useEffect } from 'react'
import { MapContainer, Marker, useMap } from 'react-leaflet'
import L from 'leaflet'
import BasemapLayer from './BasemapLayer.jsx'
import './PictureOverlay.css'

const miniPin = L.divIcon({
  className: '',
  html: '<div class="mini-map-pin"></div>',
  iconSize:   [14, 14],
  iconAnchor: [7, 7],
})

// Smoothly re-centres the mini-map when navigating between pictures
function RecenterMap({ lat, lng }) {
  const map = useMap()
  useEffect(() => {
    map.setView([lat, lng], map.getZoom(), { animate: true })
  }, [lat, lng, map])
  return null
}

export default function PictureOverlay({ picture, index, total, onClose, onPrev, onNext }) {
  useEffect(() => {
    function onKey(e) {
      if (e.key === 'Escape')              onClose()
      if (e.key === 'ArrowLeft'  && onPrev) onPrev()
      if (e.key === 'ArrowRight' && onNext) onNext()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose, onPrev, onNext])

  const gps = picture.gps

  return (
    <div className="overlay-backdrop" onClick={onClose}>
      <div className="overlay-content" onClick={e => e.stopPropagation()}>

        {/* ── Image ─────────────────────────────────────────────────── */}
        <div className="overlay-image-side">
          {onPrev && (
            <button className="overlay-nav left" onClick={onPrev} aria-label="Previous">‹</button>
          )}
          {picture.fullSizeUrl
            ? <img src={picture.fullSizeUrl} alt="" className="overlay-image" />
            : <div className="overlay-image-missing">No image</div>}
          {onNext && (
            <button className="overlay-nav right" onClick={onNext} aria-label="Next">›</button>
          )}
        </div>

        {/* ── Info panel ────────────────────────────────────────────── */}
        <aside className="overlay-info">
          <header className="overlay-header">
            <span className="overlay-counter">{index + 1} / {total}</span>
            <button className="overlay-close" onClick={onClose} aria-label="Close">✕</button>
          </header>

          {/* Mini-map */}
          {gps && (
            <div className="mini-map-wrapper">
              <MapContainer
                center={[gps.latitude, gps.longitude]}
                zoom={13}
                zoomControl={false}
                attributionControl={false}
                className="mini-map"
              >
                <BasemapLayer compact />
                <RecenterMap lat={gps.latitude} lng={gps.longitude} />
                <Marker position={[gps.latitude, gps.longitude]} icon={miniPin} />
              </MapContainer>
            </div>
          )}

          {picture.location && (
            <section className="info-section">
              <h3>Location</h3>
              <p className="info-primary">{picture.location.name}</p>
              <p className="info-secondary">{picture.location.shortDescription}</p>
            </section>
          )}

          {gps && (
            <section className="info-section">
              <h3>GPS</h3>
              <p className="info-secondary coords">
                {gps.latitude.toFixed(5)}°, {gps.longitude.toFixed(5)}°
              </p>
              {gps.altitude != null && (
                <p className="info-secondary">{Math.round(gps.altitude)} m altitude</p>
              )}
            </section>
          )}

          {picture.exif && (
            <section className="info-section">
              <h3>Camera</h3>
              {(picture.exif.cameraMake || picture.exif.cameraModel) && (
                <p className="info-primary">
                  {[picture.exif.cameraMake, picture.exif.cameraModel].filter(Boolean).join(' ')}
                </p>
              )}
              {picture.exif.takenAt && (
                <p className="info-secondary">{formatDate(picture.exif.takenAt)}</p>
              )}
              <div className="exif-chips">
                {picture.exif.aperture    && <span>{picture.exif.aperture}</span>}
                {picture.exif.focalLength && <span>{picture.exif.focalLength}</span>}
                {picture.exif.iso         && <span>ISO {picture.exif.iso}</span>}
              </div>
            </section>
          )}

          {picture.weather && (
            <section className="info-section">
              <h3>Weather</h3>
              <p className="info-primary">
                {picture.weather.temperatureCelsius.toFixed(1)} °C · {picture.weather.description}
              </p>
              <div className="exif-chips">
                <span>💧 {picture.weather.humidity}%</span>
                <span>💨 {picture.weather.windSpeedKmh.toFixed(1)} km/h</span>
              </div>
            </section>
          )}
        </aside>
      </div>
    </div>
  )
}

function formatDate(iso) {
  try {
    return new Date(iso).toLocaleString(undefined, {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit',
    })
  } catch {
    return iso
  }
}
