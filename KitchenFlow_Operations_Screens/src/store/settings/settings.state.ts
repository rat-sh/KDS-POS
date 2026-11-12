import type { Settings } from "./settings.types"
import { readSettings } from "./readSettings"
export const state: { current: Settings } = { current: readSettings() }
export const listeners = new Set<() => void>()
