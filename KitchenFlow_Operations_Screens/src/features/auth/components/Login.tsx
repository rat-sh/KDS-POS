import { useState } from "react"
import type React from "react"
import type { User } from "../../../domain/core.types"
import { Icon } from "../../../shared/ui/Icon"

export function Login({ onAuthenticate }: { onAuthenticate: (username: string, password: string) => Promise<User> }) {
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const [busy, setBusy] = useState(false)

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (busy) return
    setBusy(true)
    setError("")
    try {
      await onAuthenticate(username.trim(), password)
    } catch (cause) {
      setError(cause && typeof cause === "object" && "message" in cause
        ? String(cause.message)
        : "Username or password is incorrect.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="login-screen auth-screen">
      <section className="login-brand auth-brand">
        <div className="login-mark">KF</div>
        <span>OUTLET OPERATIONS</span>
        <h1>KITCHEN<br />FLOW</h1>
        <p>One system from order entry to service.</p>
        <div className="auth-brand-note"><i />STAFF ACCESS · ROLE BASED</div>
      </section>
      <section className="auth-panel">
        <div className="auth-content">
          <div className="auth-heading">
            <h2>Welcome back</h2>
            <p>Sign in with the username and password provided by your outlet manager.</p>
          </div>
          {error && <div className="login-error" role="alert"><Icon name="alert" /><span>{error}</span></div>}
          <form className="auth-form" onSubmit={(event) => void submit(event)}>
            <label>USERNAME
              <input autoComplete="username" autoFocus maxLength={80} placeholder="Enter your username" required value={username} onChange={(event) => setUsername(event.target.value)} />
            </label>
            <label>PASSWORD
              <input autoComplete="current-password" type="password" placeholder="Enter your password" required value={password} onChange={(event) => setPassword(event.target.value)} />
            </label>
            <button className="login-submit auth-submit" type="submit" disabled={busy}>
              {busy ? "SIGNING IN…" : "SIGN IN"}<Icon name="chevron" />
            </button>
          </form>
          <p className="auth-account-note">New staff accounts are created by a manager in Admin → Users.</p>
        </div>
      </section>
    </main>
  )
}
