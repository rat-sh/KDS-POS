import { useMemo, useState } from "react"
import type { Order } from "../../../domain/core.types"
import { calculateOrder } from "../../../api"
import { useMenuStore } from "../../../store/catalog"
import { allStationNames } from "../../../api/stationRegistry"

export function Reports({ orders }: { orders: Order[] }) {
  const stations = allStationNames()
  const dayText = (date: Date) => [date.getFullYear(), String(date.getMonth() + 1).padStart(2, "0"), String(date.getDate()).padStart(2, "0")].join("-")
  const now = new Date()
  const [to, setTo] = useState(dayText(now))
  const [from, setFrom] = useState(dayText(new Date(now.getTime() - 29 * 86_400_000)))
  const [message, setMessage] = useState("")
  const { data } = useMenuStore()
  const filtered = useMemo(() => { const start = new Date(`${from}T00:00:00`).getTime(); const end = new Date(`${to}T23:59:59`).getTime(); return orders.filter((order) => order.createdAt >= start && order.createdAt <= end) }, [from, orders, to])
  const changeRange = (nextFrom: string, nextTo: string) => {
    const start = new Date(`${nextFrom}T00:00:00`), end = new Date(`${nextTo}T23:59:59`)
    if (end < start) { setMessage("End date must be on or after the start date."); return }
    if (end.getTime() - start.getTime() > 91 * 86_400_000) { setFrom(nextFrom); setTo(dayText(new Date(start.getTime() + 91 * 86_400_000))); setMessage("Reports cover a maximum of 92 days."); return }
    setFrom(nextFrom); setTo(nextTo); setMessage("")
  }
  const completed = filtered.filter((order) => order.status === "COMPLETED")
  const cancelled = filtered.filter((order) => order.status === "CANCELLED")
  const sales = completed.reduce((sum, order) => sum + calculateOrder(order, data?.items, data?.modifiers, data?.rates).total, 0)
  const topItems = Object.entries(completed.flatMap((order) => order.lines).reduce<Record<string, number>>((counts, line) => ({ ...counts, [line.itemId]: (counts[line.itemId] ?? 0) + line.qty }), {})).sort((a, b) => b[1] - a[1]).slice(0, 8)
  const prep = stations.map((station) => { const times = completed.flatMap((order) => order.tickets.filter((ticket) => ticket.station === station && ticket.startedAt && ticket.completedAt).map((ticket) => ticket.completedAt! - ticket.startedAt!)); return { station, average: times.length ? Math.round(times.reduce((sum, time) => sum + time, 0) / times.length / 1000) : 0 } })
  const perHour = Array.from({ length: 24 }, (_, hour) => ({ hour, count: filtered.filter((order) => order.paidAt && new Date(order.paidAt).getHours() === hour).length }))
  const maxHour = Math.max(1, ...perHour.map((entry) => entry.count))
  return <main className="light-screen workspace-screen reports-screen"><header className="page-header"><h1>REPORTS</h1><div className="report-range"><label>FROM<input type="date" value={from} onChange={(event) => changeRange(event.target.value, to)} /></label><label>TO<input type="date" value={to} onChange={(event) => changeRange(from, event.target.value)} /></label></div></header>{message && <p role="status">{message}</p>}
    <section className="report-summary"><div><strong>₹{sales.toFixed(2)}</strong><span>SALES</span></div><div><strong>{completed.length}</strong><span>COMPLETED</span></div><div><strong>{cancelled.length}</strong><span>CANCELLED</span></div><div><strong>{filtered.length}</strong><span>ORDERS</span></div></section>
    <section className="reports-grid"><article><h2>TOP ITEMS</h2>{topItems.length ? topItems.map(([id, qty]) => <div className="report-row" key={id}><span>{data?.items.find((item) => item.id === id)?.name ?? id}</span><b>{qty}</b></div>) : <p>No completed items in this date range.</p>}</article>
      <article><h2>PREP TIME BY STATION</h2>{prep.map(({ station, average }) => <div className="report-row" key={station}><span>{station}</span><b>{average ? `${Math.floor(average / 60)}:${String(average % 60).padStart(2, "0")}` : "—"}</b></div>)}</article>
      <article className="orders-hour"><h2>ORDERS PER HOUR</h2><div>{perHour.map(({ hour, count }) => <label key={hour} title={`${hour}:00 · ${count} orders`}><i style={{ height: `${Math.max(4, count / maxHour * 100)}%` }} /><small>{String(hour).padStart(2, "0")}</small></label>)}</div></article>
      <article><h2>CANCELLATIONS · {cancelled.length}</h2>{cancelled.map((order) => <div className="report-row" key={order.id}><span>#{order.token} · {order.cancellation?.reason ?? "Cancelled"}</span><b>{new Date(order.cancellation?.cancelledAt ?? order.createdAt).toLocaleDateString()}</b></div>)}{!cancelled.length && <p>No cancellations in this date range.</p>}</article>
    </section>
  </main>
}
