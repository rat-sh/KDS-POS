import { loadOrders } from "../../api"
import type { Order } from "../../domain/core.types"
import { setOrders } from "./setOrders"

let pending: Promise<Order[]> | null = null

export function refreshOrders(force = false): Promise<Order[]> {
  if (pending) return force ? pending.then((): Promise<Order[]> => refreshOrders(true)) : pending
  pending = loadOrders().then((orders) => { setOrders(orders); return orders }).finally(() => { pending = null })
  return pending
}
