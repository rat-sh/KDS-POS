import { describe, expect, it } from "vitest"
import type { Order } from "../core.types"
import { completeOrder } from "../order-state/completeOrder"
import { bumpTicket, serveTicket } from "."

const initial: Order = { id: "flow", token: 12, type: "TAKEAWAY", status: "IN_KITCHEN", createdAt: 0, businessDay: "2026-01-01", rush: false, lines: [], tickets: [{ id: "fry", station: "Fry", status: "COOKING" }, { id: "grill", station: "Grill", status: "COOKING" }] }
describe("single kitchen-driven order lifecycle", () => {
  it("is ready when each ticket is ready and stays ready until a manager completes it", () => {
    const oneReady = bumpTicket(initial, "Fry", 10)
    expect(oneReady.status).toBe("IN_KITCHEN")
    const orderReady = bumpTicket(oneReady, "Grill", 20)
    expect(orderReady.status).toBe("READY")
    const oneServed = serveTicket(orderReady, "Fry", 30)
    expect(oneServed.status).toBe("READY")
    const allServed = serveTicket(oneServed, "Grill", 40)
    expect(allServed.status).toBe("READY")
    expect(completeOrder(allServed, 50).status).toBe("COMPLETED")
  })
})
