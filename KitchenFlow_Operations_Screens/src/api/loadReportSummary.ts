import type { ReportSummary } from "./report.types"
import { request } from "./request"

export function loadReportSummary(from: string, to: string, topPage: number, cancellationPage: number) {
  const query = new URLSearchParams({
    from,
    to,
    topPage: String(topPage),
    topSize: "8",
    cancellationPage: String(cancellationPage),
    cancellationSize: "20",
  })
  return request<ReportSummary>(`/reports/summary?${query.toString()}`)
}
