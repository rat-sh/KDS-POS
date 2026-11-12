import type { Settings } from "./settings.types"
import { state, listeners } from "./settings.state"
export function updateSettings(patch: Partial<Settings>) { state.current = { ...state.current, ...patch }; listeners.forEach((listener) => listener()) }
