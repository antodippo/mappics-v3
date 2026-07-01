import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import BasemapLayer from './BasemapLayer.jsx'
import { DEFAULT_BASEMAP } from '../basemaps.js'

describe('BasemapLayer', () => {
  it('defaults to the satellite basemap when localStorage is empty', () => {
    expect(DEFAULT_BASEMAP).toBe('satellite')
    render(<BasemapLayer />)

    expect(screen.getByRole('button', { name: 'Satellite' })).toHaveClass('active')
  })

  it('persists the selected basemap to localStorage', async () => {
    render(<BasemapLayer />)

    await userEvent.click(screen.getByRole('button', { name: 'Dark' }))

    expect(localStorage.getItem('mappics.basemap')).toBe('dark')
    expect(screen.getByRole('button', { name: 'Dark' })).toHaveClass('active')
  })

  it('reads the saved basemap back from localStorage on mount', () => {
    localStorage.setItem('mappics.basemap', 'light')
    render(<BasemapLayer />)

    expect(screen.getByRole('button', { name: 'Light' })).toHaveClass('active')
  })
})
