import type { MenuItem } from "../../../domain/catalog.types"
import type { Order, Station } from "../../../domain/core.types"
import { isOpenTicket } from "../../../domain/queue"
import { ticketSla } from "../../../domain/sla"
import type { TicketSlaThresholds } from "../../../domain/sla"
import { elapsed } from "../../../domain/time"

type Props = { orders: Order[]; station: Station; items: MenuItem[]; now: number; slaThresholds: TicketSlaThresholds; onJump: (id: string) => void }

export function OverdueAlert({ orders, station, items, now, slaThresholds, onJump }: Props) {
  const overdue = orders.filter((order) => { const ticket = order.tickets.find((entry) => entry.station === station); return ticket && isOpenTicket(ticket.status) && ticketSla(order, station, items, now, slaThresholds).colour === "RED" })
  if (!overdue.length) return null
  return <aside className="kds-overdue-alert" aria-label="Late tickets"><strong>Late</strong>{overdue.slice(0, 3).map((order) => { const { targetSeconds } = ticketSla(order, station, items, now, slaThresholds); const arrival = order.paidAt ?? order.createdAt; return <button className="overdue-token" type="button" key={order.id} onClick={() => onJump(order.id)}>{order.token} +{elapsed(arrival + targetSeconds * 1000, now)}</button> })}{overdue.length > 3 && <small>+{overdue.length - 3} more</small>}</aside>
}
