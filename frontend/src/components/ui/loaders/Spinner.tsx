import styles from './Spinner.module.css'

type SpinnerProps = {
  size?: number
  borderWidth?: number
  color?: string
  spinColor?: string
  className?: string
}

export function Spinner({
  size = 24,
  borderWidth = 2,
  color = 'var(--sidebar-item-hover-border-color)',
  spinColor = '#8472a1',
  className
}: SpinnerProps) {
  return (
    <span
      className={`${styles.spinner}${className ? ` ${className}` : ''}`}
      style={{
        width: size,
        height: size,
        borderWidth,
        borderColor: color,
        borderTopColor: spinColor
      }}
    />
  )
}
