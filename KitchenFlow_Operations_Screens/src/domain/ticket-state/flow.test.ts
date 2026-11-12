import { describe, expect, it } from "vitest"
import type { Order } from "../core.types"
import { bumpTicket, serveTicket } from "."

const initial: Order = { id: "flow", token: 12, type: "TAKEAWAY", status: "IN_KITCHEN", createdAt: 0, businessDay: "2026-01-01", rush: false, lines: [], tickets: [{ id: "fry", station: "Fry", status: "COOKING" }, { id: "grill", station: "Grill", status: "COOKING" }] }
describe("single kitchen-driven order lifecycle", () => {
  it("is ready only when each ticket is ready, then completes when served", () => {
    const oneReady = bumpTicket(initial, "Fry", 10)
    expect(oneReady.status).toBe("IN_KITCHEN")
    const orderReady = bumpTicket(oneReady, "Grill", 20)
    expect(orderReady.status).toBe("READY")
    const oneServed = serveTicket(orderReady, "Fry", 30)
    expect(oneServed.status).toBe("READY")
    expect(serveTicket(oneServed, "Grill", 40).status).toBe("COMPLETED")
  })
})
