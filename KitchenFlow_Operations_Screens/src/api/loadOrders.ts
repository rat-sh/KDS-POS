import { readSession } from "../store/auth"
import { isRole } from "../domain/permissions"
import type { ApiOrderCollection } from "./api.types"
import { mapApiOrder } from "./mapApiOrder"
import { request } from "./request"
import type { Order } from "../domain/core.types"

export async function loadOrders(): Promise<Order[]> {
  const role = readSession()?.user.role
  if (role && isRole(role, "Admin")) return []
  const result = await request<ApiOrderCollection>(
    "/orders?page=0&size=100&sort=createdAt,desc",
  )
  const orders = Array.isArray(result) ? result : result.content
  return orders.map(mapApiOrder)
}
