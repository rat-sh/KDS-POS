import { authState } from "./auth.state"
export function clearSession() {
  authState.current = null
}
