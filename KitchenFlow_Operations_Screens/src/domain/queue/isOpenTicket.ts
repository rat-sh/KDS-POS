import type { TicketStatus } from "../core.types"

export function isOpenTicket(status: TicketStatus) {
  return ["NEW", "ACCEPTED", "COOKING", "IN_PROGRESS"].includes(status)
}
