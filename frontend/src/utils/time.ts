export function epochToDate(value: number): Date {
  return new Date(value < 1_000_000_000_000 ? value * 1000 : value)
}
