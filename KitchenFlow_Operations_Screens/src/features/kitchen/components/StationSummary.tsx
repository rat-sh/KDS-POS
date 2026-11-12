import { Icon } from "../../../shared/ui/Icon"
type Props = { counts: Record<string, number> }
const metrics = [["NEW", "NEW"], ["IN_PROGRESS", "IN PROGRESS"], ["OVER_LIMIT", "Late"]]

export function StationSummary({ counts }: Props) {
  return <section className="kds-summary" aria-label="Ticket status">
    {metrics.map(([key, label]) => <div className="summary-count" key={key}><strong>{counts[key]}</strong><span>{label}</span></div>)}
    <div className="summary-legend" aria-label="Ticket age"><span><Icon name="check" size={16} />ON TIME</span><span><Icon name="clock" size={16} />DUE</span><span><Icon name="alert" size={16} />LATE</span></div>
    <div className="summary-flags"><span>PRIORITY <b>{counts.PRIORITY}</b></span><span>VIP <b>{counts.VIP}</b></span></div>
  </section>
}
