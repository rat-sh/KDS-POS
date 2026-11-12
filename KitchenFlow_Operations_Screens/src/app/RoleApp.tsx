import { useEffect, useMemo, useState } from "react"
import { Shell } from "../features/shell"
import { homeFor, navFor, hashForPage, pageFromHash } from "../domain/permissions"
import type { OrderLine, OrderType, User } from "../domain/core.types"
import { businessDayKey } from "../domain/time"
import { useOrdersStore } from "../store/orders"
import { AppContent } from "./AppContent"
import { loadSettings } from "../api"
import { updateSettings } from "../store/settings"

export function RoleApp({ initialUser, onLogout }: { initialUser: User; onLogout: () => void }) {
  const [page, setPage] = useState(() => pageFromHash(location.hash) || homeFor(initialUser.role))
  const [orders] = useOrdersStore()
  const [draft, setDraft] = useState<OrderLine[]>([])
  const [orderType, setOrderType] = useState<OrderType>("DINE_IN")
  const [note, setNote] = useState("")
  const [table, setTable] = useState("")
  const [priority, setPriority] = useState(false)
  const [priorityReason, setPriorityReason] = useState("")
  const today = businessDayKey(Date.now())
  const businessOrders = useMemo(() => orders.filter((order) => (order.businessDay ?? businessDayKey(order.createdAt)) === today), [orders, today])

  useEffect(() => {
    if (!location.hash) location.hash = hashForPage(page)
    const onHash = () => setPage(pageFromHash(location.hash) || homeFor(initialUser.role))
    window.addEventListener("hashchange", onHash)
    return () => window.removeEventListener("hashchange", onHash)
  }, [page, initialUser.role])

  useEffect(() => { void loadSettings().then(updateSettings).catch(() => undefined) }, [])

  const goTo = (next: string) => { setPage(next); if (page !== next) location.hash = hashForPage(next) }
  const resetDraft = () => { setDraft([]); setNote(""); setTable(""); setPriority(false); setPriorityReason(""); goTo("Counter") }
  const readyCount = businessOrders.filter((order) => order.status === "READY").length

  return <Shell user={initialUser} page={page} nav={navFor(initialUser.role)} setPage={goTo} onLogout={onLogout} readyCount={readyCount}>
    <AppContent user={initialUser} page={page} allOrders={orders} orders={businessOrders} draft={draft} setDraft={setDraft} orderType={orderType} setOrderType={setOrderType} note={note} setNote={setNote} table={table} setTable={setTable} priority={priority} setPriority={setPriority} priorityReason={priorityReason} setPriorityReason={setPriorityReason} onPay={() => goTo("Payment")} onPaid={resetDraft} onLogout={onLogout} />
  </Shell>
}
