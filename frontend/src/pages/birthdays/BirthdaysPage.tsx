import { useEffect, useMemo, useState } from "react"
import { useTranslation } from "react-i18next"
import {
  FiEdit2,
  FiPlus,
  FiRefreshCw,
  FiSearch,
  FiTrash2
} from "react-icons/fi"
import { DashboardLayout } from "@/components/layout/DashboardLayout"
import { Button } from "@/components/ui/Button"
import { Modal } from "@/components/ui/Modal"
import { useBirthdaysStore } from "@/stores/useBirthdaysStore"
import type { Birthday, BirthdayPayload } from "@/types/birthday"
import { BirthdayForm } from "./BirthdayForm"
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

  const query = useMemo(() => ({ search, month }), [month, search])

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
          <label className={styles.search}>
            <FiSearch aria-hidden="true" />
            <input
              value={search}
              placeholder={t("birthdays.searchPlaceholder")}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>

          <select
            value={month}
            onChange={(event) => setMonth(event.target.value)}
            aria-label={t("birthdays.filters.month")}
          >
            {months.map((value) => (
              <option value={value} key={value}>
                {value === "all"
                  ? t("birthdays.filters.allMonths")
                  : t(`months.${value}`)}
              </option>
            ))}
          </select>

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

        {notice && (
          <div className={styles.notice} role="status">
            <span>{notice}</span>
            <button type="button" onClick={() => setNotice(null)}>
              {t("common.dismiss")}
            </button>
          </div>
        )}

        <div className={styles.tableShell}>
          {birthdaysLoading ? (
            <div className={styles.state}>{t("birthdays.loading")}</div>
          ) : birthdayError ? (
            <div className={styles.state}>{birthdayError}</div>
          ) : birthdays.length === 0 ? (
            <div className={styles.state}>{t("birthdays.empty")}</div>
          ) : (
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>{t("birthdays.fields.name")}</th>
                  <th>{t("birthdays.fields.userId")}</th>
                  <th>{t("birthdays.fields.birthday")}</th>
                  <th>{t("birthdays.fields.zoneHours")}</th>
                  <th aria-label={t("common.actions")} />
                </tr>
              </thead>
              <tbody>
                {birthdays.map((birthday) => (
                  <tr key={birthday.user_id}>
                    <td>
                      <strong>{birthday.name}</strong>
                    </td>
                    <td className={styles.monospace}>{birthday.user_id}</td>
                    <td>{formatBirthday(birthday.birthday)}</td>
                    <td>{formatZone(birthday.zone_hours)}</td>
                    <td>
                      <div className={styles.rowActions}>
                        <button
                          type="button"
                          onClick={() => setEditing(birthday)}
                          title={t("birthdays.actions.edit")}
                        >
                          <FiEdit2 aria-hidden="true" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleting(birthday)}
                          title={t("birthdays.actions.delete")}
                        >
                          <FiTrash2 aria-hidden="true" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
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

function formatBirthday(value: string): string {
  const [year, month, day] = value.split("-")
  return `${day}/${month}/${year}`
}

function formatZone(value: number): string {
  return value >= 0 ? `UTC+${value}` : `UTC${value}`
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
