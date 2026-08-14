export function ratingBadgeClass(rating: number | null): string {
  if (rating == null || !Number.isFinite(rating)) return ''
  if (rating <= 5) return 'badge-orange'
  if (rating <= 8) return 'badge-blue'
  return 'badge-green'
}
