import { describe, expect, it } from "vitest"
import { businessDayKey } from "./businessDayKey"
import { formatTicketTimer } from "./formatTicketTimer"

describe("business day and ticket timer", () => {
  it("rolls over at 04:00 local time", () => {
    expect(businessDayKey(new Date(2026, 0, 2, 3, 59).getTime())).toBe("2026-01-01")
    expect(businessDayKey(new Date(2026, 0, 2, 4, 0).getTime())).toBe("2026-01-02")
  })
  it("shows elapsed time beyond the ticket limit", () => {
    const placed = 1_000
    expect(formatTicketTimer(placed, placed + 9 * 60_000)).toBe("09:00")
    expect(formatTicketTimer(placed, placed + 70 * 60_000)).toBe("70:00")
  })
})
