import type { MenuItem } from "../catalog.types"
import type { Order, Station, TicketStatus } from "../core.types"
import { defaultTicketSlaThresholds, ticketSla } from "../sla"
import type { TicketSlaThresholds } from "../sla"

export function ticketQueuePriority(order: Order, station: Station, items: MenuItem[], now: number, status: TicketStatus, slaThresholds: TicketSlaThresholds = defaultTicketSlaThresholds) {
  if (status === "CANCELLED") return 0
  if (ticketSla(order, station, items, now, slaThresholds).colour === "RED") return 1
  if (order.rush) return 2
  return 3
}
