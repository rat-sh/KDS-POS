import { useOnlineStatus, useNow } from "../../../shared/hooks"
import { useBoardStore } from "../../../store/board"
import { useSettingsStore } from "../../../store/settings"
import { updateSettings } from "../../../store/settings"
import { loadPublicSettings } from "../../../api"
import { useEffect } from "react"

export function StatusBoard() {
  const board = useBoardStore(), now = useNow(), online = useOnlineStatus(), settings = useSettingsStore()
  useEffect(() => { void loadPublicSettings().then(updateSettings).catch(() => undefined) }, [])
  const { preparing, ready } = board.current
  return <main className="dark-screen status-screen public-status"><header className="status-header"><div className="outlet"><span>KITCHENFLOW</span><h1>{settings.outletName}</h1></div><div className="status-clock"><strong>{new Date(now).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: false })}</strong></div>{!online && <div className="reconnecting"><b>RECONNECTING</b><small>LIVE UPDATES PAUSED</small></div>}</header>{board.loading && <div className="status-sync">SYNCING LIVE STATUS…</div>}{board.error && <div className="status-sync">{board.error}</div>}{!board.loading && !preparing.length && !ready.length ? <div className="status-empty"><h2>NO ACTIVE ORDERS</h2><p>All orders have been completed</p></div> : <div className="token-columns"><section className="preparing-column"><header><h2>PREPARING</h2><span>{preparing.length}</span></header><div className="token-grid">{preparing.map((token) => <div className="token-tile" key={token}>{token}</div>)}</div></section><section className="ready-column"><header><h2>READY</h2><span>{ready.length}</span></header><div className="token-grid">{ready.map((token) => <div className="token-tile" key={token}><strong>{token}</strong></div>)}</div></section></div>}</main>
}
