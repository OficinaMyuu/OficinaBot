import type { ButtonHTMLAttributes } from 'react'
import styles from './Button.module.css'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
}

export function Button({ className, variant = 'primary', ...props }: ButtonProps) {
  return <button className={[styles.button, styles[variant], className].filter(Boolean).join(' ')} {...props} />
}
