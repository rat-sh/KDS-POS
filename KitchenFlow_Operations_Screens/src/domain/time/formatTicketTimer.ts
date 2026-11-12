import { elapsed } from "./elapsed"

export function formatTicketTimer(placedAt: number, now: number) {
  return elapsed(placedAt, now)
}
