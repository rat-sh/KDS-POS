import { request } from "./request"

export async function updateStationConfig(id: string, patch: { name?: string; displayOrder?: number; active?: boolean }) { await request<void>(`/admin/stations/${id}`, { method: "PATCH", body: JSON.stringify(patch) }) }
