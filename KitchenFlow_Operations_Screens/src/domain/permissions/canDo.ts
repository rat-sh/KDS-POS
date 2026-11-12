import { capabilities } from "./permissions.constants"
import type { Capability, Role } from "../core.types"
export function canDo(role: Role, capability: Capability) { return capabilities[role].includes(capability) }
