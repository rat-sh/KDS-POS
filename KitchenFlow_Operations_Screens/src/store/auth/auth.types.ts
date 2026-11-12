import type { User } from "../../domain/core.types"
export type AuthSession = { accessToken: string; expiresAt: number; user: User }
