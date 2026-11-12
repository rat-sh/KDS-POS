import { useSyncExternalStore } from "react"
import { settingsSnapshot } from "./settingsSnapshot"
import { subscribeSettings } from "./subscribeSettings"
export function useSettingsStore() { return useSyncExternalStore(subscribeSettings, settingsSnapshot, settingsSnapshot) }
