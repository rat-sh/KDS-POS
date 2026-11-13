import { readSession } from "../store/auth"
import type { ApiBoard, BoardUpdate } from "./board.types"
import { apiBase } from "./request"

export async function openBoardStream(signal: AbortSignal, onBoard: BoardUpdate) {
  const headers = new Headers({ Accept: "text/event-stream", "X-Correlation-Id": crypto.randomUUID() })
  const token = readSession()?.accessToken
  if (token) headers.set("Authorization", `Bearer ${token}`)
  const response = await fetch(`${apiBase}/stream/board`, { headers, credentials: "include", signal })
  if (!response.ok || !response.body) throw new Error(`Board stream failed with ${response.status}`)

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ""
  try {
    while (!signal.aborted) {
      const chunk = await reader.read()
      if (chunk.done) return
      buffer += decoder.decode(chunk.value, { stream: true })
      const events = buffer.split("\n\n")
      buffer = events.pop() || ""
      events.forEach((event) => {
        const data = event.split("\n").find((line) => line.startsWith("data:"))
        if (!data) return
        const board = JSON.parse(data.slice(5).trim()) as ApiBoard
        onBoard({ preparing: board.preparing, ready: board.ready })
      })
    }
  } finally {
    reader.releaseLock()
  }
}
