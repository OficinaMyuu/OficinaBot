import { Navigate } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { useSession } from '@/contexts/SessionContext'
import { Spinner } from '@/components/ui/loaders'
import styles from './ProtectedRoute.module.css'

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { t } = useTranslation()
  const { isLoading, user } = useSession()

  if (isLoading) {
    return (
      <main className={styles.centered}>
        <Spinner size={20} color="var(--border-soft)" spinColor="var(--accent)" />
        <span>{t('auth.loading')}</span>
      </main>
    )
  }

  if (!user) {
    return <Navigate to="/dashboard/login" replace />
  }

  return children
}
