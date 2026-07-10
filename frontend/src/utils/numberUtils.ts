export function formatNumber(num: number): string {
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  }).format(num)
}

/**
 * Strip non-digit characters from input, then format with pt-BR
 * thousands separator for real-time input display. Preserves leading minus sign.
 */
export function formatIntegerInput(raw: string): string {
  const isNegative = raw.trim().startsWith('-')
  const digits = raw.replace(/\D/g, '')
  if (digits === '') return isNegative ? '-' : ''
  const formatted = Number(digits).toLocaleString('pt-BR')
  return isNegative ? `-${formatted}` : formatted
}

/**
 * Parse a pt-BR formatted integer string back to a number.
 * Strips non-digit characters while preserving leading minus sign.
 */
export function parseFormattedInteger(formatted: string): number {
  const isNegative = formatted.trim().startsWith('-')
  const digits = formatted.replace(/\D/g, '')
  if (digits === '') return NaN
  return Number(digits) * (isNegative ? -1 : 1)
}
