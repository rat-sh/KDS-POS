import { useMemo, useState } from "react"
import type { Dispatch, SetStateAction } from "react"
import type { OrderLine, OrderType } from "../../../domain/core.types"
import type { MenuItem } from "../../../domain/catalog.types"
import { calculateOrder } from "../../../api"
import { ModifierSheet } from "./ModifierSheet"
import { useMenuStore } from "../../../store/catalog"


type Props = { draft: OrderLine[]; setDraft: Dispatch<SetStateAction<OrderLine[]>>; orderType: OrderType; setOrderType: (value: OrderType) => void; table: string; setTable: (value: string) => void; note: string; setNote: (value: string) => void; priority: boolean; setPriority: (value: boolean) => void; priorityReason: string; setPriorityReason: (value: string) => void; onPay: () => void }

export function PosScreen({ draft, setDraft, orderType, setOrderType, table, setTable, note, setNote, priority, setPriority, priorityReason, setPriorityReason, onPay }: Props) {
  const { data, loading, error } = useMenuStore()
  const [category, setCategory] = useState("ALL ITEMS")
  const [search, setSearch] = useState("")
  const [selectedItem, setSelectedItem] = useState<MenuItem | null>(null)
  const menuItems = data?.items ?? []
  const categories = useMemo(() => ["ALL ITEMS", ...[...new Set(menuItems.map((item) => item.category))].sort()], [menuItems])
  const visible = useMemo(() => menuItems.filter((item) => {
    const match = item.name.toLowerCase().includes(search.toLowerCase())
    const group = category === "ALL ITEMS" || item.category === category
    return match && group
  }), [category, menuItems, search])
  if (loading || !data) return <main className="light-screen role-pos loading-screen">{error || "LOADING MENU…"}</main>
  const total = calculateOrder(draft, data.items, data.modifiers, data.rates)
  const unavailable = total.lines.filter((line) => !line.item.available || line.item.stockTracking && line.item.stock === 0)
  const qtyOf = (itemId: string) => draft.filter((line) => line.itemId === itemId).reduce((sum, line) => sum + line.qty, 0)
  const short = total.lines.filter((line) => line.item.stockTracking && qtyOf(line.itemId) > line.item.stock)
  const disabledReason = !draft.length ? "Add an item to start an order." : orderType === "DINE_IN" && !table.trim() ? "Enter a table number for dine-in." : priority && !priorityReason ? "Choose a reason for priority." : unavailable.length || short.length ? "Resolve unavailable or out-of-stock items." : ""
  return <main className="light-screen pos-screen role-pos"><header className="pos-header"><strong className="pos-brand">KITCHENFLOW · COUNTER</strong><div className="order-type-tabs">{(["DINE_IN", "TAKEAWAY", "DELIVERY"] as OrderType[]).map((type) => <button type="button" className={orderType === type ? "active" : ""} key={type} onClick={() => setOrderType(type)}>{type.replace("_", " ")}</button>)}</div><input className="catalog-search" aria-label="Search items" placeholder="SEARCH ITEMS" value={search} onChange={(event) => setSearch(event.target.value)} /></header>
    <div className="pos-layout"><section className="catalog"><nav className="category-tabs" aria-label="Menu categories">{categories.map((entry) => <button type="button" className={category === entry ? "active" : ""} key={entry} onClick={() => setCategory(entry)}>{entry}</button>)}</nav><div className="catalog-tools"><span>{visible.length} ITEMS</span>{error && <small>{error}</small>}</div><div className="product-grid">{visible.map((item) => <button type="button" key={item.id} disabled={!item.available || item.stockTracking && item.stock === 0} onClick={() => setSelectedItem(item)}><strong>{item.name}</strong><b>₹{item.price.toFixed(2)}</b>{(!item.available || item.stock === 0) && <small>UNAVAILABLE</small>}</button>)}</div></section>
      <aside className="cart"><div className="cart-header"><h2>{draft.reduce((sum, line) => sum + line.qty, 0)} ITEMS</h2></div>{orderType === "DINE_IN" && <label className="table-field">TABLE NUMBER<input aria-label="Table number" value={table} maxLength={4} onChange={(event) => setTable(event.target.value.replace(/[^a-z0-9]/gi, "").slice(0, 4))} required /></label>}
        {unavailable.length > 0 && <p className="warning-box">Unavailable: {unavailable.map((line) => line.item.name).join(", ")}</p>}{short.length > 0 && <p className="warning-box">Stock is too low for: {short.map((line) => line.item.name).join(", ")}</p>}
        <div className={`cart-lines ${draft.length ? "" : "empty"}`}>{draft.length ? total.lines.map((line, index) => <div className="cart-line" key={`${line.itemId}-${index}`}><strong>{line.item.name}<small>₹{(line.net / line.qty).toFixed(2)} each</small></strong><div className="quantity-stepper"><button type="button" aria-label={`Decrease ${line.item.name}`} onClick={() => setDraft((rows) => rows.flatMap((entry, i) => i !== index ? [entry] : entry.qty > 1 ? [{ ...entry, qty: entry.qty - 1 }] : []))}>−</button><span>{line.qty}</span><button type="button" aria-label={`Increase ${line.item.name}`} onClick={() => setDraft((rows) => rows.map((entry, i) => i === index ? { ...entry, qty: entry.qty + 1 } : entry))}>+</button></div><b>₹{line.net.toFixed(2)}</b></div>) : <p className="cart-empty-hint">Add an item to start an order.</p>}</div>
        <textarea aria-label="Order note" value={note} onChange={(event) => setNote(event.target.value)} placeholder="ORDER NOTE OR ALLERGY" />
        <label className="priority-entry"><input type="checkbox" checked={priority} onChange={(event) => { setPriority(event.target.checked); if (!event.target.checked) setPriorityReason("") }} /> PRIORITY</label>{priority && <label className="priority-reason">REASON<input maxLength={250} placeholder="Reason for priority" value={priorityReason} onChange={(event) => setPriorityReason(event.target.value)} /></label>}
        <footer className="cart-footer"><div><span>SUBTOTAL</span><b>₹{total.subtotal.toFixed(2)}</b></div>{total.gstBreakdown.map((entry) => <div key={entry.category}><span>GST ({entry.category}) · {(entry.rate * 100).toFixed(0)}%</span><b>₹{entry.amount.toFixed(2)}</b></div>)}<div className="grand-total"><span>TOTAL</span><b>₹{total.total.toFixed(2)}</b></div><button className="pay-button" disabled={!!disabledReason} onClick={onPay}>PAY <b>₹{total.total.toFixed(2)}</b></button>{disabledReason && draft.length > 0 && <small className="pay-disabled-reason">{disabledReason}</small>}</footer>
      </aside></div>{selectedItem && <ModifierSheet item={selectedItem} groups={data.groups} modifiers={data.modifiers} onClose={() => setSelectedItem(null)} onConfirm={(line) => { setDraft((lines) => [...lines, line]); setSelectedItem(null) }} />}</main>
}
