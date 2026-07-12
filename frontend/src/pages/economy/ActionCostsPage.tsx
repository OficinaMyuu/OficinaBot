import type { ActionCost } from "@/types/actionCost"

import { ActionCostsHeader } from "./ActionCostsHeader"
import { ActionCostsTable } from "./ActionCostsTable"
import { DashboardLayout } from "@/components/layout/DashboardLayout"
import { DataTableSkeleton } from "@/components/ui/DataTableSkeleton"
import { Notice } from "@/components/ui/Notice"
import { actionCostService } from "@/services/actionCostService"
import { useUsersStore } from "@/stores/useUsersStore"
import { toMessage } from "@/utils/errorUtils"
import { useEffect } from "react"
import { useQuery } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"
import { actionCostsQueryKey, useActionCostEditor } from "./useActionCostEditor"

import styles from "./ActionCostsPage.module.css"

const emptyActionCosts: ActionCost[] = []

export function ActionCostsPage() {
  const { t } = useTranslation()
  const actionCostsQuery = useQuery({
    queryKey: actionCostsQueryKey,
    queryFn: actionCostService.list
  })
  const items = actionCostsQuery.data ?? emptyActionCosts
  const usersById = useUsersStore((state) => state.usersById)
  const fetchUsers = useUsersStore((state) => state.fetchUsers)
  const editor = useActionCostEditor()

  useEffect(() => {
    void fetchUsers(
      items.flatMap((item) => (item.updated_by ? [item.updated_by] : []))
    )
  }, [fetchUsers, items])

  return (
    <DashboardLayout title={t("economy.actionCosts.title")}>
      <section className={styles.page} aria-labelledby="action-costs-title">
        <ActionCostsHeader
          isRefreshing={actionCostsQuery.isFetching}
          onRefresh={() => void actionCostsQuery.refetch()}
        />

        <Notice
          message={editor.notice}
          onDismiss={() => editor.setNotice(null)}
        />

        <div className={styles.tableShell}>
          {actionCostsQuery.isLoading ? (
            <DataTableSkeleton
              columns={4}
              label={t("economy.actionCosts.loading")}
            />
          ) : actionCostsQuery.isError ? (
            <div className={styles.state}>
              {toMessage(actionCostsQuery.error)}
            </div>
          ) : items.length === 0 ? (
            <div className={styles.state}>{t("economy.actionCosts.empty")}</div>
          ) : (
            <ActionCostsTable
              items={items}
              usersById={usersById}
              editingItemType={editor.editingItemType}
              draftPrice={editor.draftPrice}
              inputRef={editor.inputRef}
              isSaving={editor.isSaving}
              onBeginEditing={editor.beginEditing}
              onDraftChange={editor.setDraftPrice}
              onSave={editor.save}
              onCancelEditing={editor.cancelEditing}
            />
          )}
        </div>
      </section>
    </DashboardLayout>
  )
}
