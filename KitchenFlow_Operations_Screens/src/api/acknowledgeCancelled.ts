import { request } from "./request"
export async function acknowledgeCancelled(ticketId: string, expectedVersion?: number) { await request<void>(`/tickets/${ticketId}/acknowledge-cancel`, { method: "POST", body: JSON.stringify({ expectedVersion }) }) }
