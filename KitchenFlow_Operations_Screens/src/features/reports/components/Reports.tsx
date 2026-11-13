import { useEffect, useState } from "react"
import { loadReportSummary } from "../../../api"
import type { ReportSummary } from "../../../api/report.types"

export function Reports() {
  const dayText = (date: Date) => [date.getFullYear(), String(date.getMonth() + 1).padStart(2, "0"), String(date.getDate()).padStart(2, "0")].join("-")
  const now = new Date()
  const [to, setTo] = useState(dayText(now))
  const [from, setFrom] = useState(dayText(new Date(now.getTime() - 29 * 86_400_000)))
  const [message, setMessage] = useState("")
  const [summary, setSummary] = useState<ReportSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [topPage, setTopPage] = useState(0)
  const [cancellationPage, setCancellationPage] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    void loadReportSummary(from, to, topPage, cancellationPage)
      .then((result) => {
        if (active) {
          setSummary(result)
          setMessage("")
        }
      })
      .catch((cause: unknown) => {
        if (active) setMessage(cause instanceof Error ? cause.message : "Unable to load reports.")
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [from, to, topPage, cancellationPage])

  const changeRange = (nextFrom: string, nextTo: string) => {
    const start = new Date(`${nextFrom}T00:00:00`), end = new Date(`${nextTo}T23:59:59`)
    if (end < start) { setMessage("End date must be on or after the start date."); return }
    if (end.getTime() - start.getTime() > 91 * 86_400_000) {
      setFrom(nextFrom)
      setTo(dayText(new Date(start.getTime() + 91 * 86_400_000)))
      setTopPage(0)
      setCancellationPage(0)
      setMessage("Reports cover a maximum of 92 days.")
      return
    }
    setFrom(nextFrom)
    setTo(nextTo)
    setTopPage(0)
    setCancellationPage(0)
    setMessage("")
  }

  const perHour = summary?.ordersPerHour ?? []
  const maxHour = Math.max(1, ...perHour.map((entry) => entry.count))
  const topItems = summary?.topItems
  const cancellations = summary?.cancellations

  return <main className="light-screen workspace-screen reports-screen"><header className="page-header"><h1>REPORTS</h1><div className="report-range"><label>FROM<input type="date" value={from} onChange={(event) => changeRange(event.target.value, to)} /></label><label>TO<input type="date" value={to} onChange={(event) => changeRange(from, event.target.value)} /></label></div></header>{message && <p role="status">{message}</p>}{loading && <p role="status">Loading report…</p>}
    <section className="report-summary"><div><strong>₹{Number(summary?.sales ?? 0).toFixed(2)}</strong><span>SALES</span></div><div><strong>₹{Number(summary?.gst ?? 0).toFixed(2)}</strong><span>GST</span></div><div><strong>{summary?.completed ?? 0}</strong><span>COMPLETED</span></div><div><strong>{summary?.cancelled ?? 0}</strong><span>CANCELLED</span></div><div><strong>{summary?.orders ?? 0}</strong><span>ORDERS</span></div></section>
    <section className="reports-grid"><article><h2>TOP ITEMS</h2>{topItems?.content.length ? topItems.content.map((item) => <div className="report-row" key={item.itemId}><span>{item.name}</span><b>{item.quantity}</b></div>) : <p>No completed items in this date range.</p>}<div className="report-pagination"><button disabled={!topItems || topPage <= 0} onClick={() => setTopPage((page) => Math.max(0, page - 1))}>PREVIOUS</button><span>PAGE {topItems?.totalPages ? topPage + 1 : 0} / {topItems?.totalPages ?? 0}</span><button disabled={!topItems || topPage + 1 >= topItems.totalPages} onClick={() => setTopPage((page) => page + 1)}>NEXT</button></div></article>
      <article><h2>PREP TIME BY STATION</h2>{summary?.prepTimeByStation.map(({ stationId, stationName, averageSeconds }) => <div className="report-row" key={stationId}><span>{stationName}</span><b>{`${Math.floor(averageSeconds / 60)}:${String(averageSeconds % 60).padStart(2, "0")}`}</b></div>)}{!summary?.prepTimeByStation.length && <p>No completed ticket timings in this date range.</p>}</article>
      <article className="orders-hour"><h2>ORDERS PER HOUR</h2><div>{perHour.map(({ hour, count }) => <label key={hour} title={`${hour}:00 · ${count} orders`}><i style={{ height: `${Math.max(4, count / maxHour * 100)}%` }} /><small>{String(hour).padStart(2, "0")}</small></label>)}</div></article>
      <article><h2>CANCELLATIONS · {cancellations?.totalElements ?? 0}</h2>{cancellations?.content.map((order) => <div className="report-row" key={order.id}><span>#{order.token} · {order.reason ?? "Cancelled"}</span><b>{new Date(order.cancelledAt).toLocaleDateString()}</b></div>)}{!cancellations?.content.length && <p>No cancellations in this date range.</p>}<div className="report-pagination"><button disabled={!cancellations || cancellationPage <= 0} onClick={() => setCancellationPage((page) => Math.max(0, page - 1))}>PREVIOUS</button><span>PAGE {cancellations?.totalPages ? cancellationPage + 1 : 0} / {cancellations?.totalPages ?? 0}</span><button disabled={!cancellations || cancellationPage + 1 >= cancellations.totalPages} onClick={() => setCancellationPage((page) => page + 1)}>NEXT</button></div></article>
    </section>
  </main>
}
