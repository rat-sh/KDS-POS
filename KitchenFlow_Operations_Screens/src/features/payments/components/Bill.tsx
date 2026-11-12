import type { Order } from "../../../domain/core.types"
import { calculateOrder } from "../../../api"
import { useMenuStore } from "../../../store/catalog"
import { useSettingsStore } from "../../../store/settings"

export function Bill({ order, onDone }: { order: Order; onDone: () => void }) {
  const { data, error } = useMenuStore()
  const settings = useSettingsStore()
  if (!data) return <main className="light-screen bill-screen loading-screen">{error || "LOADING MENU…"}</main>
  const total = calculateOrder(order, data.items, data.modifiers, data.rates)
  return <main className="light-screen bill-screen"><header className="page-header"><div><span>{settings.outletName}</span><h1>BILL</h1>{order.table && <strong>TABLE {order.table}</strong>}</div><small>GSTIN {settings.gstin}</small></header><section className="bill-lines">{total.lines.map((line) => <div key={line.itemId}><span><strong>{line.item.name}</strong><small>{line.qty} × ₹{line.item.price.toFixed(2)}</small></span><b>₹{line.gross.toFixed(2)}</b></div>)}</section><footer className="bill-total"><span>TOTAL</span><strong>₹{total.total.toFixed(2)}</strong><button type="button" className="primary-action" onClick={onDone}>NEW ORDER</button></footer></main>
}
