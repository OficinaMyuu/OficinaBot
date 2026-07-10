import { apiClient } from "./apiClient"
import type { ActionCost, UpdateActionCostPayload } from "@/types/actionCost"

const ACTION_COSTS_PATH = "/economy/action-costs"

type ActionCostsResponse = {
  items: ActionCost[]
}

export const actionCostService = {
  async list(): Promise<ActionCost[]> {
    const response = await apiClient.get<ActionCostsResponse>(ACTION_COSTS_PATH)
    return response.items
  },

  update(itemType: ActionCost["item_type"], payload: UpdateActionCostPayload): Promise<ActionCost> {
    return apiClient.patch<ActionCost>(`${ACTION_COSTS_PATH}/${itemType}`, payload)
  }
}
