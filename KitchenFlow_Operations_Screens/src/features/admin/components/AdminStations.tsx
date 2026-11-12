import { useEffect, useState } from "react"
import { createStationConfig, loadStationConfigs, updateStationConfig } from "../../../api"
import type { StationConfig } from "../../../domain/catalog.types"

export function AdminStations() {
  const [stationConfigs, setStationConfigs] = useState<StationConfig[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [newName, setNewName] = useState("")
  useEffect(() => { void loadStationConfigs().then(setStationConfigs).catch((cause) => setError(cause instanceof Error ? cause.message : "Stations could not be loaded.")).finally(() => setLoading(false)) }, [])
  if (loading) return <main className="light-screen workspace-screen loading-screen">LOADING STATIONS…</main>
  if (error) return <main className="light-screen workspace-screen loading-screen">{error}</main>
  const save = async (station: StationConfig, patch: { name?: string; displayOrder?: number; active?: boolean }) => { try { await updateStationConfig(station.id, patch); setStationConfigs((current) => current.map((entry) => entry.id === station.id ? { ...entry, ...patch } : entry)) } catch (cause) { setError(cause instanceof Error ? cause.message : "Station could not be updated.") } }
  const add = async () => { const name = newName.trim(); if (!name || stationConfigs.some((station) => station.name.toLowerCase() === name.toLowerCase())) { setError("Enter a unique station name."); return } try { const result = await createStationConfig(name, stationConfigs.length + 1); setStationConfigs((current) => [...current, { id: String(result.id), name, active: true, displayOrder: current.length + 1, mappedItems: [] }]); setNewName(""); setError("") } catch (cause) { setError(cause instanceof Error ? cause.message : "Station could not be added.") } }
  return <main className="light-screen workspace-screen"><header className="page-header"><div><h1>STATIONS</h1></div><button type="button" onClick={() => void add()}>ADD STATION</button></header>{error && <p role="alert">{error}</p>}<label>NEW STATION NAME<input maxLength={80} value={newName} onChange={(event) => setNewName(event.target.value)} /></label><section className="station-config-grid">{stationConfigs.map((station: StationConfig) => <article className="station-config-card" key={station.id}><div className="config-card-header"><h2>{station.name}</h2><button type="button" onClick={() => void save(station, { active: !station.active })}>{station.active ? "DEACTIVATE" : "ACTIVATE"}</button></div><label>DISPLAY NAME<input defaultValue={station.name} onBlur={(event) => void save(station, { name: event.target.value.trim() || station.name })} /></label><label>DISPLAY ORDER<input type="number" defaultValue={station.displayOrder} onBlur={(event) => void save(station, { displayOrder: Number(event.target.value) || station.displayOrder })} /></label><section className="mapped-items-section"><strong>MAPPED ITEMS</strong><div className="mapped-items-list">{station.mappedItems.map((item: string) => <span className="mapped-chip" key={item}>{item}</span>)}</div></section></article>)}</section></main>
}
