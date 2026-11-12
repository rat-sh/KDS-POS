import { listeners } from "./orders.state"
export function subscribeOrders(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener) } }
