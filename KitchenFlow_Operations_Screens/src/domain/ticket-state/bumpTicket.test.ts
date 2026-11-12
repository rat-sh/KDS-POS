import { describe, expect, it } from "vitest"
import { bumpTicket } from "./bumpTicket"
const order = { id: "1", token: 1, type: "DINE_IN" as const, status: "IN_KITCHEN" as const, createdAt: 0, rush: false, lines: [], tickets: [{ id: "t", station: "Fry" as const, status: "IN_PROGRESS" as const }] }
describe("bumpTicket", () => { it("makes the last ticket ready", () => { expect(bumpTicket(order, "Fry", 100).status).toBe("READY") }) })

