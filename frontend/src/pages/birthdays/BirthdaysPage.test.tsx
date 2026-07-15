import type { ReactNode } from "react"

import { BirthdaysPage } from "./BirthdaysPage"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { birthdayService } from "@/services/birthdayService"
import { userService } from "@/services/userService"
import { useBirthdaysStore } from "@/stores/useBirthdaysStore"
import { useUsersStore } from "@/stores/useUsersStore"
import { calculateAge } from "@/utils/birthdayUtils"

import i18n from "@/services/i18n"
import "@/services/i18n"

vi.mock("@/components/layout/DashboardLayout", () => ({
  DashboardLayout: ({
    children,
    title
  }: {
    children: ReactNode
    title: string
  }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  )
}))

vi.mock("@/services/birthdayService", () => ({
  birthdayService: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock("@/services/userService", () => ({
  userService: {
    query: vi.fn()
  }
}))

describe("BirthdaysPage", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useBirthdaysStore.getState().reset()
    useUsersStore.getState().reset()
    vi.mocked(birthdayService.list).mockResolvedValue([birthday])
    vi.mocked(userService.query).mockResolvedValue([user])
  })

  it("shows the birthday user avatar and effective Discord name", async () => {
    render(<BirthdaysPage />)

    expect(await screen.findByText(birthday.name)).toBeInTheDocument()
    await waitFor(() => expect(userService.query).toHaveBeenCalledWith(["42"]))
    expect(screen.getByAltText("")).toHaveAttribute("src", user.avatar_url)

    fireEvent.pointerMove(screen.getByText(birthday.name).parentElement!, {
      pointerType: "mouse"
    })

    expect(await screen.findByRole("tooltip")).toHaveTextContent("Myuu")
  })

  it("shows the birthday user's age when hovering over the birthday date", async () => {
    render(<BirthdaysPage />)

    const birthdayDate = await screen.findByText("15/06/2000")
    fireEvent.pointerMove(birthdayDate, { pointerType: "mouse" })

    expect(await screen.findByRole("tooltip")).toHaveTextContent(
      i18n.t("birthdays.age", { count: calculateAge(birthday.birthday) })
    )
  })
})

const birthday = {
  user_id: "42",
  name: "Birthday name",
  birthday: "2000-06-15",
  zone_hours: -3,
  created_at: "2023-11-14T22:13:20Z",
  updated_at: "2023-11-14T22:13:20Z"
}

const user = {
  id: "42",
  username: "myuu",
  global_name: "Myuu",
  display_name: "Myuu",
  avatar_hash: null,
  avatar_url: "https://cdn.discordapp.com/embed/avatars/0.png",
  is_bot: false
}
