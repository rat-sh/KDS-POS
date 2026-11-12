import { useEffect, useState } from "react"
import { loginUser, logoutUser, restoreSession, loadStationConfigs } from "../api"
import { setStationRegistry } from "../api/stationRegistry"
import type { User } from "../domain/core.types"
import { Login } from "../features/auth"
import { StatusBoard } from "../features/board"
import { RoleApp } from "./RoleApp"
import { setOrders } from "../store/orders"

async function loadStations() {
  try {
    const stations = await loadStationConfigs()
    setStationRegistry(stations.map((s) => ({ id: Number(s.id), name: s.name })))
  } catch {
    // non-fatal: station names fall back to "Station-{id}"
  }
}

export function AppSession() {
  const [user, setUser] = useState<User | null>(null)
  const [restoring, setRestoring] = useState(true)

  useEffect(() => {
    void restoreSession().then((u) => {
      if (u) void loadStations()
      setUser(u)
    }).finally(() => setRestoring(false))
  }, [])

  if (location.pathname === "/status" || location.hash === "#/status") return <StatusBoard />
  if (restoring) return <main className="boot-session">Restoring secure session…</main>
  if (!user) return <Login onAuthenticate={async (username, password) => {
    const next = await loginUser(username, password)
    void loadStations()
    setUser(next)
    return next
  }} />

  return <RoleApp initialUser={user} onLogout={() => {
    void logoutUser()
    setOrders([])
    location.hash = ""
    setUser(null)
  }} />
}
