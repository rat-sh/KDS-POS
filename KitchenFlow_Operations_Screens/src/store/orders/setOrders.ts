import type { Order } from "../../domain/core.types"
import { state, listeners } from "./orders.state"
export function setOrders(next: Order[] | ((orders: Order[]) => Order[])) { const value = typeof next === "function" ? next(state.current) : next; state.current = value; listeners.forEach((listener) => listener()) }
