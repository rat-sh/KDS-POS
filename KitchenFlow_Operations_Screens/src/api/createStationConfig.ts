import { request } from "./request"

export async function createStationConfig(name: string, displayOrder: number) {
  return request<{ id: number }>("/admin/stations", {
    method: "POST",
    body: JSON.stringify({ name, displayOrder }),
  })
}
