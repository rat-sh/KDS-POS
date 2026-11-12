import { request } from "./request"
export async function recallTicket(ticketId: string, expectedVersion?: number) { await request<void>(`/tickets/${ticketId}/recall`, { method: "POST", body: JSON.stringify({ expectedVersion }) }) }

