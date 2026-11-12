import { homeByRole } from "./permissions.constants"
import type { Role } from "../core.types"
export function homeFor(role: Role) { return homeByRole[role] }
