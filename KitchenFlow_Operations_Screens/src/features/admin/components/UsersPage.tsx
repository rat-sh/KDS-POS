import { useEffect, useState } from "react"
import { loadUsers, saveUser } from "../../../api"
import type { Role, User } from "../../../domain/core.types"
import { supportsStationBinding } from "../../../domain/permissions"
import { allStationNames } from "../../../api/stationRegistry"

const roles: Role[] = ["Cashier", "Kitchen", "Manager", "Admin"]

export function UsersPage() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [adding, setAdding] = useState(false)
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [role, setRole] = useState<Role>("Cashier")
  const [bindings, setBindings] = useState<string[]>([])
  const stations = allStationNames()
  useEffect(() => { void loadUsers().then(setUsers).catch((cause) => setError(cause instanceof Error ? cause.message : "Users could not be loaded.")).finally(() => setLoading(false)) }, [])
  const update = async (user: User, next: User) => {
    try { await saveUser(next, false); setUsers((current) => current.map((entry) => entry.username === user.username ? next : entry)) }
    catch (cause) { setError(cause instanceof Error ? cause.message : "User could not be saved.") }
  }
  const add = async () => {
    const name = username.trim()
    if (!name || users.some((entry) => entry.username === name)) { setError("Enter a unique username."); return }
    if (password.length < 12) { setError("Set a password with at least 12 characters."); return }
    const user: User = { username: name, role, stations: supportsStationBinding(role) ? bindings : [], active: true }
    try { const id = await saveUser(user, true, password); setUsers((current) => [...current, { ...user, id }]); setUsername(""); setPassword(""); setAdding(false); setError("") }
    catch (cause) { setError(cause instanceof Error ? cause.message : "User could not be added.") }
  }
  if (loading) return <main className="light-screen workspace-screen loading-screen">LOADING USERS…</main>
  return <main className="light-screen workspace-screen users-admin"><header className="page-header"><h1>USERS</h1><button type="button" onClick={() => setAdding((current) => !current)}>ADD USER</button></header>
    {error && <p role="alert">{error}</p>}{adding && <section className="user-editor"><label>USERNAME<input autoComplete="off" maxLength={80} value={username} onChange={(event) => setUsername(event.target.value)} /></label><label>TEMPORARY PASSWORD<input autoComplete="new-password" type="password" minLength={12} value={password} onChange={(event) => setPassword(event.target.value)} /><small>Use at least 12 characters and give it to the staff member securely.</small></label><label>ROLE<select value={role} onChange={(event) => setRole(event.target.value as Role)}>{roles.map((entry) => <option key={entry}>{entry}</option>)}</select></label>{supportsStationBinding(role) && <fieldset><legend>STATIONS</legend>{stations.map((station) => <label key={station}><input type="checkbox" checked={bindings.includes(station)} onChange={(event) => setBindings((current) => event.target.checked ? [...current, station] : current.filter((entry) => entry !== station))} />{station}</label>)}</fieldset>}<button type="button" onClick={() => void add()}>SAVE USER</button></section>}
    <section className="users-list">{users.map((user) => <article className="user-admin-row" key={user.username}><strong>{user.username}</strong><label>ROLE<select value={user.role} onChange={(event) => { const nextRole = event.target.value as Role; void update(user, { ...user, role: nextRole, stations: supportsStationBinding(nextRole) ? user.stations : [] }) }}>{roles.map((entry) => <option key={entry}>{entry}</option>)}</select></label>{supportsStationBinding(user.role) ? <fieldset><legend>STATIONS</legend>{stations.map((station) => <label key={station}><input type="checkbox" checked={user.stations.includes(station)} onChange={(event) => void update(user, { ...user, stations: event.target.checked ? [...user.stations, station] : user.stations.filter((entry) => entry !== station) })} />{station}</label>)}</fieldset> : <span>NO STATION BINDING</span>}<span>{user.active ? "ACTIVE" : "INACTIVE"}</span><button type="button" onClick={() => void update(user, { ...user, active: !user.active })}>{user.active ? "DEACTIVATE" : "ACTIVATE"}</button></article>)}</section>
  </main>
}
