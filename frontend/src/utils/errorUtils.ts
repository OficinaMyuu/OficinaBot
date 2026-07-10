export function toMessage(error: unknown): string {
  if (typeof error === "object" && error !== null && "message" in error) {
    return String(error.message)
  }
  if (error instanceof Error) {
    return error.message
  }
  return "Unexpected error"
}
