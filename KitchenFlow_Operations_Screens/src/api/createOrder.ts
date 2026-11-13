import type { Order, OrderLine, OrderType, PaymentMethod } from "../domain/core.types"
import type { ApiOrder } from "./api.types"
import { mapApiOrder } from "./mapApiOrder"
import { request } from "./request"

export async function createOrder(
  lines: OrderLine[],
  type: OrderType,
  note: string,
  _token: number,
  table?: string,
  priorityReason?: string,
  idempotencyKey = crypto.randomUUID(),
  payment?: { method: PaymentMethod; amount: number; tendered?: number },
): Promise<Order> {
  const response = await request<ApiOrder>("/orders", {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey },
    body: JSON.stringify({
      orderType: type,
      table,
      items: lines.map((line) => ({
        itemId: Number(line.itemId),
        quantity: line.qty,
        modifierIds: line.modifierIds.map(Number),
      })),
      note,
      priority: !!priorityReason,
      priorityReason,
      payment: payment && {
        method: payment.method.toUpperCase(),
        amount: payment.amount.toFixed(2),
        tendered: payment.tendered?.toFixed(2),
      },
    }),
  })
  return mapApiOrder(response)
}
