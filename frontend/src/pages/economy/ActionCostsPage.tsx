import type { ActionCost } from "@/types/actionCost"
import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"
import { FiEdit2, FiRefreshCw } from "react-icons/fi"
import { DashboardLayout } from "@/components/layout/DashboardLayout"
import { Button } from "@/components/ui/Button"
import { actionCostService } from "@/services/actionCostService"
import { formatLocalTimestamp } from "@/utils/time"

import styles from "./ActionCostsPage.module.css"

const actionCostsQueryKey = ["action-costs"] as const
const priceFormatter = new Intl.NumberFormat()

export function ActionCostsPage() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [editingItemType, setEditingItemType] = useState<
    ActionCost["item_type"] | null
  >(null)
  const [draftPrice, setDraftPrice] = useState("")
  const [notice, setNotice] = useState<string | null>(null)
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
      setNotice(toMessage(error))
    }
  })

  const beginEditing = (item: ActionCost) => {
    setEditingItemType(item.item_type)
    setDraftPrice(String(item.price))
    setNotice(null)
  }

  const cancelEditing = () => {
    setEditingItemType(null)
    setDraftPrice("")
  }

  const save = (item: ActionCost) => {
    const price = Number(draftPrice)
    if (!Number.isSafeInteger(price) || price < 0) {
      setNotice(t("economy.actionCosts.messages.invalidPrice"))
      return
    }
    updateCost.mutate({ item, price })
  }

  const items = actionCostsQuery.data ?? []

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

        {notice ? (
          <div className={styles.notice} role="status">
            <span>{notice}</span>
            <button type="button" onClick={() => setNotice(null)}>
              {t("common.dismiss")}
            </button>
          </div>
        ) : null}

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
                  <th>{t("economy.actionCosts.fields.action")}</th>
                  <th>{t("economy.actionCosts.fields.command")}</th>
                  <th>{t("economy.actionCosts.fields.cost")}</th>
                  <th>{t("economy.actionCosts.fields.updatedAt")}</th>
                  <th aria-label={t("common.actions")} />
                </tr>
              </thead>
              <tbody>
                {items.map((item) => {
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
                        {isEditing ? (
                          <label className={styles.priceInput}>
                            <span className={styles.srOnly}>
                              {t("economy.actionCosts.fields.cost")}
                            </span>
                            <input
                              aria-label={t(`${itemKey}.title`)}
                              inputMode="numeric"
                              min="0"
                              onChange={(event) =>
                                setDraftPrice(event.target.value)
                              }
                              step="1"
                              type="number"
                              value={draftPrice}
                            />
                          </label>
                        ) : (
                          formatPrice(item.price)
                        )}
                      </td>
                      <td>
                        <time dateTime={item.updated_at}>
                          {formatLocalTimestamp(item.updated_at)}
                        </time>
                      </td>
                      <td>
                        <div className={styles.rowActions}>
                          {isEditing ? (
                            <>
                              <Button
                                type="button"
                                variant="secondary"
                                disabled={isSaving}
                                onClick={cancelEditing}
                              >
                                {t("common.cancel")}
                              </Button>
                              <Button
                                type="button"
                                disabled={isSaving}
                                onClick={() => save(item)}
                              >
                                {t("economy.actionCosts.actions.save")}
                              </Button>
                            </>
                          ) : (
                            <button
                              type="button"
                              className={styles.editButton}
                              title={t("economy.actionCosts.actions.edit")}
                              onClick={() => beginEditing(item)}
                            >
                              <FiEdit2 aria-hidden="true" />
                            </button>
                          )}
                        </div>
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

function ActionCostsSkeleton({ label }: { label: string }) {
  return (
    <div className={styles.skeleton} role="status" aria-label={label}>
      {[0, 1, 2, 3].map((row) => (
        <div className={styles.skeletonRow} key={row}>
          <span />
          <span />
          <span />
          <span />
        </div>
      ))}
    </div>
  )
}

function formatPrice(value: number): string {
  return priceFormatter.format(value)
}

function toMessage(error: unknown): string {
  if (typeof error === "object" && error !== null && "message" in error) {
    return String(error.message)
  }
  if (error instanceof Error) {
    return error.message
  }
  return "Unexpected error"
}
