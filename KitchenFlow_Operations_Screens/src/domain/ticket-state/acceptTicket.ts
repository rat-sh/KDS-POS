import type { Order, Station } from "../core.types"

export function acceptTicket(order: Order, station: Station, at: number) {
  return { ...order, tickets: order.tickets.map((ticket) => ticket.station === station ? { ...ticket, status: "ACCEPTED" as const, acceptedAt: at } : ticket) }
}
