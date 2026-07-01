const CARTO_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors ' +
  '&copy; <a href="https://carto.com/attributions">CARTO</a>'
const ESRI_ATTRIBUTION = 'Tiles &copy; Esri'

export const BASEMAPS = [
  {
    key: 'dark',
    label: 'Dark',
    short: 'D',
    url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
    attribution: CARTO_ATTRIBUTION,
    subdomains: 'abcd',
    maxZoom: 20,
  },
  {
    key: 'light',
    label: 'Light',
    short: 'L',
    url: 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
    attribution: CARTO_ATTRIBUTION,
    subdomains: 'abcd',
    maxZoom: 20,
  },
  {
    key: 'satellite',
    label: 'Satellite',
    short: 'Sat',
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    attribution: ESRI_ATTRIBUTION,
    maxZoom: 19,
  },
  {
    key: 'streets',
    label: 'Streets',
    short: 'St',
    url: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
    attribution: CARTO_ATTRIBUTION,
    subdomains: 'abcd',
    maxZoom: 20,
  },
]

export const DEFAULT_BASEMAP = 'satellite'
