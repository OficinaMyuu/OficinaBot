import { Navigate } from "@tanstack/react-router"
import { DashboardAccessSkeleton } from "@/components/auth/DashboardAccessSkeleton"
import { useSession } from "@/contexts/SessionContext"

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isLoading, user } = useSession()

  if (isLoading) {
    return <DashboardAccessSkeleton />
  }

  if (!user) {
    return <Navigate to="/dashboard/login" replace />
  }

  return children
}
