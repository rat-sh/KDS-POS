import { describe, expect, it } from "vitest"
import type { User } from "../core.types"
import { canDo, canVisit, homeFor, navFor, supportsStationBinding } from "."

describe("role permissions", () => {
  it("keeps the required navigation lists and landing routes", () => {
    expect(navFor("Cashier")).toEqual(["Counter", "Orders"])
    expect(navFor("Kitchen")).toEqual(["Station"])
    expect(navFor("Manager")).toEqual(["Floor", "Orders", "Availability", "Reports", "Audit"])
    expect(navFor("Admin")).toEqual(["Menu", "Categories", "Users", "Stations", "Settings", "Reports", "Audit"])
    expect(["Cashier", "Kitchen", "Manager", "Admin"].map((role) => homeFor(role as User["role"]))).toEqual(["Counter", "Station", "Floor", "Menu"])
  })
  it("separates payment, kitchen, manager and admin capabilities", () => {
    expect(canDo("Cashier", "PAY")).toBe(true)
    expect(canDo("Cashier", "CANCEL_UNPAID")).toBe(true)
    expect(canDo("Cashier", "CANCEL_PAID")).toBe(false)
    expect(canDo("Cashier", "SET_PRIORITY_AT_ENTRY")).toBe(true)
    expect(canDo("Kitchen", "START_TICKET")).toBe(true)
    expect(canDo("Kitchen", "PAY")).toBe(false)
    expect(canDo("Manager", "CANCEL_PAID")).toBe(true)
    expect(canDo("Manager", "SET_PRIORITY")).toBe(true)
    expect(canDo("Admin", "MANAGE_MENU")).toBe(true)
    expect(canDo("Admin", "PAY")).toBe(false)
  })
  it("keeps multi1 as a test fixture, not a demo account", () => {
    const multi1: User = { username: "multi1", role: "Kitchen", stations: ["Fry", "Grill"], active: true }
    expect(multi1.stations).toEqual(["Fry", "Grill"])
    expect(supportsStationBinding(multi1.role)).toBe(true)
  })
  it("guards route access centrally", () => {
    expect(canVisit("Cashier", "Payment")).toBe(true)
    expect(canVisit("Manager", "Payment")).toBe(false)
    expect(canVisit("Manager", "Stations")).toBe(false)
    expect(canVisit("Admin", "Handover")).toBe(false)
  })
})
