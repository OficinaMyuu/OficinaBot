import { create } from "zustand"
import { ticketService } from "@/services/ticketService"
import type { Ticket, TicketListQuery } from "@/types/ticket"

type TicketsState = {
  tickets: Ticket[]
  query: TicketListQuery
  nextCursor: string | null
  isLoading: boolean
  isLoadingMore: boolean
  error: string | null
  load: (query: TicketListQuery) => Promise<void>
  loadMore: () => Promise<void>
  refresh: () => Promise<void>
  reset: () => void
}

let ticketRequestId = 0

export const useTicketsStore = create<TicketsState>((set, get) => ({
  tickets: [],
  query: { search: "", status: "all" },
  nextCursor: null,
  isLoading: false,
  isLoadingMore: false,
  error: null,

  async load(query) {
    const requestId = ++ticketRequestId
    set({ query, tickets: [], nextCursor: null, isLoading: true, error: null })
    try {
      const page = await ticketService.list(query)
      if (requestId === ticketRequestId) {
        set({
          tickets: page.tickets,
          nextCursor: page.next_cursor,
          isLoading: false
        })
      }
    } catch (error) {
      if (requestId === ticketRequestId) {
        set({ error: toMessage(error), isLoading: false })
      }
    }
  },

  async loadMore() {
    const { isLoadingMore, nextCursor, query } = get()
    if (isLoadingMore || !nextCursor) {
      return
    }

    set({ isLoadingMore: true, error: null })
    try {
      const page = await ticketService.list({ ...query, cursor: nextCursor })
      set((state) => ({
        tickets: [...state.tickets, ...page.tickets],
        nextCursor: page.next_cursor,
        isLoadingMore: false
      }))
    } catch (error) {
      set({ error: toMessage(error), isLoadingMore: false })
    }
  },

  refresh() {
    return get().load(get().query)
  },

  reset() {
    ticketRequestId += 1
    set({
      tickets: [],
      query: { search: "", status: "all" },
      nextCursor: null,
      isLoading: false,
      isLoadingMore: false,
      error: null
    })
  }
}))

function toMessage(error: unknown): string {
  if (typeof error === "object" && error !== null && "message" in error) {
    return String(error.message)
  }
  if (error instanceof Error) {
    return error.message
  }
  return "Unexpected error"
}
