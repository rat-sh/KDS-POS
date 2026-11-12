import type { Order } from "../core.types"
export function startTicket(order: Order, station: Order["tickets"][number]["station"], at: number) { return { ...order, tickets: order.tickets.map((ticket) => ticket.station === station ? { ...ticket, status: "COOKING" as const, acceptedAt: at, startedAt: at } : ticket) } }
