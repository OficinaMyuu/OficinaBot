import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { BirthdayForm } from './BirthdayForm'
import '@/services/i18n'

describe('BirthdayForm', () => {
  it('submits normalized birthday payloads', () => {
    const onSubmit = vi.fn()
    render(<BirthdayForm formId="birthday-form" onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText(/id do discord/i), { target: { value: ' 12345 ' } })
    fireEvent.change(screen.getByLabelText(/nome/i), { target: { value: ' Myuu ' } })
    fireEvent.change(screen.getByLabelText(/aniversário/i), { target: { value: '2020-05-10' } })
    fireEvent.change(screen.getByLabelText(/fuso/i), { target: { value: '-2' } })
    fireEvent.submit(document.getElementById('birthday-form')!)

    expect(onSubmit).toHaveBeenCalledWith({
      userId: '12345',
      name: 'Myuu',
      birthday: '2020-05-10',
      zoneHours: -2,
    })
  })

  it('locks the Discord id while editing an existing birthday', () => {
    render(
      <BirthdayForm
        formId="birthday-form"
        birthday={{
          userId: '12345',
          name: 'Myuu',
          birthday: '2020-05-10',
          zoneHours: -3,
          createdAt: 1,
          updatedAt: 1,
        }}
        onSubmit={vi.fn()}
      />,
    )

    expect(screen.getByLabelText(/id do discord/i)).toBeDisabled()
  })
})
