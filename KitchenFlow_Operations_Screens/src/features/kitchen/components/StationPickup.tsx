import { useEffect, useState } from "react"
import type { Order, Station } from "../../../domain/core.types"
import { elapsed } from "../../../domain/time"

export function StationPickup({ orders, station, now, onServe }: { orders: Order[]; station: Station; now: number; onServe: (order: Order) => void }) {
  const [servedId, setServedId] = useState("")
  const ready = orders.filter((order) => order.tickets.some((ticket) => ticket.station === station && ["READY", "DONE"].includes(ticket.status)))
  const served = orders.find((order) => order.id === servedId && order.tickets.some((ticket) => ticket.station === station && ticket.status === "SERVED"))
  useEffect(() => { if (!servedId) return; const timer = window.setTimeout(() => setServedId(""), 1_000); return () => window.clearTimeout(timer) }, [servedId])
  const visible = [...ready, ...(served && !ready.some((order) => order.id === served.id) ? [served] : [])]
  return <section className="pickup-shelf" aria-label="Ready tickets"><h2>PICKUP · READY {ready.length}</h2>{visible.map((order) => { const ticket = order.tickets.find((entry) => entry.station === station)!; const isServed = ticket.status === "SERVED"; return <article className={isServed ? "served" : ""} key={ticket.id}><strong>#{order.token}</strong><span>{order.table ? `TABLE ${order.table}` : order.type.replace("_", " ")}</span><time>{elapsed(ticket.completedAt ?? order.paidAt ?? order.createdAt, now)}</time><button type="button" disabled={isServed} onClick={() => { setServedId(order.id); onServe(order) }}>SERVED</button></article> })}{!visible.length && <p>No ready tickets on the shelf.</p>}</section>
}
