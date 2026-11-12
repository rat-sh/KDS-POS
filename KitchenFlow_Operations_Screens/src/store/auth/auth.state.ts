import type { AuthSession } from "./auth.types"
export const authState: { current: AuthSession | null } = { current: null }
