import { describe, expect, it } from "vitest"
import { recallTicket } from "./recallTicket"
const order = { id: "1", token: 1, type: "DINE_IN" as const, status: "READY" as const, createdAt: 0, readyAt: 100, rush: false, lines: [], tickets: [{ id: "t", station: "Fry" as const, status: "DONE" as const, completedAt: 100 }] }
describe("recallTicket", () => { it("recalls inside the window", () => { expect(recallTicket(order, "Fry", 200, 600).status).toBe("IN_KITCHEN") }); it("refuses after the window", () => { expect(recallTicket(order, "Fry", 700001, 600).status).toBe("READY") }) })

