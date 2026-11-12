import { useState } from "react"
import type { MenuItem } from "../../../domain/catalog.types"
import { setAvailability } from "../../../api"
import { useMenuStore } from "../../../store/catalog"
import { refreshMenu } from "../../../store/catalog"

export function Availability() {
  const { data, loading, error } = useMenuStore()
  const [actionError, setActionError] = useState("")
  if (error) return <main className="light-screen workspace-screen loading-screen">{error}</main>
  if (loading || !data) return <main className="light-screen workspace-screen loading-screen">LOADING MENU…</main>
  const menuItems = data.items
  const toggle = async (item: MenuItem, next: boolean) => { try { await setAvailability(item.id, next); await refreshMenu(); setActionError("") } catch (cause) { setActionError(cause instanceof Error ? cause.message : "Availability could not be updated.") } }
  return <main className="light-screen workspace-screen"><header className="page-header"><div><h1>AVAILABILITY</h1></div></header>{actionError && <p role="alert">{actionError}</p>}<section className="data-table">{menuItems.map((item: MenuItem) => { const outOfStock = item.stockTracking && item.stock <= 0; const isAvailable = item.available; return <div className="availability-row" key={item.id}><div><strong>{item.name}</strong><span>{item.category} · {item.stockTracking ? `${item.stock} IN STOCK` : "STOCK NOT TRACKED"}</span></div>{outOfStock && <small>Stock is zero; set stock in Admin Menu.</small>}<button type="button" disabled={outOfStock} className={`availability-toggle ${isAvailable ? "on" : ""}`} onClick={() => void toggle(item, !isAvailable)}><span />{isAvailable ? "AVAILABLE" : "UNAVAILABLE"}</button></div> })}</section></main>
}
