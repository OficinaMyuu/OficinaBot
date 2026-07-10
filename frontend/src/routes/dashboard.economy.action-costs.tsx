/* eslint-disable react-refresh/only-export-components */
import { createFileRoute } from "@tanstack/react-router"
import { ProtectedRoute } from "@/components/auth/ProtectedRoute"
import { ActionCostsPage } from "@/pages/economy/ActionCostsPage"

export const Route = createFileRoute("/dashboard/economy/action-costs")({
  component: ActionCostsRoute
})

function ActionCostsRoute() {
  return (
    <ProtectedRoute>
      <ActionCostsPage />
    </ProtectedRoute>
  )
}
