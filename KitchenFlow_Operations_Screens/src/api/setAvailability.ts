import { request } from "./request"
export async function setAvailability(itemId: string, available: boolean, expectedVersion?: number) { await request<void>(`/items/${itemId}/availability`, { method: "PATCH", body: JSON.stringify({ available, expectedVersion }) }) }

