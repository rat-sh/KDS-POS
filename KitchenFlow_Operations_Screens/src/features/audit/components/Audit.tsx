import { useEffect, useMemo, useState } from "react"
import { loadAuditEntries } from "../../../api"
import type { Order } from "../../../domain/core.types"

type Entry = { id: string; at: number; actor: string; action: string; entity: string; reference: string; reason?: string }
const pageSize = 25

export function Audit({ orders = [] }: { orders?: Order[] }) {
  const [entries, setEntries] = useState<Entry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [filter, setFilter] = useState("ALL")
  const [search, setSearch] = useState("")
  const [page, setPage] = useState(0)
  useEffect(() => { void loadAuditEntries().then((rows) => setEntries(rows.map((row) => ({ ...row, id: String(row.id) })))).catch((cause) => setError(cause instanceof Error ? cause.message : "Audit could not be loaded.")).finally(() => setLoading(false)) }, [])
  const orderEntries = orders.flatMap((order) => [
    ...(order.events ?? []).map((event, index) => ({ id: `${event.ticket}-${index}`, at: event.time, actor: event.actor, action: `${event.from} → ${event.to}`, entity: `${event.station} ticket`, reference: `${event.token}${event.table ? ` · TABLE ${event.table}` : ""}`, reason: event.reason })),
    ...(order.audit ?? []).map((event, index) => ({ id: `${order.id}-audit-${index}`, at: event.at, actor: event.actor, action: event.action, entity: "Order", reference: `${order.token}${order.table ? ` · TABLE ${order.table}` : ""}`, reason: event.reason })),
    ...(order.cancellation ? [{ id: `${order.id}-cancel`, at: order.cancellation.cancelledAt, actor: order.cancellation.actor, action: "ORDER_CANCELLED", entity: "Order", reference: `${order.token}${order.table ? ` · TABLE ${order.table}` : ""}`, reason: `${order.cancellation.reason} · ${order.cancellation.detail}` }] : []),
  ])
  const all = [...entries, ...orderEntries].sort((a, b) => b.at - a.at)
  const counts = { ALL: all.length, ORDERS: all.filter((entry) => entry.entity === "Order").length, TICKETS: all.filter((entry) => entry.entity.includes("ticket")).length, OVERRIDES: all.filter((entry) => entry.action.includes("STEP_IN") || entry.action.includes("PRIORITY")).length }
  const filtered = useMemo(() => all.filter((entry) => (filter === "ALL" || filter === "ORDERS" && entry.entity === "Order" || filter === "TICKETS" && entry.entity.includes("ticket") || filter === "OVERRIDES" && (entry.action.includes("STEP_IN") || entry.action.includes("PRIORITY"))) && `${entry.actor} ${entry.action} ${entry.entity} ${entry.reference} ${entry.reason ?? ""}`.toLowerCase().includes(search.toLowerCase())), [all, filter, search])
  const pages = Math.max(1, Math.ceil(filtered.length / pageSize))
  if (loading) return <main className="light-screen workspace-screen loading-screen">LOADING AUDIT…</main>
  return <main className="light-screen workspace-screen audit-screen"><header className="page-header"><h1>AUDIT</h1></header>{error && <p role="status">{error}</p>}
    <section className="audit-summary"><div><strong>{all.length}</strong><span>EVENTS</span></div><div><strong>{counts.ORDERS}</strong><span>ORDER ACTIONS</span></div><div><strong>{counts.TICKETS}</strong><span>TICKET TRANSITIONS</span></div><div><strong>{counts.OVERRIDES}</strong><span>MANAGER OVERRIDES</span></div></section>
    <nav className="audit-chips" aria-label="Quick audit filters">{Object.entries(counts).map(([name, count]) => <button type="button" key={name} className={filter === name ? "active" : ""} onClick={() => { setFilter(name); setPage(0) }}>{name} · {count}</button>)}</nav>
    <label className="audit-search">FILTER EVENTS<input value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} placeholder="Actor, action, token or reason" /></label>
    <div className="audit-table-wrap"><table className="audit-table"><thead><tr><th>TIME</th><th>ACTOR</th><th>ACTION</th><th>ENTITY</th><th>REFERENCE</th><th>REASON</th></tr></thead><tbody>{filtered.slice(page * pageSize, (page + 1) * pageSize).map((entry) => <tr key={entry.id}><td>{new Date(entry.at).toLocaleString("en-IN")}</td><td>{entry.actor}</td><td>{entry.action}</td><td>{entry.entity}</td><td>{entry.reference}</td><td>{entry.reason ?? "—"}</td></tr>)}</tbody></table>{!filtered.length && <p>No matching audit events.</p>}</div>
    <footer className="audit-pagination"><span>PAGE {page + 1} OF {pages} · {filtered.length} EVENTS</span><button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>PREVIOUS</button><button type="button" disabled={page + 1 >= pages} onClick={() => setPage((value) => value + 1)}>NEXT</button></footer>
  </main>
}
