import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import StatsPage from './StatsPage.jsx'
import { jsonResponse } from '../test/fixtures.js'

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/stats']}>
      <StatsPage />
    </MemoryRouter>,
  )
}

const stat = (galleryId, value) => ({
  pictureId: `${galleryId}/p.jpg`,
  galleryId,
  galleryName: galleryId[0].toUpperCase() + galleryId.slice(1),
  thumbnailUrl: `http://example.test/${galleryId}.jpg`,
  value,
})

const statistics = (overrides = {}) => ({
  totalPictures: 128,
  galleryCount: 7,
  totalTraveledKm: 4567.8,
  northernmost: stat('iceland', 64.13),
  southernmost: stat('chile', -53.1),
  easternmost: stat('kenya', 36.8),
  westernmost: stat('chile', -70.9),
  highestAltitude: stat('kenya', 1700),
  coldest: stat('chile', -5),
  hottest: stat('kenya', 30),
  oldest: { pictureId: 'chile/p.jpg', galleryId: 'chile', galleryName: 'Chile', thumbnailUrl: 't', takenAt: '2019-03-01T12:00:00' },
  newest: { pictureId: 'kenya/p.jpg', galleryId: 'kenya', galleryName: 'Kenya', thumbnailUrl: 't', takenAt: '2022-12-01T12:00:00' },
  mostUsedCamera: 'Nikon D850',
  dateSpanDays: 1371,
  biggestGallery: { galleryId: 'iceland', name: 'Iceland', pictureCount: 42 },
  averageTemperatureCelsius: 13.4,
  ...overrides,
})

describe('StatsPage', () => {
  it('shows a loading placeholder then the header once stats load', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(statistics())))

    renderPage()

    expect(screen.getByText('Loading statistics…')).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Mappics' })).toBeInTheDocument()
  })

  it('renders the overview figures and the compass extremes', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(statistics())))

    renderPage()

    expect(await screen.findByText('128')).toBeInTheDocument()
    expect(screen.getByText('Iceland · 42')).toBeInTheDocument()
    // Distance is locale-formatted; assert the label + a km value rather than a fixed separator.
    expect(screen.getByText('Distance travelled')).toBeInTheDocument()
    expect(screen.getByText(content => content.includes('km'))).toBeInTheDocument()
    // Compass extremes + centre altitude.
    expect(screen.getByText('Northernmost')).toBeInTheDocument()
    expect(screen.getByText('Highest')).toBeInTheDocument()
    expect(screen.getByText(/1[.,]700\s*m/)).toBeInTheDocument()
  })

  it('links each record card to its gallery', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(statistics())))

    renderPage()

    const northernmost = (await screen.findByText('Northernmost')).closest('a')
    expect(northernmost).toHaveAttribute('href', '/gallery/iceland')
    expect(screen.getByText('64.13° N')).toBeInTheDocument()
    // The record card labels the gallery the picture belongs to.
    expect(northernmost).toHaveTextContent('Iceland')
  })

  it('omits records that are absent', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      jsonResponse(statistics({ northernmost: null, hottest: null })),
    ))

    renderPage()

    await screen.findByRole('link', { name: 'Mappics' })
    expect(screen.queryByText('Northernmost')).not.toBeInTheDocument()
    expect(screen.queryByText('Hottest')).not.toBeInTheDocument()
    // A sibling record is still shown.
    expect(screen.getByText('Southernmost')).toBeInTheDocument()
  })

  it('shows an error placeholder when the request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(null, { ok: false, status: 500 })))

    renderPage()

    expect(await screen.findByText(/Could not load statistics/)).toBeInTheDocument()
  })
})
