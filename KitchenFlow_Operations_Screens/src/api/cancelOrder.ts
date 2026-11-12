import { request } from "./request"
export async function cancelOrder(orderId: string, reasonCode: string, reasonText: string, expectedVersion?: number) { const codes: Record<string, string> = { "Customer request": "CUSTOMER_CHANGED_MIND", "Duplicate order": "WRONG_ORDER", "Quality issue": "KITCHEN_ISSUE", "Wrong order": "WRONG_ORDER", "Payment issue": "PAYMENT_ISSUE" }; await request<void>(`/orders/${orderId}/cancel`, { method: "POST", body: JSON.stringify({ reasonCode: codes[reasonCode] || "OTHER", reasonText, expectedVersion }) }) }

