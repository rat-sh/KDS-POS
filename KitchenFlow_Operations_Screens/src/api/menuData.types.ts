import type { MenuItem, Modifier, ModifierGroup } from "../domain/catalog.types"
export type MenuRates = Record<string, number>
export type MenuData = { items: MenuItem[]; groups: ModifierGroup[]; modifiers: Modifier[]; rates: MenuRates }
