import type { Order } from "../core.types"
export function cancelOrder(order: Order, reason: string, detail: string, actor: string, at: number) { return { ...order, status: "CANCELLED" as const, cancellation: { reason, detail, actor, cancelledAt: at }, tickets: order.tickets.map((ticket) => ({ ...ticket, status: "CANCELLED" as const, cancelledReason: reason })) } }
