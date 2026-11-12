import { request } from "./request"

export async function managerTicketAction(ticketId: string, step: "START" | "DONE" | "SERVED", reason: string, expectedVersion?: number) {
  await request<void>(`/tickets/${ticketId}/manager-action`, {
    method: "POST",
    body: JSON.stringify({ step, reason, expectedVersion }),
  })
}
