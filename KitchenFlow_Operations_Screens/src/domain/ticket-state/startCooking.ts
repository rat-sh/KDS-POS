import type { Order, Station } from "../core.types"

export function startCooking(order: Order, station: Station, at: number) { const undo = order.tickets.some((ticket) => ticket.station === station && ["DONE", "READY"].includes(ticket.status)); return { ...order, status: undo ? "IN_KITCHEN" as const : order.status, readyAt: undo ? undefined : order.readyAt, tickets: order.tickets.map((ticket) => ticket.station === station ? { ...ticket, status: "COOKING" as const, startedAt: at, completedAt: undefined } : ticket) } }
