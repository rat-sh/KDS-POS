import { useMemo, useState } from "react"
import type { Order, Station, Ticket } from "../../../domain/core.types"
import { cancelOrder, managerTicketAction, setOrderPriority, setOrderRush } from "../../../api"
import { refreshOrders } from "../../../store/orders"
import { actionMessage } from "../../../shared/logic"
import { ticketSla } from "../../../domain/sla"
import { useMenuStore } from "../../../store/catalog"
import { useSettingsStore } from "../../../store/settings"
import { useNow } from "../../../shared/hooks"
import { formatTicketTimer } from "../../../domain/time"

const reasons = ["Guest waiting", "Kitchen capacity", "Service recovery", "Safety concern"]
type Step = "START" | "DONE" | "SERVED"
const orderStatusLabels: Record<Order["status"], string> = { CREATED: "New", IN_KITCHEN: "In kitchen", READY: "Ready", CANCELLED: "Cancelled", COMPLETED: "Completed" }
const ticketStatusLabels: Record<Ticket["status"], string> = { NEW: "New", ACCEPTED: "Accepted", COOKING: "Cooking", READY: "Ready", SERVED: "Served", CANCELLED: "Cancelled", IN_PROGRESS: "Cooking", DONE: "Ready" }

export function ManagerBoard({ orders }: { orders: Order[] }) {
  const [selected, setSelected] = useState<Order | null>(null)
  const [station, setStation] = useState<Station>("")
  const [reason, setReason] = useState("")
  const [error, setError] = useState(""), [cancelIntent, setCancelIntent] = useState(false)
  const now = useNow(), { data } = useMenuStore(), settings = useSettingsStore(), items = data?.items ?? []
  const open = useMemo(() => orders.filter((order) => ["CREATED", "IN_KITCHEN", "READY"].includes(order.status)), [orders])
  const slaThresholds = { amberPercent: settings.amberPercent, redPercent: settings.redPercent }
  const overLimit = (order: Order) => order.tickets.some((ticket) => ["NEW", "ACCEPTED", "COOKING", "IN_PROGRESS"].includes(ticket.status) && ticketSla(order, ticket.station, items, now, slaThresholds).colour === "RED")
  const lateBy = (order: Order) => { const placedAt = order.paidAt ?? order.createdAt; const overdueAt = Math.min(...order.tickets.filter((ticket) => ["NEW", "ACCEPTED", "COOKING", "IN_PROGRESS"].includes(ticket.status)).map((ticket) => { const { colour, targetSeconds } = ticketSla(order, ticket.station, items, now, slaThresholds); return colour === "RED" ? placedAt + targetSeconds * 1000 : Infinity })); return formatTicketTimer(overdueAt, now) }
  const late = open.filter(overLimit)
  const waiting = open.filter((order) => order.status === "READY" && order.readyAt && now - order.readyAt > 5 * 60_000)
  const cancelled = orders.filter((order) => order.status === "CANCELLED" && order.tickets.some((ticket) => !ticket.acknowledgedAt)).length
  const attention = open.filter((order) => overLimit(order) || order.priority || order.rush || waiting.includes(order))
  const chosen = orders.find((order) => order.id === selected?.id)
  const chosenTicket = chosen?.tickets.find((ticket) => ticket.station === station)
  const step: Step | "" = chosenTicket && ["NEW", "ACCEPTED"].includes(chosenTicket.status) ? "START" : chosenTicket?.status === "COOKING" || chosenTicket?.status === "IN_PROGRESS" ? "DONE" : chosenTicket?.status === "READY" || chosenTicket?.status === "DONE" ? "SERVED" : ""
  const perform = async (action: () => Promise<void>) => { try { await action(); await refreshOrders(); setError("") } catch (cause) { setError(actionMessage(cause)); await refreshOrders() } }
  const updatePriority = () => { if (!chosen || !reason) return; void perform(() => setOrderPriority(chosen.id, !chosen.priority, reason, chosen.version)) }
  const toggleRush = (order: Order) => { void perform(() => setOrderRush(order.id, !order.rush, order.version)) }
  const cancel = () => { if (!chosen || !reason) return; void perform(async () => { await cancelOrder(chosen.id, reason, "Manager floor action", chosen.version); setSelected(null) }) }
  const stepIn = () => { if (!chosenTicket || !step || !reason) return; void perform(() => managerTicketAction(chosenTicket.id, step, reason, chosenTicket.version)) }
  const openDrawer = (order: Order, cancel = false) => { setSelected(order); setStation(order.tickets[0]?.station ?? ""); setReason(""); setError(""); setCancelIntent(cancel) }
  return <main className="light-screen workspace-screen operational-screen floor-screen"><header className="floor-header"><h1>FLOOR</h1><div className="floor-metrics">{[["OPEN ORDERS", open.length], ["Late", late.length], ["READY WAITING", open.filter((order) => order.status === "READY").length], ["CANCELLED PENDING", cancelled]].map(([label, count]) => <div key={label}><strong>{count}</strong><span>{label}</span></div>)}</div></header>
    <section className="attention-section"><h2>NEEDS ATTENTION</h2>{attention.length ? <div className="attention-list">{attention.map((order) => <article className="attention-row" key={order.id}>
      <strong>#{order.token}{order.table && ` · Table ${order.table}`}</strong>
      <span className={`attention-row-status ${overLimit(order) ? "late" : ""}`}>{overLimit(order) ? `Late +${lateBy(order)}` : waiting.includes(order) ? "Ready waiting" : "Rush order"}</span>
      <span className="attention-row-stations">{[...new Set(order.tickets.map((ticket) => ticket.station))].join(" · ")}</span>
      <div className="attention-row-actions"><button type="button" onClick={() => toggleRush(order)}>{order.rush ? "Clear rush" : "Rush"}</button><button type="button" onClick={() => openDrawer(order)}>Open</button><button type="button" onClick={() => openDrawer(order, true)}>Cancel…</button></div>
    </article>)}</div> : <p>All open orders are within their service limits.</p>}</section>
    <details className="all-open-orders"><summary>ALL OPEN ORDERS · {open.length}</summary><div className="floor-table"><div className="floor-table-row floor-table-head"><span>TOKEN</span><span>TABLE</span><span>STATUS</span><span>TYPE</span><span>STATIONS</span></div>{open.map((order) => <div className="floor-table-row" key={order.id}><strong>#{order.token}</strong><span>{order.table ?? "—"}</span><span>{orderStatusLabels[order.status]}</span><span>{order.type.replace("_", " ")}</span><span>{order.tickets.map((ticket) => `${ticket.station} ${ticketStatusLabels[ticket.status]}`).join(" · ")}</span></div>)}</div></details>
    {selected && chosen && <div className="floor-drawer-backdrop" onClick={() => setSelected(null)}><aside className="floor-drawer" role="dialog" aria-modal="true" aria-labelledby="floor-drawer-title" onClick={(event) => event.stopPropagation()}><header><h2 id="floor-drawer-title">ORDER #{chosen.token}</h2><button type="button" onClick={() => setSelected(null)}>CLOSE</button></header><p>{chosen.table ? `TABLE ${chosen.table}` : chosen.type.replace("_", " ")} · {orderStatusLabels[chosen.status]}</p><section className="floor-tickets">{chosen.tickets.map((ticket) => <button type="button" key={ticket.id} className={station === ticket.station ? "selected" : ""} onClick={() => setStation(ticket.station)}><b>{ticket.station}</b><span>{ticketStatusLabels[ticket.status]}</span></button>)}</section>
      <label>REASON<select autoFocus={cancelIntent} value={reason} onChange={(event) => setReason(event.target.value)}><option value="">Choose a reason</option>{reasons.map((entry) => <option key={entry}>{entry}</option>)}</select></label>{!reason && <small className="action-reason">Choose a reason before using a floor action.</small>}{error && <p role="alert">{error}</p>}
      <div className="floor-actions"><button type="button" disabled={!reason} onClick={updatePriority}>{chosen.priority ? "CLEAR PRIORITY" : "SET PRIORITY"}</button><button type="button" disabled={!reason || chosen.status === "CANCELLED"} onClick={cancel}>CANCEL ORDER</button><button type="button" disabled={!reason || !step} onClick={stepIn}>{step ? `STEP IN · ${step}` : "STEP IN UNAVAILABLE"}</button></div>
    </aside></div>}
  </main>
}
