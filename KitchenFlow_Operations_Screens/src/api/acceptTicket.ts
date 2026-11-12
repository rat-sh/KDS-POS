import { request } from "./request"

export async function acceptTicket(ticketId: string, expectedVersion?: number) {
  await request<void>(`/tickets/${ticketId}/accept`, {
    method: "POST",
    body: JSON.stringify({ expectedVersion }),
  })
}
