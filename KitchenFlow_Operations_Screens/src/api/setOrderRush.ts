import { request } from "./request"

export async function setOrderRush(orderId: string, rush: boolean, expectedVersion?: number) {
  await request<void>(`/orders/${orderId}/rush`, {
    method: "PATCH",
    body: JSON.stringify({ rush, expectedVersion }),
  })
}
