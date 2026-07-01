import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import MapPage from './MapPage.jsx'
import { makeGallerySummary, jsonResponse } from '../test/fixtures.js'

function renderMapPage() {
  return render(
    <MemoryRouter>
      <MapPage />
    </MemoryRouter>,
  )
}

describe('MapPage', () => {
  it('shows a loading placeholder then the header once galleries load', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse([makeGallerySummary()])))

    renderMapPage()

    expect(screen.getByText('Loading galleries…')).toBeInTheDocument()
    expect(await screen.findByText('Mappics')).toBeInTheDocument()
  })

  it('shows an error placeholder when the request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(null, { ok: false, status: 500 })))

    renderMapPage()

    expect(await screen.findByText(/Could not load galleries/)).toBeInTheDocument()
  })

  it('warns when galleries exist but none have GPS', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      jsonResponse([makeGallerySummary({ averageGps: null })]),
    ))

    renderMapPage()

    expect(await screen.findByText(/run the import first/)).toBeInTheDocument()
  })
})
