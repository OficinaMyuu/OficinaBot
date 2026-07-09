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
      user_id: '12345',
      name: 'Myuu',
      birthday: '2020-05-10',
      zone_hours: -2,
    })
  })

  it('locks the Discord id while editing an existing birthday', () => {
    render(
      <BirthdayForm
        formId="birthday-form"
        birthday={{
          user_id: '12345',
          name: 'Myuu',
          birthday: '2020-05-10',
          zone_hours: -3,
          created_at: 1,
          updated_at: 1,
        }}
        onSubmit={vi.fn()}
      />,
    )

    expect(screen.getByLabelText(/id do discord/i)).toBeDisabled()
  })
})
