import { routeByRole } from "./permissions.constants"
import type { Role } from "../core.types"

export function canVisit(role: Role, page: string) {
  return routeByRole[role].includes(page)
}
