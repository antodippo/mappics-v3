import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import PictureOverlay from './PictureOverlay.jsx'
import { makePicture } from '../test/fixtures.js'

function renderOverlay(props = {}) {
  const handlers = { onClose: vi.fn(), onPrev: vi.fn(), onNext: vi.fn() }
  const utils = render(
    <PictureOverlay
      picture={makePicture()}
      index={0}
      total={2}
      {...handlers}
      {...props}
    />,
  )
  return { ...utils, ...handlers }
}

describe('PictureOverlay', () => {
  it('renders the counter and location / GPS / camera / weather info', () => {
    renderOverlay()

    expect(screen.getByText('1 / 2')).toBeInTheDocument()
    expect(screen.getByText('Lausanne')).toBeInTheDocument()
    expect(screen.getByText('46.51970°, 6.63230°')).toBeInTheDocument()
    expect(screen.getByText('FUJIFILM X-T5')).toBeInTheDocument()
    expect(screen.getByText('ISO 200')).toBeInTheDocument()
    expect(screen.getByText('21.4 °C · Mainly clear')).toBeInTheDocument()
  })

  it('maps keyboard shortcuts to the nav callbacks', () => {
    const { onClose, onPrev, onNext } = renderOverlay()

    fireEvent.keyDown(window, { key: 'ArrowRight' })
    fireEvent.keyDown(window, { key: 'ArrowLeft' })
    fireEvent.keyDown(window, { key: 'Escape' })

    expect(onNext).toHaveBeenCalledOnce()
    expect(onPrev).toHaveBeenCalledOnce()
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('ignores arrow keys and hides nav buttons when onPrev/onNext are null', () => {
    const onClose = vi.fn()
    render(
      <PictureOverlay
        picture={makePicture()}
        index={0}
        total={1}
        onClose={onClose}
        onPrev={null}
        onNext={null}
      />,
    )

    expect(() => fireEvent.keyDown(window, { key: 'ArrowRight' })).not.toThrow()
    expect(screen.queryByRole('button', { name: 'Previous' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Next' })).not.toBeInTheDocument()
  })

  it('closes on backdrop click but not on content click', async () => {
    const { onClose, container } = renderOverlay()

    await userEvent.click(container.querySelector('.overlay-content'))
    expect(onClose).not.toHaveBeenCalled()

    await userEvent.click(container.querySelector('.overlay-backdrop'))
    expect(onClose).toHaveBeenCalledOnce()
  })
})
