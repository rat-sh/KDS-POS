export type ReportPage<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number }

export type ReportSummary = {
  sales: string
  gst: string
  orders: number
  completed: number
  cancelled: number
  topItems: ReportPage<{ itemId: number; name: string; quantity: number }>
  prepTimeByStation: Array<{ stationId: number; stationName: string; averageSeconds: number }>
  ordersPerHour: Array<{ hour: number; count: number }>
  cancellations: ReportPage<{ id: number; token: number; reason: string | null; cancelledAt: string }>
}
