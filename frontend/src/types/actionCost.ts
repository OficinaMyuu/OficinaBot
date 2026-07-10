export type StoreItemType =
  | "GROUP"
  | "GROUP_TEXT_CHANNEL"
  | "GROUP_VOICE_CHANNEL"
  | "UPDATE_GROUP"
  | "ADDITIONAL_BOT"
  | "GROUP_SLOT"
  | "GROUP_PERMISSION"
  | "PIN_MESSAGE"
  | "COLOR_ROLE"
  | "COUNTING_RELEASE"
  | "MARRIAGE"

export type ActionCost = {
  item_type: StoreItemType
  price: number
  created_at: string
  updated_at: string
  updated_by: string | null
}

export type UpdateActionCostPayload = {
  price: number
}
