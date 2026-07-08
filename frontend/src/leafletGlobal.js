// Exposes Leaflet as a global so legacy UMD plugins (leaflet.heat,
// leaflet.markercluster) that reference a bare `L` can find it. Must be its own
// module so it evaluates before the plugin imports in leafletPlugins.js.
import L from 'leaflet'

if (typeof window !== 'undefined') window.L = L
