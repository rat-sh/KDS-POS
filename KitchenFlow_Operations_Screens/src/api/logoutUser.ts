import { clearSession } from "../store/auth"
import { request } from "./request"
export async function logoutUser() { await request<void>("/auth/logout", { method: "POST" }).catch(() => undefined); clearSession() }
