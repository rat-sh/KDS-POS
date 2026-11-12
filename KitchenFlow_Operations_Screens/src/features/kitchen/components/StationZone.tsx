import { useEffect, useRef } from "react"
import type { MenuItem, Modifier } from "../../../domain/catalog.types"
import type { Order, Station } from "../../../domain/core.types"
import type { TicketSlaThresholds } from "../../../domain/sla"
import { TicketCard } from "./TicketCard"

type Action = "accept" | "start" | "done" | "ack"
type Props = { name: "SMALL" | "LARGE"; orders: Order[]; station: Station; now: number; items: MenuItem[]; modifiers: Modifier[]; slaThresholds: TicketSlaThresholds; bumpDisabled: boolean; selectedId: string; focusedItem: string; jumpId?: string; onSelect: (order: Order) => void; onAction: (order: Order, action: Action) => void; onPrint: (order: Order) => void }

export function StationZone({ name, orders, station, now, items, modifiers, slaThresholds, bumpDisabled, selectedId, focusedItem, jumpId, onSelect, onAction, onPrint }: Props) {
  const large = name === "LARGE"
  const laneMin = large ? 400 : 270
  const areaRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!jumpId) return
    areaRef.current?.querySelector(`[data-order-id="${CSS.escape(jumpId)}"]`)?.scrollIntoView({ block: "nearest" })
  }, [jumpId, orders])
  return <section className={`station-zone ${large ? "large-zone" : "small-zone"}`}>
    <header className="zone-heading"><strong>{name} ORDERS · {orders.length}</strong></header>
    <div className="zone-area" ref={areaRef}>
      {orders.length ? <div className="kds-ticket-list" style={{ gridTemplateColumns: `repeat(auto-fill, minmax(min(100%, ${laneMin}px), 1fr))` }}>{orders.map((order) => {
        const focused = focusedItem && order.lines.some((line) => line.itemId === focusedItem)
        return <div className={`ticket-focus-shell ${focusedItem && !focused ? "dimmed" : ""} ${focused ? "item-focused" : ""}`} key={order.id}>
          <TicketCard order={order} station={station} now={now} menuItems={items} modifiers={modifiers} slaThresholds={slaThresholds} bumpDisabled={bumpDisabled} large={large} cardHeight={460} selected={selectedId === order.id} onSelect={onSelect} onAction={onAction} onPrint={onPrint} />
        </div>
      })}</div> : <div className="kds-empty">NO {name} ORDERS</div>}
    </div>
  </section>
}
