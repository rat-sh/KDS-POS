import type { Ticket, TicketStatus } from "../domain/core.types"
import type { ApiTicket } from "./api.types"
import { stationNameById } from "./stationRegistry"

export function mapApiTicket(ticket: ApiTicket): Ticket { return { id: String(ticket.id), station: stationNameById(ticket.stationId), status: ticket.status as TicketStatus, acceptedAt: ticket.acceptedAt ? Date.parse(ticket.acceptedAt) : undefined, startedAt: ticket.startedAt ? Date.parse(ticket.startedAt) : undefined, completedAt: ticket.doneAt ? Date.parse(ticket.doneAt) : undefined, servedAt: ticket.servedAt ? Date.parse(ticket.servedAt) : undefined, acknowledgedAt: ticket.cancelledAcknowledgedAt ? Date.parse(ticket.cancelledAcknowledgedAt) : undefined, version: ticket.version } }
