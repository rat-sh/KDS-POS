import type { PaymentMethod } from "../domain/core.types"
import { request } from "./request"
export async function recordPayment(orderId: string, method: PaymentMethod, amount: number, tendered?: number) { await request<void>(`/orders/${orderId}/payments`, { method: "POST", body: JSON.stringify({ method: method.toUpperCase(), amount: amount.toFixed(2), tendered: tendered?.toFixed(2) }) }) }

