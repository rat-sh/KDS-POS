import type { Order } from "../core.types"
export function completeOrder(order: Order, at: number) { return { ...order, status: "COMPLETED" as const, completedAt: at } }
