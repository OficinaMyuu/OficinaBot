import { describe, expect, it } from "vitest"
import i18n from "./i18n"

describe("birthday age translations", () => {
  it("uses localized singular and plural age labels", () => {
    const translatePortuguese = i18n.getFixedT("pt-BR")
    const translateEnglish = i18n.getFixedT("en-US")

    expect(translatePortuguese("birthdays.age", { count: 1 })).toBe("1 ano")
    expect(translatePortuguese("birthdays.age", { count: 25 })).toBe("25 anos")
    expect(translateEnglish("birthdays.age", { count: 1 })).toBe("1 year")
    expect(translateEnglish("birthdays.age", { count: 25 })).toBe("25 years")
  })
})
