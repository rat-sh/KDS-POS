import { request } from "./request"
export async function bumpTicket(ticketId: string, expectedVersion?: number) { await request<void>(`/tickets/${ticketId}/bump`, { method: "POST", body: JSON.stringify({ expectedVersion }) }) }

