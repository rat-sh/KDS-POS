import type { ApiBoard, BoardData } from "./board.types"
import { request } from "./request"

export async function loadBoard(): Promise<BoardData> {
  const board = await request<ApiBoard>("/board")
  return { preparing: board.preparing, ready: board.ready }
}
