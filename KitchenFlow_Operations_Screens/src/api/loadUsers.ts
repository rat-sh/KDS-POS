import type { ApiPage, ApiUser } from "./api.types"
import { mapApiUser } from "./mapApiUser"
import { request } from "./request"

export async function loadUsers() { const result = await request<ApiPage<ApiUser> | ApiUser[]>("/admin/users?page=0&size=100"); return (Array.isArray(result) ? result : result.content).map(mapApiUser) }
