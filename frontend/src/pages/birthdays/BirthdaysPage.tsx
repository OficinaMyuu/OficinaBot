import { useEffect, useMemo, useState } from "react"
import { useTranslation } from "react-i18next"
import { FiPlus, FiRefreshCw } from "react-icons/fi"
import { DashboardLayout } from "@/components/layout/DashboardLayout"
import { Button } from "@/components/ui/Button"
import { CustomSelect } from "@/components/ui/CustomSelect"
import { DataTableSkeleton } from "@/components/ui/DataTableSkeleton"
import { Modal } from "@/components/ui/Modal"
import { Notice } from "@/components/ui/Notice"
import { SearchInput } from "@/components/ui/SearchInput"
import { useDebouncedValue } from "@/hooks/useDebouncedValue"
import { useBirthdaysStore } from "@/stores/useBirthdaysStore"
import type { Birthday, BirthdayPayload } from "@/types/birthday"
import { toMessage } from "@/utils/errorUtils"
import { BirthdayForm } from "./BirthdayForm"
import { BirthdaysTable } from "./BirthdaysTable"
import styles from "./BirthdaysPage.module.css"

const months = [
  "all",
  "1",
  "2",
  "3",
  "4",
  "5",
  "6",
  "7",
  "8",
  "9",
  "10",
  "11",
  "12"
]
const formId = "birthday-form"

export function BirthdaysPage() {
  const { t } = useTranslation()
  const [search, setSearch] = useState("")
  const debouncedSearch = useDebouncedValue(search)
  const [month, setMonth] = useState("all")
  const [editing, setEditing] = useState<Birthday | null>(null)
  const [isCreating, setIsCreating] = useState(false)
  const [deleting, setDeleting] = useState<Birthday | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const birthdays = useBirthdaysStore((state) => state.birthdays)
  const birthdaysLoading = useBirthdaysStore((state) => state.isLoading)
  const birthdaysSaving = useBirthdaysStore((state) => state.isSaving)
  const birthdayError = useBirthdaysStore((state) => state.error)
  const loadBirthdays = useBirthdaysStore((state) => state.load)
  const refreshBirthdays = useBirthdaysStore((state) => state.refresh)
  const createBirthday = useBirthdaysStore((state) => state.createBirthday)
  const updateBirthday = useBirthdaysStore((state) => state.updateBirthday)
  const deleteBirthday = useBirthdaysStore((state) => state.deleteBirthday)

  const query = useMemo(
    () => ({ search: debouncedSearch, month }),
    [debouncedSearch, month]
  )
  const monthOptions = months.map((value) => ({
    value,
    label:
      value === "all" ? t("birthdays.filters.allMonths") : t(`months.${value}`)
  }))

  useEffect(() => {
    void loadBirthdays(query)
  }, [loadBirthdays, query])

  const submitBirthday = async (payload: BirthdayPayload) => {
    try {
      if (editing) {
        await updateBirthday(payload)
        setEditing(null)
        setNotice(t("birthdays.messages.updated"))
      } else {
        await createBirthday(payload)
        setIsCreating(false)
        setNotice(t("birthdays.messages.created"))
      }
    } catch (error) {
      setNotice(toMessage(error))
    }
  }

  const removeBirthday = async (birthday: Birthday) => {
    try {
      await deleteBirthday(birthday)
      setDeleting(null)
      setNotice(t("birthdays.messages.deleted"))
    } catch (error) {
      setNotice(toMessage(error))
    }
  }

  return (
    <DashboardLayout title={t("birthdays.title")}>
      <section className={styles.page}>
        <div className={styles.toolbar}>
          <SearchInput
            value={search}
            clearLabel={t("common.clearSearch")}
            aria-label={t("birthdays.searchPlaceholder")}
            placeholder={t("birthdays.searchPlaceholder")}
            onChange={(event) => setSearch(event.target.value)}
            onClear={() => setSearch("")}
          />

          <CustomSelect
            value={month}
            options={monthOptions}
            className={styles.filter}
            ariaLabel={t("birthdays.filters.month")}
            onValueChange={setMonth}
            searchable
          />

          <Button
            type="button"
            variant="secondary"
            onClick={() => void refreshBirthdays()}
          >
            <FiRefreshCw aria-hidden="true" />
            {t("common.refresh")}
          </Button>
          <Button type="button" onClick={() => setIsCreating(true)}>
            <FiPlus aria-hidden="true" />
            {t("birthdays.actions.create")}
          </Button>
        </div>

        <Notice message={notice} onDismiss={() => setNotice(null)} />

        <div className={styles.tableShell}>
          {birthdaysLoading ? (
            <DataTableSkeleton columns={5} label={t("birthdays.loading")} />
          ) : birthdayError ? (
            <div className={styles.state}>{birthdayError}</div>
          ) : birthdays.length === 0 ? (
            <div className={styles.state}>{t("birthdays.empty")}</div>
          ) : (
            <BirthdaysTable
              birthdays={birthdays}
              onEdit={setEditing}
              onDelete={setDeleting}
            />
          )}
        </div>
      </section>

      <Modal
        open={isCreating || Boolean(editing)}
        title={
          editing ? t("birthdays.actions.edit") : t("birthdays.actions.create")
        }
        onClose={() => {
          setIsCreating(false)
          setEditing(null)
        }}
        footer={
          <>
            <Button
              type="button"
              variant="secondary"
              onClick={() => {
                setIsCreating(false)
                setEditing(null)
              }}
            >
              {t("common.cancel")}
            </Button>
            <Button type="submit" form={formId} disabled={birthdaysSaving}>
              {t("common.save")}
            </Button>
          </>
        }
      >
        <BirthdayForm
          birthday={editing}
          formId={formId}
          onSubmit={(payload) => void submitBirthday(payload)}
        />
      </Modal>

      <Modal
        open={Boolean(deleting)}
        title={t("birthdays.actions.delete")}
        onClose={() => setDeleting(null)}
        footer={
          <>
            <Button
              type="button"
              variant="secondary"
              onClick={() => setDeleting(null)}
            >
              {t("common.cancel")}
            </Button>
            <Button
              type="button"
              variant="danger"
              disabled={birthdaysSaving}
              onClick={() => deleting && void removeBirthday(deleting)}
            >
              {t("common.delete")}
            </Button>
          </>
        }
      >
        <p className={styles.confirmText}>
          {t("birthdays.deleteConfirmation", { name: deleting?.name })}
        </p>
      </Modal>
    </DashboardLayout>
  )
}
