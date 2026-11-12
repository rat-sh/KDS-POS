import { useEffect, useRef, useState } from "react"
import type { MenuItem } from "../../../domain/catalog.types"
import type { OrderLine } from "../../../domain/core.types"

type ModifierGroup = { id: string; name: string; type: "single" | "multiple"; min: number; max: number; modifiers: string[] }
type Modifier = { id: string; name: string; price: number; group: string }
type Props = { item: MenuItem; groups: ModifierGroup[]; modifiers: Modifier[]; onClose: () => void; onConfirm: (line: OrderLine) => void }

export function ModifierSheet({ item, groups, modifiers, onClose, onConfirm }: Props) {
  const [selected, setSelected] = useState<string[]>([])
  const sheetRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    const sheet = sheetRef.current
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const getFocusable = () => Array.from(sheet?.querySelectorAll<HTMLElement>("button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex='-1'])") ?? [])
    getFocusable()[0]?.focus()

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault()
        onClose()
        return
      }
      if (event.key !== "Tab") return
      const focusable = getFocusable()
      if (!focusable.length) {
        event.preventDefault()
        sheet?.focus()
        return
      }
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      const focusIsOutside = !sheet?.contains(document.activeElement)
      if (event.shiftKey && (document.activeElement === first || focusIsOutside)) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && (document.activeElement === last || focusIsOutside)) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener("keydown", handleKeyDown)
    return () => {
      document.removeEventListener("keydown", handleKeyDown)
      previousFocus?.focus()
    }
  }, [onClose])

  const toggle = (id: string, groupId: string) => {
    const group = groups.find((entry) => entry.id === groupId)
    if (!group) return
    setSelected((current) => current.includes(id)
      ? current.filter((entry) => entry !== id)
      : group.type === "single"
        ? [...current.filter((entry) => !group.modifiers.includes(entry)), id]
        : current.filter((entry) => group.modifiers.includes(entry)).length >= group.max
          ? current
          : [...current, id])
  }
  const canAdd = item.modifierGroups.every((groupId) => {
    const group = groups.find((entry) => entry.id === groupId)
    return !group || selected.filter((id) => group.modifiers.includes(id)).length >= group.min
  })

  return (
    <div className="sheet-backdrop" role="presentation">
      <section ref={sheetRef} className="modifier-sheet" role="dialog" aria-modal="true" aria-labelledby="modifier-sheet-title">
        <header>
          <span>MODIFIERS</span>
          <h2 id="modifier-sheet-title">{item.name}</h2>
        </header>
        <div className="modifier-content">
          {item.modifierGroups.map((groupId) => {
            const group = groups.find((entry) => entry.id === groupId)
            if (!group) return null
            return (
              <fieldset key={group.id}>
                <legend>{group.name} {group.min ? `(MIN ${group.min})` : ""}</legend>
                <div>
                  {group.modifiers.map((id) => {
                    const modifier = modifiers.find((entry) => entry.id === id)
                    if (!modifier) return null
                    const isSelected = selected.includes(id)
                    return (
                      <button type="button" className={isSelected ? "selected" : ""} aria-pressed={isSelected} key={id} onClick={() => toggle(id, group.id)}>
                        {modifier.name}<small>{modifier.price ? `₹${modifier.price}` : "NO CHARGE"}</small>
                      </button>
                    )
                  })}
                </div>
              </fieldset>
            )
          })}
        </div>
        <footer>
          <button type="button" onClick={onClose}>CANCEL</button>
          <button type="button" disabled={!canAdd} onClick={() => onConfirm({ itemId: item.id, qty: 1, modifierIds: selected })}>ADD TO ORDER</button>
        </footer>
      </section>
    </div>
  )
}
