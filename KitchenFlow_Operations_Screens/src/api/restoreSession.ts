import { clearSession, setSession } from "../store/auth"
import { mapApiUser } from "./mapApiUser"
import { request } from "./request"
import type { ApiLoginResponse } from "./api.types"
export async function restoreSession() {
  try {
    const refreshed = await request<ApiLoginResponse>(
      "/auth/refresh",
      { method: "POST" },
      { retryAuth: false },
    )
    const user = mapApiUser(refreshed.user)
    setSession({
      accessToken: refreshed.accessToken,
      expiresAt: Date.now() + refreshed.expiresIn * 1000,
      user,
    })
    return user
  } catch {
    clearSession()
    return null
  }
}
