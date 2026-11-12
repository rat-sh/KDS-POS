import type { CSSProperties } from "react"
import type { MenuItem, Modifier } from "../../../domain/catalog.types"
import type { Order, Station } from "../../../domain/core.types"
import { ticketSla } from "../../../domain/sla"
import type { TicketSlaThresholds } from "../../../domain/sla"
import { formatTicketTimer } from "../../../domain/time"
import { Icon } from "../../../shared/ui/Icon"

type Action = "accept" | "start" | "done" | "ack"
type Props = { order: Order; station: Station; now: number; menuItems: MenuItem[]; modifiers: Modifier[]; slaThresholds: TicketSlaThresholds; bumpDisabled?: boolean; selected?: boolean; measure?: boolean; large?: boolean; cardHeight?: number; onSelect?: (order: Order) => void; onAction?: (order: Order, action: Action) => void; onPrint?: (order: Order) => void }

export function TicketCard({ order, station, now, menuItems, modifiers, slaThresholds, bumpDisabled = false, selected = false, measure = false, large = false, cardHeight, onSelect, onAction, onPrint }: Props) {
  const ticket = order.tickets.find((entry) => entry.station === station)
  if (!ticket) return null
  if (ticket.status === "CANCELLED") return <article className="ticket-cancelled-strip"><strong>#{order.token}</strong><span title={order.cancellation?.reason ?? ticket.cancelledReason ?? "Order cancelled"}>CANCELLED</span><button type="button" onClick={() => onAction?.(order, "ack")}>ACKNOWLEDGE</button></article>
  const lines = order.lines.filter((line) => menuItems.find((item) => item.id === line.itemId)?.stations.includes(station))
  const sla = ticketSla(order, station, menuItems, now, slaThresholds)
  const stage = sla.colour === "GREEN" ? "fresh" : sla.colour === "AMBER" ? "watch" : "over-limit"
  const placedAt = order.paidAt ?? order.createdAt
  const timer = formatTicketTimer(placedAt, now)
  const target = formatTicketTimer(now - sla.targetSeconds * 1000, now)
  const color = stage === "fresh" ? "#D7EBDD" : stage === "watch" ? "#F4E9BB" : "#E9A9A4"
  const stageLabel = stage === "fresh" ? "ON TIME" : stage === "watch" ? "DUE" : "LATE"
  const stageIcon = stage === "fresh" ? "check" : stage === "watch" ? "clock" : "alert"
  const status = ticket.status === "IN_PROGRESS" ? "COOKING" : ticket.status
  const action: Action = status === "NEW" ? "accept" : status === "ACCEPTED" ? "start" : "done"
  const label = { accept: "ACCEPT", start: "START COOKING", done: "DONE", ack: "ACKNOWLEDGE" }[action]
  return <article data-order-id={order.id} className={`ticket-card stage-${stage} ${large ? "ticket-large" : ""} ${selected ? "ticket-selected" : ""} ticket-${status.toLowerCase()}`} style={{ "--stage-color": color, "--ticket-height": cardHeight ? `${cardHeight}px` : undefined } as CSSProperties} onClick={measure ? undefined : () => onSelect?.(order)}>
    <header className="ticket-band" style={{ backgroundColor: color }}><div className="ticket-band-top"><button type="button" className="band-token" aria-pressed={selected} disabled={measure} onClick={(event) => { event.stopPropagation(); onSelect?.(order) }}>#{order.token}</button><strong className="ticket-timer">{timer} / {target}</strong><button type="button" className="ticket-print" aria-label="Print ticket" title="Print ticket" onClick={(event) => { event.stopPropagation(); onPrint?.(order) }}><Icon name="print" size={22} /></button></div>
      <div className="ticket-band-bottom">{order.table && <b>TABLE {order.table}</b>}<span>{order.type.replace("_", " ")}</span>{order.priority && <b className="rush-chip">PRIORITY · {order.priorityReason ?? "NO REASON"}</b>}<b className="ticket-state">{status}</b><span className="stage-word"><Icon name={stageIcon} size={18} />{stageLabel}</span></div></header>
    {order.note && <aside className={`ticket-note ${order.note.trim().toUpperCase().startsWith("ALLERGY:") ? "allergy" : ""}`}>{order.note.trim().toUpperCase().startsWith("ALLERGY:") && <b>ALLERGY</b>}{order.note.trim().replace(/^ALLERGY:\s*/i, "")}</aside>}
    <div className="ticket-items">{lines.map((line, index) => <section className="ticket-item" key={`${line.itemId}-${index}`}><b className="item-qty">{line.qty}×</b><div className="item-details"><strong>{menuItems.find((item) => item.id === line.itemId)?.name ?? line.itemId}</strong>{line.modifierIds.map((id) => { const modifier = modifiers.find((entry) => entry.id === id); return modifier ? <span className={`ticket-modifier ${modifier.name.toLowerCase().startsWith("no ") ? "removal" : ""}`} key={id}>{modifier.name.toLowerCase().startsWith("no ") ? "⊘ " : "+ "}{modifier.name.toUpperCase()}</span> : null })}</div></section>)}</div>
    <footer className="ticket-footer"><span>#{order.id}</span><button type="button" className="ticket-action" disabled={bumpDisabled && action === "done"} onClick={(event) => { event.stopPropagation(); onAction?.(order, action) }}>{label}</button></footer>
  </article>
}
