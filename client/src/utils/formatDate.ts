export function formatBooked(createdAt: string): string {
  const d = new Date(createdAt)
  const day = String(d.getDate()).padStart(2, '0')
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${day}-${month}-${d.getFullYear()}, ${hours}:${minutes}`
}
