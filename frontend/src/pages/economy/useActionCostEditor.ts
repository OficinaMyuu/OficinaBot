import type { ActionCost } from "@/types/actionCost"
import { useRef, useState } from "react"

import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"
import { actionCostService } from "@/services/actionCostService"
import { toMessage } from "@/utils/errorUtils"
import { formatIntegerInput, parseFormattedInteger } from "@/utils/numberUtils"

export const actionCostsQueryKey = ["action-costs"] as const

export function useActionCostEditor() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [editingItemType, setEditingItemType] = useState<
    ActionCost["item_type"] | null
  >(null)
  const [draftPrice, setDraftPrice] = useState("")
  const [notice, setNotice] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const updateCost = useMutation({
    mutationFn: ({ item, price }: { item: ActionCost; price: number }) =>
      actionCostService.update(item.item_type, { price }),
    onSuccess: (updated) => {
      queryClient.setQueryData<ActionCost[]>(
        actionCostsQueryKey,
        (items = []) =>
          items.map((item) =>
            item.item_type === updated.item_type ? updated : item
          )
      )
      setEditingItemType(null)
      setNotice(t("economy.actionCosts.messages.updated"))
    },
    onError: (error) => {
      setEditingItemType(null)
      setNotice(toMessage(error))
    }
  })

  const beginEditing = (item: ActionCost) => {
    setEditingItemType(item.item_type)
    setDraftPrice(formatIntegerInput(String(item.price)))
    setNotice(null)
    requestAnimationFrame(() => {
      inputRef.current?.focus()
      inputRef.current?.select()
    })
  }

  const save = (item: ActionCost) => {
    const price = parseFormattedInteger(draftPrice)
    if (!Number.isSafeInteger(price) || price < 0) {
      setNotice(t("economy.actionCosts.messages.invalidPrice"))
      return
    }
    if (price === item.price) {
      setEditingItemType(null)
      return
    }
    updateCost.mutate({ item, price })
  }

  return {
    beginEditing,
    cancelEditing: () => setEditingItemType(null),
    draftPrice,
    editingItemType,
    inputRef,
    isSaving: updateCost.isPending,
    notice,
    save,
    setDraftPrice,
    setNotice
  }
}
