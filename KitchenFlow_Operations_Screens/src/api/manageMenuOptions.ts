import { request } from "./request"

export type ModifierGroupInput = { name: string; selectionType: "SINGLE" | "MULTIPLE"; minSelect: number; maxSelect: number }
export type ModifierInput = { name: string; priceDelta: number; active: boolean }

export async function createItemModifierGroup(itemId: string, group: ModifierGroupInput) {
  return request<{ id: number }>(`/admin/items/${itemId}/modifier-groups`, { method: "POST", body: JSON.stringify(group) })
}

export async function createModifier(groupId: string, modifier: ModifierInput) {
  return request<{ id: number }>(`/admin/modifier-groups/${groupId}/modifiers`, { method: "POST", body: JSON.stringify(modifier) })
}
