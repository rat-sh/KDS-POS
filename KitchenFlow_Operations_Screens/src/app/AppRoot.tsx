import { BootGate } from "./BootGate"
import { AppSession } from "./AppSession"
import { ClockProvider } from "../shared/hooks"

export function AppRoot() {
  return <ClockProvider><BootGate><AppSession /></BootGate></ClockProvider>
}
