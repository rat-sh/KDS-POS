import { request } from "./request"
import type { ApiHealth } from "./api.types"
export async function checkBackend() { return request<ApiHealth>("/actuator/health", {}, { root: true }) }
