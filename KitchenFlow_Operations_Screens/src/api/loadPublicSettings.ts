import type { ApiSettings } from "./api.types"
import { request } from "./request"

export async function loadPublicSettings() {
  return request<Pick<ApiSettings, "outletName">>("/config/public")
}
