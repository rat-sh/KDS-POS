import type { ApiSettings } from "./api.types"
import { request } from "./request"

export async function loadSettings() {
  return request<ApiSettings>("/admin/config")
}
