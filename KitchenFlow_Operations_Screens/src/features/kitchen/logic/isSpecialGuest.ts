import type { Order } from "../../../domain/core.types"

export function isSpecialGuest(order: Order) {
  return /\b(vip|special guest)\b/i.test(order.note ?? "")
}
