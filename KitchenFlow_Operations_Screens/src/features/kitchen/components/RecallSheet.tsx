import type { Order, Station } from "../../../domain/core.types"
import type { MenuItem } from "../../../domain/catalog.types"
import { remainingTime } from "../../../domain/time"

export function RecallSheet({ orders, station, now, items, onRecall, onClose }: { orders: Order[]; station: Station; now: number; items: MenuItem[]; onRecall: (order: Order) => void; onClose: () => void }) {
  const recent = orders.filter((order) => order.status !== "COMPLETED" && order.status !== "CANCELLED" && order.lines.some((line) => items.find((item) => item.id === line.itemId)?.stations.includes(station)) && order.tickets.some((ticket) => ticket.station === station && ["READY", "DONE"].includes(ticket.status) && !!ticket.completedAt && now - ticket.completedAt < 600_000))
  if (!recent.length) return <div className="kds-sheet-backdrop" onClick={onClose}><section className="kds-recall-sheet" onClick={(event) => event.stopPropagation()}><header><strong>RECALL TICKETS</strong><button onClick={onClose}>CLOSE</button></header><p>No tickets in the 10-minute recall window.</p></section></div>
  return <div className="kds-sheet-backdrop" onClick={onClose}><section className="kds-recall-sheet" onClick={(event) => event.stopPropagation()}><header><strong>RECALL TICKETS</strong><button onClick={onClose}>CLOSE</button></header>{recent.map((order) => { const ticket = order.tickets.find((entry) => entry.station === station)!; return <div className="recall-row" key={ticket.id}><b>{order.token}</b><span>{remainingTime(ticket.completedAt! + 600_000, now)}</span><button onClick={() => { onRecall(order); onClose() }}>RECALL</button></div> })}</section></div>
}
