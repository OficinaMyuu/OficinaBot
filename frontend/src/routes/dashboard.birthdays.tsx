/* eslint-disable react-refresh/only-export-components */
import { createFileRoute } from '@tanstack/react-router'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { BirthdaysPage } from '@/pages/birthdays/BirthdaysPage'

export const Route = createFileRoute('/dashboard/birthdays')({
  component: BirthdaysRoute,
})

function BirthdaysRoute() {
  return (
    <ProtectedRoute>
      <BirthdaysPage />
    </ProtectedRoute>
  )
}
