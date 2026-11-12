import { useMemo, useState } from "react"
import type { Order } from "../../../domain/core.types"
import { cancelOrder } from "../../../api"
import { refreshOrders } from "../../../store/orders"
import { actionMessage } from "../../../shared/logic"
import { Toast } from "../../../shared/ui"
import { CancelModal } from "./CancelModal"
import { elapsed } from "../../../domain/time"

export function OrdersList({ orders, canCancelUnpaid = false }: { orders: Order[]; canCancelUnpaid?: boolean }) {
  const [selected, setSelected] = useState<Order | null>(null)
  const [error, setError] = useState("")
  const [search, setSearch] = useState("")
  const matches = useMemo(() => orders.filter((order) => String(order.token).includes(search.trim())), [orders, search])
  const ready = matches.filter((order) => order.status === "READY")
  const cancel = async (order: Order, reason: string, detail: string) => {
    try {
      await cancelOrder(order.id, reason, detail, order.version)
      await refreshOrders()
      setSelected(null)
    } catch (cause) { setError(actionMessage(cause)); await refreshOrders() }
  }
  return <main className="light-screen workspace-screen"><header className="page-header"><div><h1>ORDERS</h1></div><label className="token-search">TOKEN SEARCH<input aria-label="Search by token" value={search} onChange={(event) => setSearch(event.target.value.replace(/\D/g, ""))} inputMode="numeric" /></label></header>
    {error && <Toast>{error}</Toast>}
    <section className="ready-strip" aria-label="Ready orders"><strong>READY</strong>{ready.length ? ready.map((order) => <span key={order.id}>#{order.token}{order.table ? ` · TABLE ${order.table}` : ""}</span>) : <span>None waiting</span>}</section>
    <section className="data-table orders-table"><div className="data-row table-head"><span>TOKEN</span><span>ORDER</span><span>STATUS</span><span>TYPE</span><span>TICKETS</span><span>ACTION</span></div>
      {matches.map((order) => <article className="data-row order-live-row" key={order.id}><strong>#{order.token}{order.table && <small>TABLE {order.table}</small>}</strong><span>{order.id}</span><span>{order.status}</span><span>{order.type.replace("_", " ")}</span><div className="order-station-status">{order.tickets.map((ticket) => <small key={ticket.id}>{ticket.station}: {ticket.status === "DONE" ? "READY" : ticket.status} · cook {elapsed(0, ticket.startedAt && ticket.completedAt ? ticket.completedAt - ticket.startedAt : 0)}</small>)}</div>
        {canCancelUnpaid && order.status === "CREATED" ? <button type="button" onClick={() => setSelected(order)}>CANCEL</button> : <span aria-label="Cancellation unavailable">—</span>}</article>)}
      {!matches.length && <p className="table-empty">No orders match that token.</p>}
    </section>
    {selected && <CancelModal token={selected.token} onClose={() => setSelected(null)} onConfirm={(reason, detail) => void cancel(selected, reason, detail)} />}
  </main>
}
