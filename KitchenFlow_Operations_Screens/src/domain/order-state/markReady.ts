import type { Order } from "../core.types"
export function markReady(order: Order, at: number) { return { ...order, status: "READY" as const, readyAt: at } }
