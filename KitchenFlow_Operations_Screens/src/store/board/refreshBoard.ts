import { loadBoard } from "../../api"
import { boardListeners, boardState } from "./board.state"
import { applyBoard } from "./applyBoard"

export async function refreshBoard() { boardState.snapshot = { ...boardState.snapshot, loading: true }; boardListeners.forEach((listener) => listener()); try { applyBoard(await loadBoard()) } catch (error) { boardState.snapshot = { ...boardState.snapshot, loading: false, error: error instanceof Error ? error.message : "Unable to load the public status board." } } finally { boardListeners.forEach((listener) => listener()) } }
