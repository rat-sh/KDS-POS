import type { MenuItem } from "../../../domain/catalog.types"
import type { Order, Station } from "../../../domain/core.types"
import { isOpenTicket } from "../../../domain/queue"

type Props = { items: MenuItem[]; orders: Order[]; station: Station; focusedItem: string; onFocus: (id: string) => void; now?: number }

export function StationInventory({ items, orders, station, focusedItem, onFocus, now = Date.now() }: Props) {
  const businessStart = new Date(now); businessStart.setHours(4, 0, 0, 0); if (now < businessStart.getTime()) businessStart.setDate(businessStart.getDate() - 1)
  const today = orders.filter((order) => (order.paidAt ?? order.createdAt) >= businessStart.getTime() && (order.paidAt ?? order.createdAt) <= now)
  const tickets = today.flatMap((order) => { const ticket = order.tickets.find((entry) => entry.station === station); return ticket && ticket.status !== "CANCELLED" ? [{ order, ticket }] : [] })
  const units = (itemId: string, openOnly = false) => tickets.filter(({ ticket }) => !openOnly || isOpenTicket(ticket.status)).reduce((sum, { order }) => sum + order.lines.filter((line) => line.itemId === itemId).reduce((qty, line) => qty + line.qty, 0), 0)
  const categoryNames = [...new Set(items.map((item) => item.category))].sort()
  const groups = categoryNames.map((category) => ({ name: category.toUpperCase(), items: items.filter((item) => item.category === category && item.stations.includes(station)) })).filter((group) => group.items.length)
  const totalUnits = tickets.reduce((sum, { order }) => sum + order.lines.filter((line) => items.find((item) => item.id === line.itemId)?.stations.includes(station)).reduce((qty, line) => qty + line.qty, 0), 0)
  const todayLabel = new Date(now).toLocaleDateString("en-IN", { day: "2-digit", month: "short" }).toUpperCase()
  return <aside className="kds-inventory" aria-label="Today's station order book"><header><strong>{station.toUpperCase()} · {todayLabel}</strong></header><div className="inventory-totals"><span>Today: {tickets.length} tickets · {totalUnits} units</span></div>{groups.map((group) => <section className="kds-inventory-group" key={group.name}><h2>{group.name}</h2>{group.items.map((item) => { const qty = units(item.id), queued = units(item.id, true), active = focusedItem === item.id; return <button type="button" className={`inventory-row ${qty ? "has-qty" : "zero"} ${active ? "focused" : ""}`} key={item.id} onClick={() => onFocus(active ? "" : item.id)}><span>{item.name}<small>{queued} open</small></span><b>{qty}</b></button> })}</section>)}</aside>
}
