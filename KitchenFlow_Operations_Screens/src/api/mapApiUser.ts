import type { User } from "../domain/core.types"
import type { ApiUser } from "./api.types"
import { stationNameById } from "./stationRegistry"

export function mapApiUser(user: ApiUser): User { return { id: user.id, username: user.username, staffId: String(user.id), displayName: user.username, role: user.role[0] + user.role.slice(1).toLowerCase() as User["role"], stations: user.stationIds.map(stationNameById).filter(Boolean), active: user.active } }
