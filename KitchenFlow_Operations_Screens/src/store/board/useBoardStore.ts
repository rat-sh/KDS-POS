import { useEffect, useSyncExternalStore } from "react"
import { openBoardStream } from "../../api"
import { applyBoard } from "./applyBoard"
import { boardListeners, boardState } from "./board.state"
import { refreshBoard } from "./refreshBoard"

export function useBoardStore() { const snapshot = useSyncExternalStore((listener) => { boardListeners.add(listener); return () => boardListeners.delete(listener) }, () => boardState.snapshot, () => boardState.snapshot); useEffect(() => { const controller = new AbortController(); void refreshBoard(); void openBoardStream(controller.signal, applyBoard).catch(() => undefined); const id = window.setInterval(() => void refreshBoard(), 5000); return () => { controller.abort(); window.clearInterval(id) } }, []); return snapshot }
