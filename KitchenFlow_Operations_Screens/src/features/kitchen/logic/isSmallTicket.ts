import type { MenuItem, Modifier } from "../../../domain/catalog.types"
import type { Order, Station } from "../../../domain/core.types"

export function isSmallTicket(order: Order, station: Station, items: MenuItem[], modifiers: Modifier[], maxHeight = 460) {
  const lines = order.lines.filter((line) => items.find((item) => item.id === line.itemId)?.stations.includes(station))
  const itemHeight = lines.reduce((height, line) => {
    const item = items.find((entry) => entry.id === line.itemId)
    const modifierHeight = line.modifierIds.reduce((sum, id) => sum + (modifiers.some((modifier) => modifier.id === id) ? 19 : 0), 0)
    return height + 48 + Math.ceil((item?.name.length ?? 20) / 23) * 21 + modifierHeight
  }, 0)
  const noteHeight = order.note ? 34 + Math.ceil(order.note.length / 42) * 20 : 0
  return 90 + itemHeight + noteHeight + 80 <= maxHeight
}
