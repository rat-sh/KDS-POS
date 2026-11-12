import type { Order } from "../core.types"
export function flagRush(order: Order) { return { ...order, rush: !order.rush } }
