import { request } from "./request"

export async function serveTicket(ticketId: string, expectedVersion?: number) {
  await request<void>(`/tickets/${ticketId}/serve`, {
    method: "POST",
    body: JSON.stringify({ expectedVersion }),
  })
}
