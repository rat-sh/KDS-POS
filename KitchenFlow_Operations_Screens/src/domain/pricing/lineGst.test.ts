import { describe, expect, it } from "vitest"
import { lineGst } from "./lineGst"
describe("lineGst", () => { it("rounds half up per line", () => { expect(lineGst({ itemId: "x", qty: 1, modifierIds: [] }, { id: "x", name: "x", category: "Food", price: 101, stations: ["Fry"], prepSeconds: 1, available: true, stock: 1, stockTracking: true, modifierGroups: [] }, 0, .05)).toBe(5.05) }) })

