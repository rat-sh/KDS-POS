import { request } from "./request"
export async function completeOrder(orderId: string, expectedVersion?: number) { await request<void>(`/orders/${orderId}/complete`, { method: "POST", body: JSON.stringify({ expectedVersion }) }) }

