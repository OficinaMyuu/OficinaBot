import type { ActionCost } from "@/types/actionCost"

import { useRef, useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"
import { FiRefreshCw } from "react-icons/fi"
import { DashboardLayout } from "@/components/layout/DashboardLayout"
import { SortableHeader } from "@/components/ui/SortableHeader"
import { Button } from "@/components/ui/Button"
import { Notice } from "@/components/ui/Notice"
import { actionCostService } from "@/services/actionCostService"
import { formatLocalTimestamp } from "@/utils/time"
import { formatIntegerInput, parseFormattedInteger } from "@/utils/numberUtils"
import { useTableSort } from "@/utils/useTableSort"
import { toMessage } from "@/utils/errorUtils"
import { CostCell } from "./CostCell"
import { ActionCostsSkeleton } from "./ActionCostsSkeleton"

import styles from "./ActionCostsPage.module.css"

const actionCostsQueryKey = ["action-costs"] as const

export function ActionCostsPage() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [editingItemType, setEditingItemType] = useState<
    ActionCost["item_type"] | null
  >(null)
  const [draftPrice, setDraftPrice] = useState("")
  const [notice, setNotice] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const actionCostsQuery = useQuery({
    queryKey: actionCostsQueryKey,
    queryFn: actionCostService.list
  })
  const updateCost = useMutation({
    mutationFn: ({ item, price }: { item: ActionCost; price: number }) =>
      actionCostService.update(item.item_type, {
        price
      }),
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

  const items = actionCostsQuery.data ?? []
  const { sorted, sortKey, sortDir, toggle } = useTableSort(items)

  return (
    <DashboardLayout title={t("economy.actionCosts.title")}>
      <section className={styles.page} aria-labelledby="action-costs-title">
        <header className={styles.heading}>
          <div>
            <h2 id="action-costs-title">{t("economy.actionCosts.title")}</h2>
            <p>{t("economy.actionCosts.description")}</p>
          </div>
          <Button
            type="button"
            variant="secondary"
            disabled={actionCostsQuery.isFetching}
            onClick={() => void actionCostsQuery.refetch()}
          >
            <FiRefreshCw aria-hidden="true" />
            {t("common.refresh")}
          </Button>
        </header>

        <Notice message={notice} onDismiss={() => setNotice(null)} />

        <div className={styles.tableShell}>
          {actionCostsQuery.isLoading ? (
            <ActionCostsSkeleton label={t("economy.actionCosts.loading")} />
          ) : actionCostsQuery.isError ? (
            <div className={styles.state}>
              {toMessage(actionCostsQuery.error)}
            </div>
          ) : items.length === 0 ? (
            <div className={styles.state}>{t("economy.actionCosts.empty")}</div>
          ) : (
            <table className={styles.table}>
              <thead>
                <tr>
                  <SortableHeader
                    label={t("economy.actionCosts.fields.action")}
                    sortKey="item_type"
                    activeSortKey={sortKey as string | null}
                    sortDir={sortDir}
                    onSort={(key) => toggle(key as keyof ActionCost)}
                  />
                  <th>{t("economy.actionCosts.fields.command")}</th>
                  <SortableHeader
                    label={t("economy.actionCosts.fields.cost")}
                    sortKey="price"
                    activeSortKey={sortKey as string | null}
                    sortDir={sortDir}
                    onSort={(key) => toggle(key as keyof ActionCost)}
                  />
                  <SortableHeader
                    label={t("economy.actionCosts.fields.updatedAt")}
                    sortKey="updated_at"
                    activeSortKey={sortKey as string | null}
                    sortDir={sortDir}
                    onSort={(key) => toggle(key as keyof ActionCost)}
                  />
                </tr>
              </thead>
              <tbody>
                {sorted.map((item) => {
                  const itemKey = `economy.actionCosts.items.${item.item_type}`
                  const isEditing = item.item_type === editingItemType
                  const isSaving = isEditing && updateCost.isPending

                  return (
                    <tr key={item.item_type}>
                      <td>
                        <strong>{t(`${itemKey}.title`)}</strong>
                      </td>
                      <td className={styles.command}>
                        {t(`${itemKey}.command`)}
                      </td>
                      <td>
                        <CostCell
                          item={item}
                          isEditing={isEditing}
                          isSaving={isSaving}
                          draftPrice={draftPrice}
                          inputRef={inputRef}
                          onBeginEditing={() => beginEditing(item)}
                          onDraftChange={setDraftPrice}
                          onSave={() => save(item)}
                          onCancel={() => setEditingItemType(null)}
                        />
                      </td>
                      <td>
                        <time dateTime={item.updated_at}>
                          {formatLocalTimestamp(item.updated_at)}
                        </time>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </div>
      </section>
    </DashboardLayout>
  )
}
