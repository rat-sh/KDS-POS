import { request } from "./request"

export async function adjustStock(itemId: string, quantityDelta: number, reason: string, expectedVersion?: number) {
  await request<void>(`/items/${itemId}/stock-adjustments`, {
    method: "POST",
    body: JSON.stringify({ quantityDelta, reason, expectedVersion }),
  })
}
