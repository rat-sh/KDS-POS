import { request } from "./request"

export async function deleteMenuItem(itemId: string) {
  await request<void>(`/admin/items/${itemId}`, { method: "DELETE" })
}
