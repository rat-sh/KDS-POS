import type { ApiAuditEntry, ApiPage } from "./api.types"
import { request } from "./request"

export async function loadAuditEntries() { const result = await request<ApiPage<ApiAuditEntry> | ApiAuditEntry[]>("/audit?size=100"); return (Array.isArray(result) ? result : result.content).map((entry) => ({ id: String(entry.id), at: Date.parse(entry.occurredAt), actor: entry.actorId ? String(entry.actorId) : entry.actorRole, action: entry.action, entity: entry.entityType, reference: String(entry.entityId) })) }
