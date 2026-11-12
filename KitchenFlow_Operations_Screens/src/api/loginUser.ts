import { mapApiUser } from "./mapApiUser"
import { request } from "./request"
import { setSession } from "../store/auth"
import type { ApiLoginResponse } from "./api.types"
import type { User } from "../domain/core.types"

export async function loginUser(username: string, password: string) {
  const result = await request<ApiLoginResponse>("/auth/login", { method: "POST", body: JSON.stringify({ username, password }) })
  const user = mapApiUser(result.user)
  setSession({ accessToken: result.accessToken, expiresAt: Date.now() + result.expiresIn * 1000, user })
  return user
}
