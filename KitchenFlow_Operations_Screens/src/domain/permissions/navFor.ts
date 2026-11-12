import { navByRole } from "./permissions.constants"
import type { Role } from "../core.types"
export function navFor(role: Role) { return navByRole[role] }
