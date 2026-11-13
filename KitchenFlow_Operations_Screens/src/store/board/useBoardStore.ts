import { useEffect, useSyncExternalStore } from "react"
import { openBoardStream } from "../../api"
import { applyBoard } from "./applyBoard"
import { boardListeners, boardState } from "./board.state"
import { refreshBoard } from "./refreshBoard"

const MAX_RECONNECT_DELAY_MS = 30_000

function waitForRetry(delayMs: number, signal: AbortSignal) {
  return new Promise<void>((resolve) => {
    const finish = () => {
      window.clearTimeout(timeout)
      signal.removeEventListener("abort", finish)
      resolve()
    }
    const timeout = window.setTimeout(finish, delayMs)
    signal.addEventListener("abort", finish, { once: true })
    if (signal.aborted) finish()
  })
}

async function reconnectBoardStream(signal: AbortSignal) {
  let retryDelayMs = 1_000
  while (!signal.aborted) {
    const connectedAt = Date.now()
    try {
      await openBoardStream(signal, applyBoard)
    } catch {
      // Polling remains active while the stream backs off and reconnects.
    }
    if (signal.aborted) return
    if (Date.now() - connectedAt > MAX_RECONNECT_DELAY_MS) retryDelayMs = 1_000
    await waitForRetry(retryDelayMs, signal)
    retryDelayMs = Math.min(retryDelayMs * 2, MAX_RECONNECT_DELAY_MS)
  }
}

export function useBoardStore() {
  const snapshot = useSyncExternalStore(
    (listener) => {
      boardListeners.add(listener)
      return () => boardListeners.delete(listener)
    },
    () => boardState.snapshot,
    () => boardState.snapshot,
  )

  useEffect(() => {
    const controller = new AbortController()
    void refreshBoard()
    void reconnectBoardStream(controller.signal)
    const id = window.setInterval(() => void refreshBoard(), 5_000)
    return () => {
      controller.abort()
      window.clearInterval(id)
    }
  }, [])

  return snapshot
}
