import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import App from './App'

describe('App Component', () => {
  it('renders app overview layout successfully', async () => {
    render(<App />)
    expect(await screen.findByText('OficinaServices')).toBeInTheDocument()
    expect(screen.getByText('OficinaBot: Synced')).toBeInTheDocument()
  })
})
