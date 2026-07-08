import { useState, useEffect } from 'react'
import { MapContainer, ZoomControl } from 'react-leaflet'
import { Link } from 'react-router-dom'
import { fetchStatistics } from '../api/client.js'
import BasemapLayer from '../components/BasemapLayer.jsx'
import AppHeader from '../components/AppHeader.jsx'
import useDocumentTitle from '../useDocumentTitle.js'
import './StatsPage.css'

const latLabel = v => `${Math.abs(v).toFixed(2)}° ${v >= 0 ? 'N' : 'S'}`
const lonLabel = v => `${Math.abs(v).toFixed(2)}° ${v >= 0 ? 'E' : 'W'}`
const metres   = v => `${Math.round(v).toLocaleString()} m`
const celsius  = v => `${v.toFixed(1)} °C`
const day      = iso => new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })

// A scalar figure (no picture behind it).
function StatCard({ title, value }) {
  if (value == null) return null
  return (
    <div className="stat-card">
      <span className="stat-title">{title}</span>
      <span className="stat-value">{value}</span>
    </div>
  )
}

// An extremum that resolves to a specific picture — links through to its gallery.
// `area` places the card in a named grid cell (used by the compass) so an absent
// record leaves its cell empty instead of shifting the others.
function RecordCard({ title, stat, format, area }) {
  if (!stat) return null
  return (
    <Link
      to={`/gallery/${stat.galleryId}`}
      state={{ from: '/stats', fromLabel: 'Stats', pictureId: stat.pictureId }}
      className="stat-card stat-record"
      style={area ? { gridArea: area } : undefined}
    >
      {stat.thumbnailUrl && (
        <img src={stat.thumbnailUrl} alt="" className="stat-thumb" loading="lazy" />
      )}
      <span className="stat-body">
        <span className="stat-title">{title}</span>
        <span className="stat-value">{format(stat)}</span>
        {stat.galleryName && <span className="stat-gallery">{stat.galleryName}</span>}
      </span>
    </Link>
  )
}

export default function StatsPage() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useDocumentTitle('Stats · Mappics')

  useEffect(() => {
    fetchStatistics()
      .then(setStats)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="page-placeholder">Loading statistics…</div>
  if (error)   return <div className="page-placeholder">Could not load statistics: {error}</div>

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
        <BasemapLayer defaultKey="dark" storageKey="mappics.basemap.stats" />
        <ZoomControl position="bottomright" />
      </MapContainer>

      <section className="stats-panel">
        <h1 className="stats-heading">Statistics</h1>

        <div className="stats-row cols-3">
          <StatCard title="Pictures" value={stats.totalPictures.toLocaleString()} />
          <StatCard title="Galleries" value={stats.galleryCount.toLocaleString()} />
          <StatCard
            title="Biggest gallery"
            value={stats.biggestGallery && `${stats.biggestGallery.name} · ${stats.biggestGallery.pictureCount}`}
          />
        </div>

        <div className="stats-row cols-2">
          <StatCard title="Distance travelled" value={`${Math.round(stats.totalTraveledKm).toLocaleString()} km`} />
          <StatCard title="Time span" value={stats.dateSpanDays == null ? null : `${stats.dateSpanDays.toLocaleString()} days`} />
        </div>

        <div className="stats-compass">
          <RecordCard area="north"  title="Northernmost" stat={stats.northernmost}    format={s => latLabel(s.value)} />
          <RecordCard area="west"   title="Westernmost"  stat={stats.westernmost}     format={s => lonLabel(s.value)} />
          <RecordCard area="center" title="Highest"      stat={stats.highestAltitude} format={s => metres(s.value)} />
          <RecordCard area="east"   title="Easternmost"  stat={stats.easternmost}     format={s => lonLabel(s.value)} />
          <RecordCard area="south"  title="Southernmost" stat={stats.southernmost}    format={s => latLabel(s.value)} />
        </div>

        <div className="stats-pairs">
          <RecordCard title="Hottest" stat={stats.hottest} format={s => celsius(s.value)} />
          <RecordCard title="Coldest" stat={stats.coldest} format={s => celsius(s.value)} />
          <RecordCard title="Oldest"  stat={stats.oldest}  format={s => day(s.takenAt)} />
          <RecordCard title="Newest"  stat={stats.newest}  format={s => day(s.takenAt)} />
        </div>
      </section>
    </div>
  )
}
