import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { Birthday, BirthdayPayload } from '@/types/birthday'
import styles from './BirthdayForm.module.css'

type BirthdayFormProps = {
  birthday?: Birthday | null
  formId: string
  onSubmit: (payload: BirthdayPayload) => void
}

export function BirthdayForm({ birthday, formId, onSubmit }: BirthdayFormProps) {
  const { t } = useTranslation()
  const initialValue = useMemo<BirthdayPayload>(
    () => ({
      userId: birthday?.userId ?? '',
      name: birthday?.name ?? '',
      birthday: birthday?.birthday ?? '',
      zoneHours: birthday?.zoneHours ?? -3,
    }),
    [birthday],
  )
  const [value, setValue] = useState(initialValue)

  return (
    <form
      className={styles.form}
      id={formId}
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit({
          ...value,
          userId: value.userId.trim(),
          name: value.name.trim(),
        })
      }}
    >
      <label>
        <span>{t('birthdays.fields.userId')}</span>
        <input
          inputMode="numeric"
          pattern="[0-9]+"
          required
          value={value.userId}
          disabled={Boolean(birthday)}
          onChange={(event) => setValue((current) => ({ ...current, userId: event.target.value }))}
        />
      </label>
      <label>
        <span>{t('birthdays.fields.name')}</span>
        <input
          maxLength={255}
          required
          value={value.name}
          onChange={(event) => setValue((current) => ({ ...current, name: event.target.value }))}
        />
      </label>
      <label>
        <span>{t('birthdays.fields.birthday')}</span>
        <input
          required
          type="date"
          value={value.birthday}
          onChange={(event) => setValue((current) => ({ ...current, birthday: event.target.value }))}
        />
      </label>
      <label>
        <span>{t('birthdays.fields.zoneHours')}</span>
        <input
          max={14}
          min={-12}
          required
          type="number"
          value={value.zoneHours}
          onChange={(event) => setValue((current) => ({ ...current, zoneHours: Number(event.target.value) }))}
        />
      </label>
    </form>
  )
}
