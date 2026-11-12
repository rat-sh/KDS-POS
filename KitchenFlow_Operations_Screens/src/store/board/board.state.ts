import type { BoardData } from "../../api/board.types"
export type BoardState = { current: BoardData; loading: boolean; error?: string; lastUpdatedAt?: number }
export const boardState: { snapshot: BoardState } = { snapshot: { current: { preparing: [], ready: [] }, loading: true } }
export const boardListeners = new Set<() => void>()
