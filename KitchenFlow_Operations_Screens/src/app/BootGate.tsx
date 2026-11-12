import { useEffect, useState, type ReactNode } from "react"
import { checkBackend, loadStationConfigs } from "../api"
import { setStationRegistry } from "../api/stationRegistry"

type BootState = {
  status: "checking" | "ready" | "error"
  step: string
  message?: string
}

export function BootGate({ children }: { children: ReactNode }) {
  const [state, setState] = useState<BootState>({ status: "checking", step: "Connecting to KitchenFlow" })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let active = true
    setState({ status: "checking", step: "Connecting to the Spring Boot API" })
    void checkBackend()
      .then(() => loadStationConfigs())
      .then((stations) => {
        setStationRegistry(stations.map((s) => ({ id: Number(s.id), name: s.name })))
        if (active) setState({ status: "ready", step: "Backend connected" })
      })
      .catch((error: unknown) => {
        if (active) setState({
          status: "error",
          step: "Backend connection failed",
          message: error instanceof Error ? error.message : "The Spring Boot API is unavailable.",
        })
      })
    return () => { active = false }
  }, [attempt])

  if (state.status === "ready") return <>{children}</>
  return (
    <main className="boot-screen">
      <section className="boot-card">
        <div className="login-mark">KF</div>
        <span className="section-label">KITCHENFLOW STARTUP</span>
        <h1>{state.step}</h1>
        <p>Connecting to the restaurant database and API.</p>
        <div className={`boot-progress boot-progress-size-${state.status === "checking" ? 48 : 100}`}><i /></div>
        <strong>{state.status === "checking" ? "48%" : "100%"}</strong>
        {state.status === "checking" && <small>Checking backend health…</small>}
        {state.status === "error" && <>
          <div className="login-error">{state.message}</div>
          <div className="boot-actions"><button type="button" onClick={() => setAttempt((value) => value + 1)}>RETRY CONNECTION</button></div>
        </>}
      </section>
    </main>
  )
}
