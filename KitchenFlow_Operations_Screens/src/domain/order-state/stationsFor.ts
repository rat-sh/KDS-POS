import type { MenuItem } from "../catalog.types"
import type { OrderLine, Station } from "../core.types"
export function stationsFor(lines: OrderLine[], items: MenuItem[]) { return [...new Set(lines.flatMap((line) => items.find((item) => item.id === line.itemId)!.stations))] as Station[] }
