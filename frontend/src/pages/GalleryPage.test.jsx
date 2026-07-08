import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import GalleryPage from './GalleryPage.jsx'
import { makeGalleryDetail, jsonResponse } from '../test/fixtures.js'

function renderGalleryPage(entry = '/gallery/g1') {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/gallery/:id" element={<GalleryPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('GalleryPage', () => {
  it('renders the gallery name, photo count and one thumbnail per picture', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(makeGalleryDetail())))

    const { container } = renderGalleryPage()

    expect(screen.getByText('Loading gallery…')).toBeInTheDocument()
    expect(await screen.findByText('Swiss Trip')).toBeInTheDocument()
    expect(screen.getByText('2 photos')).toBeInTheDocument()
    expect(container.querySelectorAll('.thumbnail-btn')).toHaveLength(2)
  })

  it('opens the picture overlay when a thumbnail is clicked', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(makeGalleryDetail())))

    const { container } = renderGalleryPage()
    await screen.findByText('Swiss Trip')

    await userEvent.click(container.querySelector('.thumbnail-btn'))

    expect(screen.getByText('1 / 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument()
  })

  it('back link defaults to the home map when no referer is given', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(makeGalleryDetail())))

    renderGalleryPage()

    const back = await screen.findByRole('link', { name: '← Mappics' })
    expect(back).toHaveAttribute('href', '/')
  })

  it('opens the overlay for the picture passed in navigation state', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(makeGalleryDetail())))

    renderGalleryPage({ pathname: '/gallery/g1', state: { pictureId: 'pic-2' } })

    // pic-2 is the second of two pictures, so its overlay opens at 2 / 2.
    expect(await screen.findByText('2 / 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument()
  })

  it('back link points to the referer page passed in navigation state', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(makeGalleryDetail())))

    renderGalleryPage({ pathname: '/gallery/g1', state: { from: '/stats', fromLabel: 'Stats' } })

    const back = await screen.findByRole('link', { name: '← Stats' })
    expect(back).toHaveAttribute('href', '/stats')
  })
})
