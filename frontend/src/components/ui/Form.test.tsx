import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { FormField, Input, Switch } from './Form'

describe('Form Controls Components', () => {
  it('renders FormField with label and child input', () => {
    render(
      <FormField label="Username label">
        <Input placeholder="Enter username..." />
      </FormField>,
    )
    expect(screen.getByText('Username label')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Enter username...')).toBeInTheDocument()
  })

  it('renders FormField error message and highlights label color', () => {
    render(
      <FormField label="Username label" error="Username required">
        <Input placeholder="Enter username..." />
      </FormField>,
    )
    expect(screen.getByText('⚠️ Username required')).toBeInTheDocument()
  })

  it('triggers onChange with state update when Switch checked toggles', () => {
    const handleChange = vi.fn()
    render(<Switch checked={false} onChange={handleChange} labelAfter="Toggle me" />)

    const checkbox = screen.getByRole('checkbox')
    fireEvent.click(checkbox)
    expect(handleChange).toHaveBeenCalledWith(true)
  })
})
