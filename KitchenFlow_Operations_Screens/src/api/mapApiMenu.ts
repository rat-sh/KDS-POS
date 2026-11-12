import type { MenuItem, Modifier, ModifierGroup } from "../domain/catalog.types"
import type { ApiMenu } from "./api.types"
import type { MenuData } from "./menuData.types"
import { stationNameById } from "./stationRegistry"

export function mapApiMenu(menu: ApiMenu): MenuData {
  const groupsById = new Map<string, ModifierGroup>()
  const modifiersById = new Map<string, Modifier>()
  const items: MenuItem[] = []
  const rates: MenuData["rates"] = {}

  for (const category of menu.categories) {
    const categoryName = category.name
    rates[categoryName] = Number(category.gstRate) / 100

    for (const item of category.items) {
      const modifierGroupIds = item.modifierGroups.map((group) => String(group.id))
      items.push({
        id: String(item.id),
        name: item.name,
        category: categoryName,
        price: Number(item.price),
        stations: item.stationIds.map((id) => stationNameById(id)).filter(Boolean) as string[],
        prepSeconds: item.targetPrepSeconds,
        available: item.available,
        stock: item.stockQuantity ?? 0,
        stockTracking: item.trackStock ?? false,
        modifierGroups: modifierGroupIds,
        version: item.version,
      })

      for (const group of item.modifierGroups) {
        const groupId = String(group.id)
        groupsById.set(groupId, {
          id: groupId,
          name: group.name,
          type: group.selectionType === "SINGLE" ? "single" : "multiple",
          min: group.minSelect,
          max: group.maxSelect,
          modifiers: group.modifiers.map((modifier) => String(modifier.id)),
        })
        for (const modifier of group.modifiers) {
          const modifierId = String(modifier.id)
          modifiersById.set(modifierId, {
            id: modifierId,
            name: modifier.name,
            price: Number(modifier.priceDelta),
            group: groupId,
          })
        }
      }
    }
  }

  return {
    items,
    groups: [...groupsById.values()],
    modifiers: [...modifiersById.values()],
    rates,
  }
}
