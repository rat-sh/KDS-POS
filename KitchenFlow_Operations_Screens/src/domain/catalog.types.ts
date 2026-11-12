import type { Station } from "./core.types"
export type ModifierGroup = { id: string; name: string; type: "single" | "multiple"; min: number; max: number; modifiers: string[] }
export type Modifier = { id: string; name: string; price: number; group: string }
export type MenuItem = { id: string; name: string; category: string; price: number; stations: Station[]; prepSeconds: number; available: boolean; stock: number; stockTracking: boolean; modifierGroups: string[]; version?: number }
export type StationConfig = { id: string; name: string; active: boolean; displayOrder: number; mappedItems: string[] }
