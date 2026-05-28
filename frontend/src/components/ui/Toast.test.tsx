import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { ToastProvider, useToast } from './Toast'

const ToastDemoComponent = () => {
  const { showToast } = useToast()
  return <button onClick={() => showToast('Test toast alert!', 'success')}>Trigger Toast</button>
}

describe('Toast Notification System', () => {
  it('triggers and renders a toast notification successfully', () => {
    render(
      <ToastProvider>
        <ToastDemoComponent />
      </ToastProvider>,
    )

    const triggerBtn = screen.getByRole('button', { name: /trigger toast/i })
    fireEvent.click(triggerBtn)

    expect(screen.getByText('Test toast alert!')).toBeInTheDocument()
    expect(screen.getByText('✅')).toBeInTheDocument()
  })
})
