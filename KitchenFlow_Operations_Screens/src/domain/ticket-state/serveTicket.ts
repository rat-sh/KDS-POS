import type { Order, Station } from "../core.types"

export function serveTicket(order: Order, station: Station, at: number) {
  const tickets = order.tickets.map((ticket) => ticket.station === station ? { ...ticket, status: "SERVED" as const, servedAt: at } : ticket)
  const active = tickets.filter((ticket) => ticket.status !== "CANCELLED")
  const served = active.every((ticket) => ticket.status === "SERVED")
  const ready = active.every((ticket) => ticket.status === "READY" || ticket.status === "DONE" || ticket.status === "SERVED")
  return { ...order, status: served ? "COMPLETED" as const : ready ? "READY" as const : "IN_KITCHEN" as const, completedAt: served ? at : order.completedAt, tickets }
}
