import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import GalleryPage from './GalleryPage.jsx'
import { makeGalleryDetail, jsonResponse } from '../test/fixtures.js'

function renderGalleryPage() {
  return render(
    <MemoryRouter initialEntries={['/gallery/g1']}>
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
})
