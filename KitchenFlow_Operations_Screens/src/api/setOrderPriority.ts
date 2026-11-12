import { request } from "./request"

export async function setOrderPriority(orderId: string, priority: boolean, reason: string, expectedVersion?: number) {
  await request<void>(`/orders/${orderId}/priority`, {
    method: "PATCH",
    body: JSON.stringify({ priority, reason, expectedVersion }),
  })
}
