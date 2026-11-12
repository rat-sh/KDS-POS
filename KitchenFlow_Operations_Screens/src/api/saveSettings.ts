import type { ApiSettings } from "./api.types"
import { request } from "./request"

export async function saveSettings(settings: ApiSettings) {
  await request<void>("/admin/config", {
    method: "PATCH",
    body: JSON.stringify(settings),
  })
}
