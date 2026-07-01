// Shared test fixtures mirroring the backend domain shape (see CLAUDE.md).

export function makePicture(overrides = {}) {
  return {
    id: 'pic-1',
    galleryId: 'g1',
    filename: 'IMG_0001.jpg',
    thumbnailUrl: 'http://example.test/thumb/pic-1.jpg',
    fullSizeUrl: 'http://example.test/full/pic-1.jpg',
    gps: { latitude: 46.5197, longitude: 6.6323, altitude: 372 },
    exif: {
      cameraMake: 'FUJIFILM',
      cameraModel: 'X-T5',
      takenAt: '2024-08-15T14:30:00',
      focalLength: '35mm',
      aperture: 'f/2.8',
      iso: 200,
    },
    location: { name: 'Lausanne', shortDescription: 'Vaud, Switzerland' },
    weather: {
      temperatureCelsius: 21.4,
      humidity: 58,
      windSpeedKmh: 12.3,
      wmoCode: 1,
      description: 'Mainly clear',
    },
    ...overrides,
  }
}

export function makeGalleryDetail(overrides = {}) {
  return {
    id: 'g1',
    name: 'Swiss Trip',
    pictures: [
      makePicture({ id: 'pic-1' }),
      makePicture({ id: 'pic-2', gps: { latitude: 46.2, longitude: 6.15, altitude: 400 } }),
    ],
    ...overrides,
  }
}

export function makeGallerySummary(overrides = {}) {
  return {
    id: 'g1',
    name: 'Swiss Trip',
    averageGps: { latitude: 46.5, longitude: 6.6 },
    pictureCount: 2,
    ...overrides,
  }
}

// Resolve fetch with a JSON body, mimicking the Response API client.js uses.
export function jsonResponse(body, { ok = true, status = 200 } = {}) {
  return { ok, status, json: async () => body }
}
