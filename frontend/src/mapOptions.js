// Zoom feel, shared by every map.
//
// Leaflet snaps to whole zoom levels by default, and each level doubles the
// scale — a single wheel notch is a big jump. `zoomSnap` allows half-level
// resting positions and `wheelPxPerZoomLevel` slows the wheel/trackpad down so
// those positions are actually reachable. `zoomDelta` stays coarse on purpose:
// it drives the +/- buttons and keyboard, where a half step feels broken.
export const ZOOM_OPTIONS = {
  zoomSnap: 0.5,
  zoomDelta: 1,
  wheelPxPerZoomLevel: 120,
}
