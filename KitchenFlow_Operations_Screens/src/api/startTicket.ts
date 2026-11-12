import { request } from "./request"
export async function startTicket(ticketId: string, expectedVersion?: number) { await request<void>(`/tickets/${ticketId}/start`, { method: "POST", body: JSON.stringify({ expectedVersion }) }) }

