import type { MenuItem, Modifier } from "../domain/catalog.types"
import type { Order, OrderLine } from "../domain/core.types"
type Rates = Record<string, number>

export type GstBreakdown = { category: string; rate: number; amount: number }

export function calculateOrder(
  orderOrLines: OrderLine[] | Pick<Order, "lines">,
  items: MenuItem[] = [],
  modifiers: Modifier[] = [],
  rates: Rates = {}
) {
  const lines = Array.isArray(orderOrLines) ? orderOrLines : orderOrLines.lines
  const details = lines.flatMap((line) => {
    const item = items.find((entry) => entry.id === line.itemId)
    if (!item) return []
    const modifierTotal = line.modifierIds.reduce(
      (sum, id) => sum + (modifiers.find((entry) => entry.id === id)?.price ?? 0),
      0
    )
    const net = (item.price + modifierTotal) * line.qty
    const gst = Math.round(net * (rates[item.category] ?? 0) * 100) / 100
    return [{ ...line, item, modifierTotal, net, gst, gross: net + gst }]
  })
  const subtotal = details.reduce((sum, line) => sum + line.net, 0)
  // Per-category GST breakdown — dynamic, no hardcoded categories
  const gstByCategory = new Map<string, { rate: number; amount: number }>()
  for (const line of details) {
    const cat = line.item.category
    const existing = gstByCategory.get(cat) ?? { rate: rates[cat] ?? 0, amount: 0 }
    gstByCategory.set(cat, { rate: existing.rate, amount: existing.amount + line.gst })
  }
  const gstBreakdown: GstBreakdown[] = [...gstByCategory.entries()].map(([category, { rate, amount }]) => ({
    category,
    rate,
    amount,
  }))
  const totalGst = gstBreakdown.reduce((sum, entry) => sum + entry.amount, 0)
  return { lines: details, subtotal, gstBreakdown, total: subtotal + totalGst }
}
