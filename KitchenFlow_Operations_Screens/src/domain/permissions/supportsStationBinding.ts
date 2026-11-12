import type { Role } from "../core.types"

export function supportsStationBinding(role: Role) {
  return role === "Kitchen"
}
