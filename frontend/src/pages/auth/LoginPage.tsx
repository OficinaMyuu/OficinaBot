import { useSearch } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { FaDiscord } from 'react-icons/fa'
import { Button } from '@/components/ui/Button'
import { useSession } from '@/contexts/SessionContext'
import styles from './LoginPage.module.css'

type LoginSearch = {
  error?: string
}

export function LoginPage() {
  const { t } = useTranslation()
  const { login, error } = useSession()
  const search = useSearch({ strict: false }) as LoginSearch
  const authError = search.error ? t(`auth.errors.${search.error}`, t('auth.errors.generic')) : error

  return (
    <main className={styles.page}>
      <section className={styles.panel}>
        <div className={styles.brandMark}>O</div>
        <h1>{t('auth.title')}</h1>
        <p>{t('auth.subtitle')}</p>
        {authError && <div className={styles.error}>{authError}</div>}
        <Button type="button" onClick={login}>
          <FaDiscord aria-hidden="true" />
          {t('auth.loginWithDiscord')}
        </Button>
      </section>
    </main>
  )
}
