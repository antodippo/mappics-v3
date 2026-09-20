import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import BasemapLayer from './BasemapLayer.jsx'
import { BASEMAPS, DEFAULT_BASEMAP } from '../basemaps.js'

const tileUrls = () =>
  screen.getAllByTestId('tile-layer').map(el => el.getAttribute('data-url'))

const basemap = key => BASEMAPS.find(b => b.key === key)

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

  it('renders the satellite imagery under its reference overlays', () => {
    const satellite = basemap('satellite')
    render(<BasemapLayer />)

    expect(tileUrls()).toEqual([
      satellite.url,
      ...satellite.overlays.map(o => o.url),
    ])
  })

  it('renders the dark basemap under its label overlay', () => {
    const dark = basemap('dark')
    localStorage.setItem('mappics.basemap', 'dark')
    render(<BasemapLayer />)

    expect(tileUrls()).toEqual([dark.url, ...dark.overlays.map(o => o.url)])
  })

  it('renders streets as a single layer', () => {
    localStorage.setItem('mappics.basemap', 'streets')
    render(<BasemapLayer />)

    expect(tileUrls()).toEqual([basemap('streets').url])
  })
})
