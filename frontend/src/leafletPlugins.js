// leaflet.heat and leaflet.markercluster are old UMD plugins that reference a
// GLOBAL `L` instead of importing Leaflet. Under Vite's ESM there is no global
// `L`, so we expose it first — from a separate module, because static imports
// within a single file are hoisted and would run before any assignment here.
import './leafletGlobal.js'
import 'leaflet.heat'
import 'leaflet.markercluster'
import 'leaflet.markercluster/dist/MarkerCluster.css'
import 'leaflet.markercluster/dist/MarkerCluster.Default.css'
