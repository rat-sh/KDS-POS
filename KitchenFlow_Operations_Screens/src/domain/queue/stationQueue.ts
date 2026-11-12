import type { MenuItem } from "../catalog.types"
import type { Order, Station } from "../core.types"
import { isOpenTicket } from "./isOpenTicket"
import { ticketQueuePriority } from "./ticketQueuePriority"
import type { TicketSlaThresholds } from "../sla"

export function stationQueue(orders: Order[], station: Station, items: MenuItem[], now: number, slaThresholds?: TicketSlaThresholds) {
  return orders.filter((order) => {
    const ticket = order.tickets.find((entry) => entry.station === station)
    return (ticket?.status === "CANCELLED" && !ticket.acknowledgedAt || ticket && isOpenTicket(ticket.status)) && order.lines.some((line) => items.find((item) => item.id === line.itemId)?.stations.includes(station))
  }).sort((a, b) => {
    const ticketA = a.tickets.find((ticket) => ticket.station === station)!
    const ticketB = b.tickets.find((ticket) => ticket.station === station)!
    return ticketQueuePriority(a, station, items, now, ticketA.status, slaThresholds) - ticketQueuePriority(b, station, items, now, ticketB.status, slaThresholds) || (a.paidAt || a.createdAt) - (b.paidAt || b.createdAt)
  })
}
