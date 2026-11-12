import { request } from "./request"

export type CategoryRecord = { id: number; name: string; gstRate: string; active: boolean }

export async function loadCategories(): Promise<CategoryRecord[]> {
  return request<CategoryRecord[]>("/admin/categories")
}

export async function createCategory(name: string, gstRate: number): Promise<{ id: number }> {
  return request<{ id: number }>("/admin/categories", {
    method: "POST",
    body: JSON.stringify({ name: name.trim(), gstRate }),
  })
}

export async function updateCategory(
  id: number,
  patch: { name?: string; gstRate?: number; active?: boolean }
): Promise<void> {
  await request<void>(`/admin/categories/${id}`, {
    method: "PATCH",
    body: JSON.stringify(patch),
  })
}
