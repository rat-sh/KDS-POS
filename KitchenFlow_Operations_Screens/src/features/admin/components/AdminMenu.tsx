import { useState } from "react"
import type { MenuItem } from "../../../domain/catalog.types"
import { adjustStock, createItemModifierGroup, createModifier, deleteMenuItem, saveMenuItem } from "../../../api"
import { refreshMenu, useMenuStore } from "../../../store/catalog"
import { allStationNames } from "../../../api/stationRegistry"

type MenuDraft = { id: string; name: string; category: string; price: string; prepSeconds: string; stock: string; stations: string[]; available: boolean; modifierGroups: string[] }
const emptyItem: MenuDraft = { id: "", name: "", category: "", price: "", prepSeconds: "", stock: "", stations: [], available: true, modifierGroups: [] }

export function AdminMenu() {
  const { data, loading, error } = useMenuStore()
  const stations = allStationNames()
  const [editing, setEditing] = useState<MenuDraft>(emptyItem)
  const [isNew, setIsNew] = useState(false)
  const [message, setMessage] = useState("")
  const [stockItem, setStockItem] = useState<MenuItem | null>(null)
  const [stockDelta, setStockDelta] = useState("")
  const [stockReason, setStockReason] = useState("")
  const [groupName, setGroupName] = useState("")
  const [groupSelection, setGroupSelection] = useState<"SINGLE" | "MULTIPLE">("SINGLE")
  const [modifierGroupId, setModifierGroupId] = useState("")
  const [modifierName, setModifierName] = useState("")
  const [modifierPrice, setModifierPrice] = useState("0")
  if (loading || !data) return <main className="light-screen workspace-screen loading-screen">{error || "LOADING MENU…"}</main>
  const begin = (item?: MenuItem) => { setIsNew(!item); setEditing(item ? { id: item.id, name: item.name, category: item.category, price: String(item.price), prepSeconds: String(item.prepSeconds), stock: String(item.stock), stations: [...item.stations], available: item.available, modifierGroups: [...item.modifierGroups] } : { ...emptyItem }) }
  const categories = Object.keys(data.rates).sort()
  const save = async () => {
    if (!editing.name.trim() || !editing.category || !editing.stations.length || Number(editing.price) < 0 || Number(editing.stock) < 0 || Number(editing.prepSeconds) <= 0) { setMessage("Enter a name, category, station, non-negative price and stock, and prep seconds above zero."); return }
    const item: MenuItem = { id: editing.id || editing.name.toLowerCase().replace(/[^a-z0-9]+/g, "-"), name: editing.name.trim(), category: editing.category, price: Number(editing.price), prepSeconds: Number(editing.prepSeconds), stock: Number(editing.stock), stockTracking: true, stations: editing.stations, modifierGroups: editing.modifierGroups, available: Number(editing.stock) > 0 && editing.available }
    try { await saveMenuItem(item, isNew); await refreshMenu(); setEditing({ ...emptyItem }); setIsNew(false); setMessage("Menu item saved.") } catch (cause) { setMessage(cause instanceof Error ? cause.message : "Menu item could not be saved.") }
  }
  const deactivate = async (item: MenuItem) => { try { await saveMenuItem({ ...item, available: !item.available && item.stock > 0 }, false); await refreshMenu() } catch (cause) { setMessage(cause instanceof Error ? cause.message : "Availability could not be changed.") } }
  const archive = async (item: MenuItem) => { try { await deleteMenuItem(item.id); await refreshMenu(); setMessage(`${item.name} archived.`) } catch (cause) { setMessage(cause instanceof Error ? cause.message : "This item could not be archived. Items used by orders must be retained.") } }
  const saveStockAdjustment = async () => {
    if (!stockItem) return
    const delta = Number(stockDelta)
    if (!Number.isInteger(delta) || delta === 0 || !stockReason.trim()) { setMessage("Enter a non-zero whole quantity and a reason."); return }
    try { await adjustStock(stockItem.id, delta, stockReason.trim(), stockItem.version); await refreshMenu(); setStockItem(null); setStockDelta(""); setStockReason(""); setMessage("Stock adjustment saved.") }
    catch (cause) { setMessage(cause instanceof Error ? cause.message : "Stock adjustment could not be saved.") }
  }
  const addOptionGroup = async () => {
    if (!editing.id || !groupName.trim()) { setMessage("Save the menu item first, then enter an option group name."); return }
    try { const created = await createItemModifierGroup(editing.id, { name: groupName.trim(), selectionType: groupSelection, minSelect: 0, maxSelect: groupSelection === "SINGLE" ? 1 : 10 }); setEditing((current) => ({ ...current, modifierGroups: [...new Set([...current.modifierGroups, String(created.id)])] })); setGroupName(""); setModifierGroupId(String(created.id)); await refreshMenu(); setMessage("Option group added.") }
    catch (cause) { setMessage(cause instanceof Error ? cause.message : "Option group could not be added.") }
  }
  const addModifier = async () => {
    if (!modifierGroupId || !modifierName.trim() || !Number.isFinite(Number(modifierPrice))) { setMessage("Choose a group and enter a valid option."); return }
    try { await createModifier(modifierGroupId, { name: modifierName.trim(), priceDelta: Number(modifierPrice), active: true }); setModifierName(""); setModifierPrice("0"); await refreshMenu(); setMessage("Option added.") }
    catch (cause) { setMessage(cause instanceof Error ? cause.message : "Option could not be added.") }
  }
  return <main className="light-screen workspace-screen admin-menu-screen"><header className="page-header"><h1>MENU</h1><button type="button" onClick={() => begin()}>ADD ITEM</button></header><p>Items with 0 stock are marked Unavailable.</p>
    <section className="data-table admin-menu"><div className="data-row table-head"><span>ITEM</span><span>PRICE</span><span>STATIONS</span><span>PREP</span><span>STOCK</span><span>STATUS</span><span>ACTIONS</span></div>{data.items.map((item) => <div className="data-row" key={item.id}><strong>{item.name}</strong><span>₹{item.price.toFixed(2)}</span><span>{item.stations.join(" · ")}</span><span>{Math.ceil(item.prepSeconds / 60)} min</span><span className="admin-stock-cell">{item.stock}<label className="stock-toggle"><input type="checkbox" checked={item.stock > 0 && item.available} disabled={item.stock === 0} aria-label={"Toggle availability for " + item.name} onChange={() => void deactivate(item)} /><span aria-hidden="true"></span></label></span><span className={item.stock > 0 && item.available ? "availability-chip available" : "availability-chip unavailable"}>{item.stock > 0 && item.available ? "Available" : "Unavailable"}</span><div><button type="button" onClick={() => begin(item)}>EDIT</button><button type="button" onClick={() => { setStockItem(item); setMessage("") }}>STOCK</button><button type="button" onClick={() => void archive(item)}>ARCHIVE</button></div></div>)}</section>
    {stockItem && <section className="menu-editor" role="dialog" aria-modal="true"><h2>ADJUST STOCK · {stockItem.name}</h2><p>Current quantity: {stockItem.stock}</p><label>QUANTITY CHANGE<input type="number" step="1" value={stockDelta} onChange={(event) => setStockDelta(event.target.value)} /></label><label>REASON<input maxLength={250} value={stockReason} onChange={(event) => setStockReason(event.target.value)} /></label><p role="status">{message}</p><button type="button" onClick={() => void saveStockAdjustment()}>SAVE ADJUSTMENT</button><button type="button" onClick={() => setStockItem(null)}>CLOSE</button></section>}
    {editing.name !== "" || isNew ? <section className="menu-editor"><h2>{isNew ? "ADD MENU ITEM" : `EDIT ${editing.name}`}</h2><label>NAME<input value={editing.name} onChange={(event) => setEditing((current) => ({ ...current, name: event.target.value }))} /></label><label>CATEGORY<select value={editing.category} onChange={(event) => setEditing((current) => ({ ...current, category: event.target.value }))}><option value="">Select category</option>{categories.map((cat) => <option key={cat}>{cat}</option>)}</select></label><label>PRICE<input type="number" min="0" step="0.01" value={editing.price} onChange={(event) => setEditing((current) => ({ ...current, price: event.target.value }))} /></label><label>PREP SECONDS<input type="number" min="1" step="1" value={editing.prepSeconds} onChange={(event) => setEditing((current) => ({ ...current, prepSeconds: event.target.value }))} /></label><label>STOCK<input type="number" min="0" step="1" value={editing.stock} onChange={(event) => setEditing((current) => ({ ...current, stock: event.target.value }))} /></label><label><input type="checkbox" disabled={Number(editing.stock) === 0} checked={Number(editing.stock) > 0 && editing.available} onChange={(event) => setEditing((current) => ({ ...current, available: event.target.checked }))} />AVAILABLE</label><fieldset><legend>STATIONS</legend>{stations.map((station) => <label key={station}><input type="checkbox" checked={editing.stations.includes(station)} onChange={(event) => setEditing((current) => ({ ...current, stations: event.target.checked ? [...current.stations, station] : current.stations.filter((entry) => entry !== station) }))} />{station}</label>)}</fieldset><fieldset><legend>OPTION GROUPS</legend>{data.groups.map((group) => <label key={group.id}><input type="checkbox" checked={editing.modifierGroups.includes(group.id)} onChange={(event) => setEditing((current) => ({ ...current, modifierGroups: event.target.checked ? [...current.modifierGroups, group.id] : current.modifierGroups.filter((id) => id !== group.id) }))} />{group.name}</label>)}<label>NEW GROUP NAME<input value={groupName} onChange={(event) => setGroupName(event.target.value)} /></label><label>SELECTION<select value={groupSelection} onChange={(event) => setGroupSelection(event.target.value as "SINGLE" | "MULTIPLE")}><option value="SINGLE">SINGLE</option><option value="MULTIPLE">MULTIPLE</option></select></label><button type="button" disabled={!editing.id} onClick={() => void addOptionGroup()}>ADD GROUP</button><label>GROUP FOR NEW OPTION<select value={modifierGroupId} onChange={(event) => setModifierGroupId(event.target.value)}><option value="">SELECT GROUP</option>{data.groups.filter((group) => editing.modifierGroups.includes(group.id)).map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}</select></label><label>OPTION NAME<input value={modifierName} onChange={(event) => setModifierName(event.target.value)} /></label><label>OPTION PRICE CHANGE<input type="number" step="0.01" value={modifierPrice} onChange={(event) => setModifierPrice(event.target.value)} /></label><button type="button" disabled={!editing.id} onClick={() => void addModifier()}>ADD OPTION</button></fieldset><p role="status">{message}</p><button type="button" onClick={() => void save()}>SAVE ITEM</button><button type="button" onClick={() => { setEditing({ ...emptyItem }); setIsNew(false) }}>CLOSE</button></section> : message && <p role="status">{message}</p>}
  </main>
}
