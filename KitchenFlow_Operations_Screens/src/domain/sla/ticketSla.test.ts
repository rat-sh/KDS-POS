import { describe, expect, it } from "vitest"
import type { MenuItem } from "../catalog.types"
import { ticketSla } from "./ticketSla"

const item: MenuItem = { id: "burger", name: "Burger", category: "Food", price: 1, stations: ["Fry"], prepSeconds: 100, available: true, stock: 1, stockTracking: true, modifierGroups: [] }
const order = { id: "o", token: 1, type: "DINE_IN" as const, status: "IN_KITCHEN" as const, createdAt: 0, paidAt: 0, rush: false, lines: [{ itemId: "burger", qty: 1, modifierIds: [] }], tickets: [{ id: "t", station: "Fry" as const, status: "NEW" as const }] }

describe("ticketSla", () => {
  it("measures elapsed time against the longest station item target", () => {
    const sla = (seconds: number) => ticketSla(order, "Fry", [item], seconds * 1000, { amberPercent: 80, redPercent: 100 })
    expect(sla(79)).toEqual({ percent: 79, colour: "GREEN", targetSeconds: 100 })
    expect(sla(80).colour).toBe("AMBER")
    expect(sla(100).colour).toBe("AMBER")
    expect(sla(101).colour).toBe("RED")
  })
})
