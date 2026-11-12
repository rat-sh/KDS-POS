import type { User } from "../domain/core.types"
import { request } from "./request"

export async function saveUser(user: User, create: boolean, password?: string) {
  const body = JSON.stringify({ username: user.username, role: user.role, stations: user.stations, active: user.active, password })
  if (create) {
    const created = await request<{ id: number }>("/admin/users", {
      method: "POST",
      body,
    })
    return created.id
  }
  if (!user.id) throw new Error("Reload users before editing this account.")
  await request<void>(`/admin/users/${user.id}`, {
    method: "PATCH",
    body,
  })
  return user.id
}
