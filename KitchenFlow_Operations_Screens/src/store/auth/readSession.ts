import { authState } from "./auth.state"
export function readSession() {
  return authState.current
}
