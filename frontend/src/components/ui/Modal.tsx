import { useEffect } from 'react'
import { FiX } from 'react-icons/fi'
import styles from './Modal.module.css'

type ModalProps = {
  children: React.ReactNode
  footer?: React.ReactNode
  open: boolean
  title: string
  onClose: () => void
}

export function Modal({ children, footer, open, title, onClose }: ModalProps) {
  useEffect(() => {
    if (!open) return

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, open])

  if (!open) {
    return null
  }

  return (
    <div className={styles.backdrop} role="presentation" onMouseDown={onClose}>
      <section className={styles.modal} role="dialog" aria-modal="true" aria-label={title} onMouseDown={(event) => event.stopPropagation()}>
        <header className={styles.header}>
          <h2>{title}</h2>
          <button type="button" onClick={onClose} aria-label="Close">
            <FiX aria-hidden="true" />
          </button>
        </header>
        <div className={styles.body}>{children}</div>
        {footer && <footer className={styles.footer}>{footer}</footer>}
      </section>
    </div>
  )
}
