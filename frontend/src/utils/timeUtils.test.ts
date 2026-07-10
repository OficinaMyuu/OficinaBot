import { describe, expect, it } from "vitest"
import { formatLocalTimestamp } from "./timeUtils"

describe("formatLocalTimestamp", () => {
  it("formats RFC 3339 timestamps returned by the API", () => {
    expect(formatLocalTimestamp("2023-11-14T22:13:20Z")).toMatch(
      /14 de Nov\. de 2023, às \d{2}:\d{2}/
    )
  })
})
