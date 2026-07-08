import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import AppHeader from './AppHeader.jsx'

function renderHeader(initialPath = '/') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AppHeader />
    </MemoryRouter>,
  )
}

describe('AppHeader', () => {
  it('renders the Mappics title linking home', () => {
    renderHeader()

    const title = screen.getByRole('link', { name: 'Mappics' })
    expect(title).toHaveAttribute('href', '/')
  })

  it('renders the Galleries, Heatmap and Stats nav links', () => {
    renderHeader()

    expect(screen.getByRole('link', { name: 'Galleries' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('link', { name: 'Heatmap' })).toHaveAttribute('href', '/heatmap')
    expect(screen.getByRole('link', { name: 'Stats' })).toHaveAttribute('href', '/stats')
  })

  it('marks the Heatmap link active on that route', () => {
    renderHeader('/heatmap')

    expect(screen.getByRole('link', { name: 'Heatmap' })).toHaveClass('active')
    expect(screen.getByRole('link', { name: 'Galleries' })).not.toHaveClass('active')
  })
})
