import { readOrders } from "./readOrders"
import type { Order } from "../../domain/core.types"
export const state: { current: Order[] } = { current: readOrders() }
export const listeners = new Set<() => void>()
