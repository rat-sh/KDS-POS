import type { MenuItem } from "../domain/catalog.types"
import { request } from "./request"

export async function saveMenuItem(item: MenuItem, create: boolean) {
  await request<void>(create ? "/admin/items" : `/admin/items/${item.id}`, { method: create ? "POST" : "PATCH", body: JSON.stringify(item) })
}
