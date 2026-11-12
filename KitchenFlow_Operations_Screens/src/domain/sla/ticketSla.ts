import type { MenuItem } from "../catalog.types"
import type { Order, Station } from "../core.types"
import { ticketTargetTime } from "./ticketTargetTime"

export type TicketSlaThresholds = { amberPercent: number; redPercent: number }
export const defaultTicketSlaThresholds: TicketSlaThresholds = { amberPercent: 80, redPercent: 100 }

export function ticketSla(order: Order, station: Station, items: MenuItem[], now: number, { amberPercent, redPercent }: TicketSlaThresholds) {
  const targetSeconds = ticketTargetTime(order, station, items)
  const elapsedSeconds = Math.max(0, now - (order.paidAt ?? order.createdAt)) / 1000
  const percent = targetSeconds ? elapsedSeconds / targetSeconds * 100 : 0
  const colour = percent < amberPercent ? "GREEN" : percent <= redPercent ? "AMBER" : "RED"
  return { percent, colour, targetSeconds }
}
