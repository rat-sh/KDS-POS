import { useEffect, useState } from "react"
import type { ApiTicketRecord } from "../../../api/api.types"
import { loadStationRecords, type TicketRecordState } from "../../../api/loadStationRecords"
import type { Station } from "../../../domain/core.types"

type Props = { station: Station; onClose: () => void }

const states: TicketRecordState[] = ["ACCEPTED", "SERVED", "ALL"]

export function StationRecord({ station, onClose }: Props) {
  const [state, setState] = useState<TicketRecordState>("ALL")
  const [search, setSearch] = useState("")
  const [records, setRecords] = useState<ApiTicketRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError("")
    loadStationRecords(station, state, search, controller.signal)
      .then(setRecords)
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : "Unable to load station records.")
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [station, state, search])

  const time = (value: string | null) => value ? new Date(value).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: false }) : "--:--"
  const duration = (seconds: number) => seconds ? `${Math.floor(seconds / 60)}m ${seconds % 60}s` : "--"

  return <div className="kds-sheet-backdrop">
    <section className="kds-record-sheet" role="dialog" aria-modal="true" aria-labelledby="station-record-title">
      <header><h2 id="station-record-title">TODAY'S RECORD · {station.toUpperCase()}</h2><button onClick={onClose}>CLOSE</button></header>
      <div role="group" aria-label="Filter ticket records" className="kds-record-filters">
        {states.map((value) => <button key={value} type="button" aria-pressed={state === value} onClick={() => setState(value)}>{value}</button>)}
      </div>
      <input aria-label="Search by token" placeholder="SEARCH TOKEN" value={search} onChange={(event) => setSearch(event.target.value)} />
      {loading && <p role="status">Loading records…</p>}
      {error && <p role="alert">{error}</p>}
      {!loading && !error && records.length === 0 && <p className="kds-empty">NO TICKET RECORDS</p>}
      {records.map((record) => <article key={record.ticketId}>
        <b>#{record.token} · {record.orderNumber} · {record.state}</b>
        <small>ACCEPTED {time(record.acceptedAt)}{record.acceptedByName ? ` · ${record.acceptedByName}` : ""} · STARTED {time(record.startedAt)} · DONE {time(record.doneAt)}</small>
        <small>SERVED {time(record.servedAt)}{record.servedByName ? ` · ${record.servedByName}` : ""}</small>
        <small>ACCEPT TO START {duration(record.acceptToStartSeconds)} · COOK {duration(record.cookSeconds)} · PICKUP {duration(record.pickupSeconds)} · RECALLS {record.recallCount}</small>
      </article>)}
    </section>
  </div>
}
