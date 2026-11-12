import type { MenuItem } from "../catalog.types"
import type { OrderLine } from "../core.types"
export function lineGst(line: OrderLine, item: MenuItem, modifierTotal: number, rate: number) { const net = (item.price + modifierTotal) * line.qty; return Math.round(net * rate * 100) / 100 }
