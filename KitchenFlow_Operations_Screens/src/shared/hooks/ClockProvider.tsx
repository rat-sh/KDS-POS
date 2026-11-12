import { createContext, useEffect, useState, type ReactNode } from "react"

export const clockContext = createContext(Date.now())

export function ClockProvider({ children }: { children: ReactNode }) {
  const [now, setNow] = useState(Date.now())
  useEffect(() => { const id = window.setInterval(() => setNow(Date.now()), 1000); return () => window.clearInterval(id) }, [])
  return <clockContext.Provider value={now}>{children}</clockContext.Provider>
}
