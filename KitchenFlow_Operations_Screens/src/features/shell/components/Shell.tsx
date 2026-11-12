import type { ReactNode } from "react"
import type { User } from "../../../domain/core.types"
export function Shell({ user, page, nav, setPage, onLogout, children, readyCount = 0 }: { user: User; page: string; nav: string[]; setPage: (page: string) => void; onLogout: () => void; children: ReactNode; readyCount?: number }) {
  const station = page === "Station"
  return <div className={`role-shell ${station ? "shell-dark" : ""} ${station ? "shell-kitchen" : ""}`}>
    {station && <nav className="station-route-nav" aria-label="Main navigation"><a href="#/station" aria-current="page">Station</a></nav>}
    {!station && <header className="shell-header"><button className="shell-brand" onClick={() => setPage(nav[0] ?? page)}><b>KF</b><strong>KITCHENFLOW</strong></button>
      <nav aria-label="Main navigation">{nav.map((item) => <button type="button" key={item} aria-current={page === item ? "page" : undefined} className={page === item ? "active" : ""} onClick={() => setPage(item)}>{item}{item === "Orders" && readyCount > 0 && <b className="ready-badge">{readyCount}</b>}</button>)}</nav>
      <div className="shell-user"><div><strong>{user.displayName ?? user.username}</strong><span>{user.role.toUpperCase()} · ID {user.staffId ?? user.username}</span></div><button type="button" onClick={onLogout}>LOG OUT</button></div>
    </header>}
    <div className="shell-content">{children}</div>
  </div>
}
