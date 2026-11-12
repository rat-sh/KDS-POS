import type { Capability, Role } from "../core.types"
export const capabilities: Record<Role, Capability[]> = {
  Cashier: ["PAY", "CANCEL_UNPAID", "SET_PRIORITY_AT_ENTRY"],
  Kitchen: ["START_TICKET", "BUMP_TICKET", "RECALL_TICKET"],
  Manager: ["CANCEL_PAID", "SET_PRIORITY", "START_TICKET", "BUMP_TICKET", "RECALL_TICKET"],
  Admin: ["MANAGE_USERS", "MANAGE_MENU", "MANAGE_STATIONS", "VIEW_AUDIT", "VIEW_REPORTS"],
}
export const navByRole: Record<Role, string[]> = {
  Cashier: ["Counter", "Orders"],
  Kitchen: ["Station"],
  Manager: ["Floor", "Orders", "Availability", "Reports", "Audit"],
  Admin: ["Menu", "Categories", "Users", "Stations", "Settings", "Reports", "Audit"],
}
export const homeByRole: Record<Role, string> = {
  Cashier: "Counter", Kitchen: "Station", Manager: "Floor", Admin: "Menu",
}
export const routeByRole: Record<Role, string[]> = {
  Cashier: ["Counter", "Orders", "Payment"],
  Kitchen: ["Station"],
  Manager: ["Floor", "Orders", "Availability", "Reports", "Audit"],
  Admin: ["Menu", "Categories", "Users", "Stations", "Settings", "Reports", "Audit"],
}
