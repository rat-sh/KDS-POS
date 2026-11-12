import { useEffect, useState } from "react"
import { createCategory, loadCategories, updateCategory } from "../../../api/manageCategories"
import type { CategoryRecord } from "../../../api/manageCategories"
import { refreshMenu } from "../../../store/catalog"

export function AdminCategories() {
  const [categories, setCategories] = useState<CategoryRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [message, setMessage] = useState("")
  const [newName, setNewName] = useState("")
  const [newRate, setNewRate] = useState("0")

  useEffect(() => {
    void loadCategories()
      .then(setCategories)
      .catch((cause) => setError(cause instanceof Error ? cause.message : "Categories could not be loaded."))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <main className="light-screen workspace-screen loading-screen">LOADING CATEGORIES…</main>
  if (error && categories.length === 0) return <main className="light-screen workspace-screen loading-screen">{error}</main>

  const add = async () => {
    const name = newName.trim()
    const rate = Number(newRate)
    if (!name) { setMessage("Enter a category name."); return }
    if (!Number.isFinite(rate) || rate < 0 || rate > 100) { setMessage("GST rate must be between 0 and 100."); return }
    if (categories.some((cat) => cat.name.toLowerCase() === name.toLowerCase())) { setMessage("A category with this name already exists."); return }
    try {
      const created = await createCategory(name, rate)
      setCategories((current) => [...current, { id: created.id, name, gstRate: rate.toFixed(2), active: true }])
      setNewName("")
      setNewRate("0")
      setMessage("Category added.")
      await refreshMenu()
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : "Category could not be added.")
    }
  }

  const toggle = async (cat: CategoryRecord) => {
    try {
      await updateCategory(cat.id, { active: !cat.active })
      setCategories((current) => current.map((entry) => entry.id === cat.id ? { ...entry, active: !cat.active } : entry))
      await refreshMenu()
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : "Category could not be updated.")
    }
  }

  const rename = async (cat: CategoryRecord, name: string) => {
    const trimmed = name.trim()
    if (!trimmed || trimmed === cat.name) return
    try {
      await updateCategory(cat.id, { name: trimmed })
      setCategories((current) => current.map((entry) => entry.id === cat.id ? { ...entry, name: trimmed } : entry))
      setMessage("Category updated.")
      await refreshMenu()
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : "Category could not be renamed.")
    }
  }

  const setRate = async (cat: CategoryRecord, rate: string) => {
    const parsed = Number(rate)
    if (!Number.isFinite(parsed) || parsed < 0 || parsed > 100) return
    try {
      await updateCategory(cat.id, { gstRate: parsed })
      setCategories((current) => current.map((entry) => entry.id === cat.id ? { ...entry, gstRate: parsed.toFixed(2) } : entry))
      setMessage("GST rate updated.")
      await refreshMenu()
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : "Rate could not be updated.")
    }
  }

  return (
    <main className="light-screen workspace-screen">
      <header className="page-header">
        <div>
          <h1>CATEGORIES</h1>
          <p>Define your menu categories and their GST rates. Menu items are assigned to these categories.</p>
        </div>
      </header>
      {error && <p role="alert">{error}</p>}
      <section className="data-table">
        <div className="data-row table-head">
          <span>NAME</span><span>GST RATE (%)</span><span>STATUS</span><span>ACTIONS</span>
        </div>
        {categories.length === 0 && (
          <p className="empty-state">No categories yet. Add your first category below to start building your menu.</p>
        )}
        {categories.map((cat) => (
          <div className="data-row" key={cat.id}>
            <input
              defaultValue={cat.name}
              maxLength={80}
              onBlur={(event) => void rename(cat, event.target.value)}
            />
            <input
              type="number"
              min="0"
              max="100"
              step="0.01"
              defaultValue={Number(cat.gstRate)}
              onBlur={(event) => void setRate(cat, event.target.value)}
            />
            <span className={cat.active ? "availability-chip available" : "availability-chip unavailable"}>
              {cat.active ? "Active" : "Inactive"}
            </span>
            <button type="button" onClick={() => void toggle(cat)}>
              {cat.active ? "DEACTIVATE" : "ACTIVATE"}
            </button>
          </div>
        ))}
      </section>
      <section className="menu-editor">
        <h2>ADD CATEGORY</h2>
        <label>NAME<input maxLength={80} value={newName} onChange={(event) => setNewName(event.target.value)} /></label>
        <label>GST RATE (%)<input type="number" min="0" max="100" step="0.01" value={newRate} onChange={(event) => setNewRate(event.target.value)} /></label>
        {message && <p role="status">{message}</p>}
        <button type="button" onClick={() => void add()}>ADD CATEGORY</button>
      </section>
    </main>
  )
}

