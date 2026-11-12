import type { Station, User } from "../../../domain/core.types"
import type { Order } from "../../../domain/core.types"
import type { MenuItem } from "../../../domain/catalog.types"
import { averageStationPrepTime } from "../logic/averageStationPrepTime"
import { useOnlineStatus } from "../../../shared/hooks"

type Props = {
  user: User
  station: Station
  orders: Order[]
  items: MenuItem[]
  now: number
  onStation: (station: Station) => void
  onRecord: () => void
  onRecall: () => void
  onLogout?: () => void
}

export function KdsHeader({
  user,
  station,
  orders,
  items,
  now,
  onStation,
  onRecord,
  onRecall,
  onLogout,
}: Props) {
  const online = useOnlineStatus()
  const clock = new Date(now).toLocaleTimeString("en-IN", { hour12: false })
  return (
    <header className="kds-header">
      <div className="kds-brand">
        <b>KF</b>
        <strong>KITCHENFLOW</strong>
      </div>
      <nav className="kds-station" aria-label="Kitchen stations">
        {user.stations.length > 1 ? (
          user.stations.map((entry) => (
            <button
              type="button"
              key={entry}
              className={station === entry ? "active" : ""}
              onClick={() => onStation(entry)}
            >
              {entry.toUpperCase()}
            </button>
          ))
        ) : (
          <strong>{station.toUpperCase()}</strong>
        )}
      </nav>
      {user.stations.length > 1 && <select className="kds-station-menu" aria-label="Choose station" value={station} onChange={(event) => onStation(event.target.value as Station)}>{user.stations.map((entry) => <option key={entry}>{entry}</option>)}</select>}
      <span className="kds-clock">{clock}</span>
      <span className="kds-avg">
        AVG PREP TODAY {averageStationPrepTime(orders, station, items, now)}
      </span>
      <div className="kds-identity">
        <strong>{user.username}</strong>
      </div>
      <span className={`kds-live ${online ? "" : "offline"}`}>
        <i /> {online ? "LIVE" : "OFFLINE"}
      </span>
      <button className="kds-record" onClick={onRecord}>
        RECORD
      </button>
      <button className="kds-record" onClick={onRecall}>RECALL</button>
      {onLogout && (
        <button className="kds-logout" type="button" onClick={onLogout}>
          LOG OUT
        </button>
      )}
    </header>
  )
}
