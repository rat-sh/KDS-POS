import type { MenuItem, Modifier } from "../../../domain/catalog.types"
import type { Order, Station } from "../../../domain/core.types"

type Props = { order?: Order; station: Station; items: MenuItem[]; modifiers: Modifier[] }

export function StationPrintTicket({ order, station, items, modifiers }: Props) {
  if (!order) return null
  const lines = order.lines.filter((line) => items.find((item) => item.id === line.itemId)?.stations.includes(station))
  const placed = new Date(order.paidAt ?? order.createdAt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: false })
  const ticket = order.tickets.find((entry) => entry.station === station)
  const accepted = ticket?.acceptedAt ? new Date(ticket.acceptedAt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: false }) : placed
  return <article className="kds-print-ticket"><b>{station.toUpperCase()} · TOKEN {order.token}</b>{order.table && <b>TABLE {order.table}</b>}<span>{order.type.replace("_", " ")}</span>{order.priority && <b>PRIORITY</b>}{order.vip && <b>VIP</b>}<span>#{order.id}</span><span>ACCEPTED {accepted}</span>{order.note && <p>{order.note}</p>}<hr />{lines.map((line, index) => <section key={`${line.itemId}-${index}`}><b>{line.qty}x {items.find((item) => item.id === line.itemId)?.name ?? line.itemId}</b>{line.modifierIds.map((id) => { const modifier = modifiers.find((entry) => entry.id === id); return modifier ? <span key={id}>{modifier.name.toLowerCase().startsWith("no ") ? `(x) ${modifier.name.toUpperCase()}` : `+ ${modifier.name.toUpperCase()}`}</span> : null })}</section>)}<hr /></article>
}
