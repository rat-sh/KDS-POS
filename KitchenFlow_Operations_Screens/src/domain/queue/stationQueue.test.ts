import { describe, expect, it } from "vitest"
import type { MenuItem } from "../catalog.types"
import type { Order } from "../core.types"
import { stationQueue } from "./stationQueue"

const items: MenuItem[] = [{ id: "burger", name: "Burger", category: "Food", price: 1, stations: ["Fry"], prepSeconds: 1200, available: true, stock: 1, stockTracking: true, modifierGroups: [] }]
const order = (id: string, status: Order["status"], ticketStatus: Order["tickets"][number]["status"], rush: boolean, paidAt: number): Order => ({ id, token: 1, type: "DINE_IN", status, createdAt: paidAt, paidAt, rush, lines: [{ itemId: "burger", qty: 1, modifierIds: [] }], tickets: [{ id: `${id}-ticket`, station: "Fry", status: ticketStatus }] })

describe("stationQueue", () => {
  it("orders cancelled, overdue, rush, then oldest remaining tickets", () => {
    const orders = [order("new", "IN_KITCHEN", "NEW", false, 1_380_000), order("rush", "IN_KITCHEN", "NEW", true, 1_440_000), order("overdue", "IN_KITCHEN", "IN_PROGRESS", false, 0), order("cancelled", "CANCELLED", "CANCELLED", false, 0), order("old", "IN_KITCHEN", "NEW", false, 1_140_000)]
    expect(stationQueue(orders, "Fry", items, 1_500_000).map((entry) => entry.id)).toEqual(["cancelled", "overdue", "rush", "old", "new"])
  })

  it("omits orders with no item mapped to the selected station", () => {
    const otherStation = { ...items[0], stations: ["Grill"] }
    expect(stationQueue([order("grill", "IN_KITCHEN", "NEW", false, 0)], "Fry", [otherStation], 0)).toHaveLength(0)
  })

  it("keeps ready, served, and done tickets out of the open lane", () => {
    const orders = [order("new", "IN_KITCHEN", "NEW", false, 0), order("progress", "IN_KITCHEN", "IN_PROGRESS", false, 0), order("ready", "READY", "READY", false, 0), order("served", "COMPLETED", "SERVED", false, 0), order("done", "COMPLETED", "DONE", false, 0)]
    expect(stationQueue(orders, "Fry", items, 0).map((entry) => entry.id)).toEqual(["new", "progress"])
  })

  it("retains a shared item on each station that handles it", () => {
    const shared = { ...items[0], stations: ["Fry", "Grill"] }
    const sharedOrder = { ...order("shared", "IN_KITCHEN", "NEW", false, 0), tickets: [{ id: "f", station: "Fry", status: "NEW" as const }, { id: "g", station: "Grill", status: "NEW" as const }] }
    expect(stationQueue([sharedOrder], "Fry", [shared], 0)).toHaveLength(1)
    expect(stationQueue([sharedOrder], "Grill", [shared], 0)).toHaveLength(1)
  })
})
