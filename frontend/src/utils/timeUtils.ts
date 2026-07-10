import utc from "dayjs/plugin/utc"
import dayjs from "dayjs"
import isToday from "dayjs/plugin/isToday"
import duration from "dayjs/plugin/duration"
import timezone from "dayjs/plugin/timezone"
import isYesterday from "dayjs/plugin/isYesterday"

dayjs.extend(utc)
dayjs.extend(isToday)
dayjs.extend(duration)
dayjs.extend(timezone)
dayjs.extend(isYesterday)

const MONTHS = [
  "Jan",
  "Fev",
  "Mar",
  "Abr",
  "Mai",
  "Jun",
  "Jul",
  "Ago",
  "Set",
  "Out",
  "Nov",
  "Dez"
]

export function formatLocalTimestamp(date: string): string {
  const d = dayjs.utc(date).local()
  const time = d.format("HH:mm")

  if (d.isToday()) {
    return `Hoje, às ${time}`
  }

  if (d.isYesterday()) {
    return `Ontem, às ${time}`
  }

  const day = d.date()
  const month = MONTHS[d.month()]
  const year = d.year()

  return `${day} de ${month}. de ${year}, às ${time}`
}
