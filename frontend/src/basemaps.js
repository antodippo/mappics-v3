const ESRI_ATTRIBUTION = 'Tiles &copy; Esri'
const ESRI = 'https://server.arcgisonline.com/ArcGIS/rest/services'

// Esri tile paths are {z}/{y}/{x}, not the {z}/{x}/{y} most XYZ providers use.
const tiles = service => `${ESRI}/${service}/MapServer/tile/{z}/{y}/{x}`

// maxNativeZoom below maxZoom lets Leaflet upscale tiles past the deepest level
// a service publishes, instead of rendering blank tiles.
export const BASEMAPS = [
  {
    key: 'dark',
    label: 'Dark',
    short: 'D',
    url: tiles('Canvas/World_Dark_Gray_Base'),
    attribution: ESRI_ATTRIBUTION,
    maxZoom: 20,
    maxNativeZoom: 16,
    overlays: [{ url: tiles('Canvas/World_Dark_Gray_Reference'), maxNativeZoom: 16 }],
  },
  {
    key: 'light',
    label: 'Light',
    short: 'L',
    url: tiles('Canvas/World_Light_Gray_Base'),
    attribution: ESRI_ATTRIBUTION,
    maxZoom: 20,
    maxNativeZoom: 16,
    overlays: [{ url: tiles('Canvas/World_Light_Gray_Reference'), maxNativeZoom: 16 }],
  },
  {
    key: 'satellite',
    label: 'Satellite',
    short: 'Sat',
    url: tiles('World_Imagery'),
    attribution: ESRI_ATTRIBUTION,
    maxZoom: 20,
    maxNativeZoom: 19,
    overlays: [
      { url: tiles('Reference/World_Boundaries_and_Places'), maxNativeZoom: 13 },
      { url: tiles('Reference/World_Transportation'), maxNativeZoom: 19 },
    ],
  },
  {
    key: 'streets',
    label: 'Streets',
    short: 'St',
    url: tiles('World_Street_Map'),
    attribution: ESRI_ATTRIBUTION,
    maxZoom: 20,
    maxNativeZoom: 19,
  },
]

export const DEFAULT_BASEMAP = 'satellite'
