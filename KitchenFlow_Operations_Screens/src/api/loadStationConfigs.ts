import type { StationConfig } from "../domain/catalog.types"
import type { ApiStation } from "./api.types"
import { request } from "./request"

export async function loadStationConfigs(): Promise<StationConfig[]> { const result = await request<{ content: ApiStation[] } | ApiStation[]>("/stations?includeInactive=true"); const stations = Array.isArray(result) ? result : result.content; return stations.map((station) => ({ id: String(station.id), name: station.name, active: station.active, displayOrder: station.displayOrder, mappedItems: [] })) }
