import { apiClient } from "./apiClient"
import type { TicketListQuery, TicketPage } from "@/types/ticket"

const TICKETS_PATH = "/tickets"

export const ticketService = {
  list(query: TicketListQuery): Promise<TicketPage> {
    const params = new URLSearchParams()
    const search = query.search.trim()
    if (search) {
      params.set("search", search)
    }
    if (query.status !== "all") {
      params.set("status", query.status)
    }
    if (query.limit !== undefined) {
      params.set("limit", String(query.limit))
    }
    if (query.cursor) {
      params.set("cursor", query.cursor)
    }

    const suffix = params.size > 0 ? `?${params.toString()}` : ""
    return apiClient.get<TicketPage>(`${TICKETS_PATH}${suffix}`)
  }
}
