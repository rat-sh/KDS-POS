import type { Order, Station } from "../core.types"

export function recordTransition(previous: Order, next: Order, station: Station, actor: string, time: number, reason?: string) {
  const before = previous.tickets.find((ticket) => ticket.station === station)
  const after = next.tickets.find((ticket) => ticket.station === station)
  if (!before || !after || before.status === after.status) return next
  const event = { ticket: after.id, order: next.id, token: next.token, table: next.table, station, from: before.status, to: after.status, actor, time, reason }
  return { ...next, events: [...(previous.events ?? []), event] }
}
