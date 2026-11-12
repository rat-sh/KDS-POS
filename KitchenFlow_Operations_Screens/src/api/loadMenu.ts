import { request } from "./request"
import { mapApiMenu } from "./mapApiMenu"
import type { ApiMenu } from "./api.types"
export async function loadMenu() { return mapApiMenu(await request<ApiMenu>("/menu?includeUnavailable=true")) }
