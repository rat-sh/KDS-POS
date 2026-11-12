import type { MenuItem } from "../../../domain/catalog.types"
import type { Order, Station } from "../../../domain/core.types"
import { elapsed } from "../../../domain/time"

export function averageStationPrepTime(orders: Order[], station: Station, items: MenuItem[], now: number) {
  const start = new Date(now); start.setHours(4, 0, 0, 0); if (now < start.getTime()) start.setDate(start.getDate() - 1)
  const completed = orders.flatMap((order) => order.tickets.filter((ticket) => ticket.station === station && ["READY", "DONE"].includes(ticket.status) && ticket.completedAt && ticket.completedAt >= start.getTime() && order.lines.some((line) => items.find((item) => item.id === line.itemId)?.stations.includes(station))).map((ticket) => ticket.completedAt! - (order.paidAt ?? order.createdAt)))
  if (!completed.length) return "--:--"
  return elapsed(0, completed.reduce((sum, value) => sum + value, 0) / completed.length)
}
