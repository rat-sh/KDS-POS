import type { AuthSession } from "./auth.types"
import { authState } from "./auth.state"
export function setSession(session: AuthSession) {
  authState.current = session
}
