export function businessDayKey(at: number) {
  const date = new Date(at)
  if (date.getHours() < 4) date.setDate(date.getDate() - 1)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}
