import type { Order } from "../core.types"
export function acknowledgeCancelled(order: Order, station: Order["tickets"][number]["station"]) { return { ...order, tickets: order.tickets.map((ticket) => ticket.station === station ? { ...ticket, acknowledgedAt: Date.now() } : ticket) } }
