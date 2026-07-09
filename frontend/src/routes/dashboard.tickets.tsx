/* eslint-disable react-refresh/only-export-components */
import { createFileRoute } from '@tanstack/react-router'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { TicketsPage } from '@/pages/tickets/TicketsPage'

export const Route = createFileRoute('/dashboard/tickets')({
  component: TicketsRoute,
})

function TicketsRoute() {
  return (
    <ProtectedRoute>
      <TicketsPage />
    </ProtectedRoute>
  )
}
