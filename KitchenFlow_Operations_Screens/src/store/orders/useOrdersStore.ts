import { useSyncExternalStore } from "react"
import { useEffect } from "react"
import { ordersSnapshot } from "./ordersSnapshot"
import { refreshOrders } from "./refreshOrders"
import { setOrders } from "./setOrders"
import { subscribeOrders } from "./subscribeOrders"
export function useOrdersStore() { const orders = useSyncExternalStore(subscribeOrders, ordersSnapshot, ordersSnapshot); useEffect(() => { void refreshOrders().catch(() => undefined); const timer = window.setInterval(() => void refreshOrders().catch(() => undefined), 5000); return () => window.clearInterval(timer) }, []); return [orders, setOrders] as const }
