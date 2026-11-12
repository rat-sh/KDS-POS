import { useEffect, useState } from "react"
import type { Order, OrderLine, OrderType, PaymentMethod } from "../../../domain/core.types"
import { calculateOrder, createOrder, recordPayment } from "../../../api"
import { useMenuStore } from "../../../store/catalog"
import { refreshOrders } from "../../../store/orders"
import { useSettingsStore } from "../../../store/settings"
import { actionMessage } from "../../../shared/logic"
import { Banner } from "../../../shared/ui"
import { Bill } from "./Bill"

export function Payment({ draft, orders, orderType, table, note, priority, priorityReason, onDone }: { draft: OrderLine[]; orders: Order[]; orderType: OrderType; table: string; note: string; priority: boolean; priorityReason: string; onDone: () => void }) {
  const { data, error: menuError } = useMenuStore()
  const settings = useSettingsStore()
  const [method, setMethod] = useState<PaymentMethod>("Cash")
  const [tendered, setTendered] = useState("")
  const [error, setError] = useState("")
  const [paying, setPaying] = useState(false)
  const [idempotencyKey] = useState(() => crypto.randomUUID())
  const [completedOrder, setCompletedOrder] = useState<Order | null>(null)
  const total = calculateOrder(draft, data?.items, data?.modifiers, data?.rates)
  const [loadedTotal, setLoadedTotal] = useState(total.total)
  const changed = loadedTotal !== total.total
  useEffect(() => { if (!changed) return; setLoadedTotal(total.total); setTendered("") }, [changed, total.total])
  const amount = Number(tendered) || 0
  const canRecord = method !== "Cash" || amount >= total.total
  const pay = async () => { if (paying) return; setPaying(true); setError(""); try { const token = Math.max(0, ...orders.map((order) => order.token)) + 1; const created = await createOrder(draft, orderType, note, token, orderType === "DINE_IN" ? table : undefined, priority ? priorityReason : undefined, idempotencyKey); await recordPayment(created.id, method, total.total, method === "Cash" ? amount : undefined); const saved = await refreshOrders(true); setCompletedOrder(saved.find((order) => order.id === created.id) ?? { ...created, status: "IN_KITCHEN", paidAt: Date.now(), payment: { method } }) } catch (cause) { setError(actionMessage(cause)) } finally { setPaying(false) } }
  if (!data) return <main className="light-screen payment-screen loading-screen">{menuError || "LOADING MENU…"}</main>
  if (completedOrder) return <div className="payment-bill-overlay"><Bill order={completedOrder} onDone={onDone} /></div>
  return <main className="light-screen payment-screen role-payment" role="dialog" aria-modal="true" aria-labelledby="payment-dialog-title">{error && <Banner>{error}</Banner>}<header className="page-header"><div><span>PAYMENT</span><h1 id="payment-dialog-title">COLLECT PAYMENT</h1></div></header>{changed && <div className="warning-box restore">AMOUNT_MISMATCH · Order changed. Total reloaded.</div>}<div className="payment-layout"><section className="payment-methods"><span className="section-label">SELECT PAYMENT METHOD</span><div className="locked-amount"><span>AMOUNT TO COLLECT</span><strong>₹{total.total.toFixed(2)}</strong></div><div className="payment-method-tabs">{(["Cash", "UPI", "Card"] as PaymentMethod[]).map((entry) => <button type="button" className={method === entry ? "active" : ""} key={entry} onClick={() => setMethod(entry)}>{entry.toUpperCase()}</button>)}</div>{method === "Cash" && <label className="tendered-field">TENDERED<input inputMode="decimal" value={tendered} onChange={(event) => setTendered(event.target.value)} /><small>CHANGE DUE ₹{Math.max(0, amount - total.total).toFixed(2)}</small></label>}</section><aside className="payment-summary"><div><span>{settings.outletName}</span>{settings.gstin && <small>GSTIN {settings.gstin}</small>}</div><strong>TOTAL ₹{total.total.toFixed(2)}</strong><button className="record-payment" disabled={!canRecord || paying} onClick={() => void pay()}>{paying ? "RECORDING…" : "RECORD PAYMENT"}</button></aside></div></main>
}
