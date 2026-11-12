import { listeners } from "./settings.state"
export function subscribeSettings(listener: () => void) { listeners.add(listener); return () => listeners.delete(listener) }
