export function calculateAge(
  birthday: string,
  today: Date = new Date()
): number {
  const [birthYear, birthMonth, birthDay] = birthday.split("-").map(Number)

  let age = today.getFullYear() - birthYear
  const birthdayHasPassed =
    today.getMonth() + 1 > birthMonth ||
    (today.getMonth() + 1 === birthMonth && today.getDate() >= birthDay)

  if (!birthdayHasPassed) {
    age -= 1
  }

  return age
}
