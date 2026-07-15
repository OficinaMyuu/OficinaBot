import { describe, expect, it } from "vitest"
import { calculateAge } from "./birthdayUtils"

describe("calculateAge", () => {
  it("does not count the current year's birthday before it happens", () => {
    expect(calculateAge("2000-06-15", new Date(2026, 5, 14))).toBe(25)
  })

  it("counts the current year's birthday on and after the date", () => {
    expect(calculateAge("2000-06-15", new Date(2026, 5, 15))).toBe(26)
    expect(calculateAge("2000-06-15", new Date(2026, 6, 14))).toBe(26)
  })
})
