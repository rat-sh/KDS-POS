import type { ApiStation, ApiTicketRecord } from "./api.types"
import { request } from "./request"

export type TicketRecordState = "ACCEPTED" | "SERVED" | "ALL"

export async function loadStationRecords(stationName: string, state: TicketRecordState, token: string, signal?: AbortSignal): Promise<ApiTicketRecord[]> {
  const result = await request<{ content: ApiStation[] } | ApiStation[]>("/stations?includeInactive=true", { signal })
  const stations = Array.isArray(result) ? result : result.content
  const station = stations.find((entry) => entry.name.toLowerCase() === stationName.toLowerCase())
  if (!station) return []
  const query = new URLSearchParams({ state })
  if (token.trim()) query.set("token", token.trim())
  const records = await request<{ content: ApiTicketRecord[] }>(`/stations/${station.id}/records?${query}`, { signal })
  return records.content
}
