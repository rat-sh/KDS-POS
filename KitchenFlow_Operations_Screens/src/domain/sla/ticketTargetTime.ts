import type { MenuItem } from "../catalog.types"
import type { Order, Station } from "../core.types"
export function ticketTargetTime(order: Order, station: Station, items: MenuItem[]) { return Math.max(0, ...order.lines.flatMap((line) => { const item = items.find((entry) => entry.id === line.itemId); return item?.stations.includes(station) ? [item.prepSeconds] : [] })) }
