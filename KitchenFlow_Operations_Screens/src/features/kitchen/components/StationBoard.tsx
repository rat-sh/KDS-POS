import { useEffect, useMemo, useState } from "react"
import type { Order, Station, User } from "../../../domain/core.types"
import { isOpenTicket, stationQueue } from "../../../domain/queue"
import { ticketSla } from "../../../domain/sla"
import { acknowledgeCancelled, acceptTicket, bumpTicket, recallTicket, serveTicket, startTicket } from "../../../api"
import { refreshOrders } from "../../../store/orders"
import { useMenuStore } from "../../../store/catalog"
import { useNow, useOnlineStatus } from "../../../shared/hooks"
import { useSettingsStore } from "../../../store/settings"
import { actionMessage } from "../../../shared/logic"
import { Toast } from "../../../shared/ui"
import { KdsHeader } from "./KdsHeader"
import { StationInventory } from "./StationInventory"
import { RecallSheet } from "./RecallSheet"
import { StationPrintTicket } from "./StationPrintTicket"
import { StationZone } from "./StationZone"
import { StationSummary } from "./StationSummary"
import { OverdueAlert } from "./OverdueAlert"
import { StationRecord } from "./StationRecord"
import { isSmallTicket } from "../logic/isSmallTicket"
import { StationPickup } from "./StationPickup"

type Props = { user: User; orders: Order[]; onLogout?: () => void }

