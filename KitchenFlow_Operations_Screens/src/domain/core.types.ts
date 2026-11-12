/** Station name — resolved at runtime from the /api/v1/stations response. */
export type Station = string
export type Role = "Cashier" | "Kitchen" | "Manager" | "Admin"
export type OrderStatus = "CREATED" | "IN_KITCHEN" | "READY" | "CANCELLED" | "COMPLETED"
export type TicketStatus = "NEW" | "ACCEPTED" | "COOKING" | "READY" | "SERVED" | "CANCELLED" | "IN_PROGRESS" | "DONE"
export type OrderType = "DINE_IN" | "TAKEAWAY" | "DELIVERY"
export type PaymentMethod = "Cash" | "UPI" | "Card"
export type Capability = "PAY" | "START_TICKET" | "BUMP_TICKET" | "RECALL_TICKET" | "CANCEL_UNPAID" | "CANCEL_PAID" | "SET_PRIORITY_AT_ENTRY" | "SET_PRIORITY" | "COMPLETE_ORDER" | "MANAGE_STATIONS" | "MANAGE_USERS" | "MANAGE_MENU" | "VIEW_AUDIT" | "VIEW_REPORTS"

export type OrderLine = { itemId: string; qty: number; modifierIds: string[] }
export type Ticket = { id: string; station: Station; status: TicketStatus; acceptedAt?: number; startedAt?: number; completedAt?: number; servedAt?: number; acknowledgedAt?: number; cancelledReason?: string; version?: number }
export type TicketEvent = { ticket: string; order: string; token: number; table?: string; station: Station; from: TicketStatus; to: TicketStatus; actor: string; time: number; reason?: string }
export type AuditEvent = { action: string; actor: string; at: number; reason: string }
export type Order = { id: string; token: number; table?: string; type: OrderType; status: OrderStatus; createdAt: number; businessDay?: string; paidAt?: number; readyAt?: number; completedAt?: number; rush: boolean; priority?: boolean; priorityReason?: string; vip?: boolean; note?: string; lines: OrderLine[]; tickets: Ticket[]; events?: TicketEvent[]; audit?: AuditEvent[]; payment?: { method: PaymentMethod; reference?: string }; cancellation?: { reason: string; detail: string; actor: string; cancelledAt: number }; version?: number }
export type User = { id?: number; username: string; role: Role; stations: Station[]; active: boolean; staffId?: string; displayName?: string }
