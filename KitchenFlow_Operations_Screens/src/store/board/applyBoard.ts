import type { BoardData } from "../../api/board.types"
import { boardListeners, boardState } from "./board.state"

export function applyBoard(current: BoardData) { boardState.snapshot = { current, loading: false, lastUpdatedAt: Date.now() }; boardListeners.forEach((listener) => listener()) }