export function StationBoard({ user, orders, onLogout }: Props) {
  const [station, setStation] = useState<Station>(user.stations[0] ?? ""), [error, setError] = useState(""), [selectedId, setSelectedId] = useState(""), [jumpId, setJumpId] = useState(""), [recallOpen, setRecallOpen] = useState(false), [focusedItem, setFocusedItem] = useState("")
  const [couponOrder, setCouponOrder] = useState<Order | null>(null)
  const [undoTicket, setUndoTicket] = useState<{ orderId: string; until: number } | null>(null)
  const [recordOpen, setRecordOpen] = useState(false)
  const [inventoryOpen, setInventoryOpen] = useState(() => window.innerWidth >= 1500)
  const now = useNow(), online = useOnlineStatus(), settings = useSettingsStore(), { data } = useMenuStore()
  const items = data?.items ?? [], modifiers = data?.modifiers ?? [], slaThresholds = { amberPercent: settings.amberPercent, redPercent: settings.redPercent }
  const tickets = useMemo(() => stationQueue(orders, station, items, now, slaThresholds).sort((a, b) => {
    const band = (order: Order) => {
      const ticket = order.tickets.find((entry) => entry.station === station)
      if (ticket?.status === "CANCELLED") return 0
      const active = ["NEW", "ACCEPTED", "COOKING", "IN_PROGRESS"].includes(ticket?.status ?? "")
      if (active && ticketSla(order, station, items, now, slaThresholds).colour === "RED") return 1
      if (order.priority || order.rush) return 2
      if (order.vip) return 3
      if (active) return 4
      if (ticket?.status === "DONE" || ticket?.status === "SERVED") return 5
      return 6
    }
    const ticketA = a.tickets.find((entry) => entry.station === station), ticketB = b.tickets.find((entry) => entry.station === station)
    const priorityA = band(a), priorityB = band(b)
    return priorityA - priorityB || (priorityA === 5 ? (ticketA?.completedAt ?? 0) - (ticketB?.completedAt ?? 0) : (a.paidAt ?? a.createdAt) - (b.paidAt ?? b.createdAt))
  }), [orders, station, items, now, slaThresholds])
  const smallHeight = Math.min(460, Math.max(300, window.innerHeight - 308))
  const small = tickets.filter((order) => isSmallTicket(order, station, items, modifiers, smallHeight))
  const large = tickets.filter((order) => !small.includes(order))
  const open = tickets.filter((order) => order.tickets.some((ticket) => ticket.station === station && isOpenTicket(ticket.status)))
  const counts: Record<string, number> = { NEW: 0, IN_PROGRESS: 0, OVER_LIMIT: 0, PRIORITY: open.filter((order) => order.priority).length, VIP: open.filter((order) => order.vip).length }
  open.forEach((order) => { const ticket = order.tickets.find((entry) => entry.station === station); if (!ticket) return; counts[ticket.status === "NEW" ? "NEW" : "IN_PROGRESS"]++; if (ticketSla(order, station, items, now, slaThresholds).colour === "RED") counts.OVER_LIMIT++ })
  const act = async (order: Order, action: "accept" | "start" | "bump" | "serve" | "recall" | "ack" | "undo") => {
    const ticket = order.tickets.find((entry) => entry.station === station); if (!ticket) return
    if (action === "undo" && (!undoTicket || undoTicket.orderId !== order.id || Date.now() > undoTicket.until)) return
    try {
      if (action === "accept") await acceptTicket(ticket.id, ticket.version)
      else if (action === "start") await startTicket(ticket.id, ticket.version)
      else if (action === "bump") await bumpTicket(ticket.id, ticket.version)
      else if (action === "serve") await serveTicket(ticket.id, ticket.version)
      else if (action === "recall" || action === "undo") await recallTicket(ticket.id, ticket.version)
      else await acknowledgeCancelled(ticket.id, ticket.version)
      if (action === "accept") setCouponOrder({ ...order, tickets: order.tickets.map((entry) => entry.station === station ? { ...entry, status: "ACCEPTED", acceptedAt: now } : entry) })
      if (action === "undo") setUndoTicket(null)
      await refreshOrders()
    } catch (cause) {
      setError(actionMessage(cause))
      await refreshOrders()
    }
  }
  const markDone = (order: Order) => { setJumpId(order.id); setUndoTicket({ orderId: order.id, until: Date.now() + 5_000 }); void act(order, "bump") }
  useEffect(() => { if (selectedId && !tickets.some((order) => order.id === selectedId)) setSelectedId("") }, [selectedId, tickets])
  useEffect(() => { if (undoTicket && now >= undoTicket.until) setUndoTicket(null) }, [now, undoTicket])
  useEffect(() => {
    const narrow = window.matchMedia("(max-width: 1499px)")
    const closeInventory = (event: MediaQueryListEvent) => { if (event.matches) setInventoryOpen(false) }
    narrow.addEventListener("change", closeInventory)
    return () => narrow.removeEventListener("change", closeInventory)
  }, [])
  return <main className="dark-screen station-screen operational-screen kds-screen kds-manager-screen">
    {error && <Toast>{error}</Toast>}
    <KdsHeader user={user} station={station} orders={orders} items={items} now={now} onStation={(next) => { setStation(next); setSelectedId("") }} onRecord={() => setRecordOpen(true)} onRecall={() => setRecallOpen(true)} onLogout={onLogout} />{!online && <div className="offline-banner" role="status"><span>Offline. This board may be out of date. Done is disabled until you reconnect.</span></div>}
    <StationSummary counts={counts} />
    <OverdueAlert orders={open} station={station} items={items} now={now} slaThresholds={slaThresholds} onJump={(id) => { setSelectedId(id); setJumpId(id) }} />
    <div className="kds-workspace">
    <button className="station-inventory-toggle" type="button" aria-expanded={inventoryOpen} onClick={() => setInventoryOpen((value) => !value)}>STATION ITEMS {inventoryOpen ? "−" : "+"}</button><div className={`station-inventory-panel ${inventoryOpen ? "open" : "closed"}`}><StationInventory items={items} orders={orders} station={station} focusedItem={focusedItem} onFocus={setFocusedItem} now={now} /></div>
      <div className="station-zones two-zones">
        <StationZone name="SMALL" orders={small} station={station} now={now} items={items} modifiers={modifiers} slaThresholds={slaThresholds} bumpDisabled={!online} selectedId={selectedId} focusedItem={focusedItem} jumpId={jumpId} onSelect={(order) => setSelectedId((current) => current === order.id ? "" : order.id)} onAction={(order, action) => action === "done" ? markDone(order) : void act(order, action === "accept" ? "accept" : action === "start" ? "start" : "ack")} onPrint={setCouponOrder} />
        <StationZone name="LARGE" orders={large} station={station} now={now} items={items} modifiers={modifiers} slaThresholds={slaThresholds} bumpDisabled={!online} selectedId={selectedId} focusedItem={focusedItem} jumpId={jumpId} onSelect={(order) => setSelectedId((current) => current === order.id ? "" : order.id)} onAction={(order, action) => action === "done" ? markDone(order) : void act(order, action === "accept" ? "accept" : action === "start" ? "start" : "ack")} onPrint={setCouponOrder} />
      </div>
    </div>
    <StationPickup orders={orders} station={station} now={now} onServe={(order) => void act(order, "serve")} />
    {undoTicket && orders.find((order) => order.id === undoTicket.orderId) && <aside className="undo-done-bar" role="status"><span>Ticket #{orders.find((order) => order.id === undoTicket.orderId)?.token} marked READY</span><button type="button" onClick={() => void act(orders.find((order) => order.id === undoTicket.orderId)!, "undo")}>UNDO DONE · {Math.max(0, Math.ceil((undoTicket.until - now) / 1000))}s</button></aside>}
    {recallOpen && <RecallSheet orders={orders} station={station} now={now} items={items} onRecall={(order) => void act(order, "recall")} onClose={() => setRecallOpen(false)} />}
    {recordOpen && <StationRecord station={station} onClose={() => setRecordOpen(false)} />}
    {couponOrder && <div className="kds-sheet-backdrop" role="presentation"><section className="coupon-sheet" role="dialog" aria-modal="true" aria-label="Print ticket"><h2>PRINT TICKET</h2><StationPrintTicket order={couponOrder} station={station} items={items} modifiers={modifiers} /><div><button onClick={() => window.print()}>PRINT</button><button onClick={() => setCouponOrder(null)}>CLOSE</button></div></section></div>}
  </main>
}
