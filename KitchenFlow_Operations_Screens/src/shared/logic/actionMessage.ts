type ErrorLike = { code?: string; message?: string }

export function actionMessage(error: unknown) { const value = error as ErrorLike; if (value.code === "STALE_VERSION") return "Ticket changed elsewhere. Ticket reloaded."; if (value.code === "ITEM_UNAVAILABLE") return "Item unavailable. Remove it from the cart to continue."; if (value.code === "OUT_OF_STOCK") return "Stock changed. Reduce or remove the short item."; if (value.code === "AMOUNT_MISMATCH") return "Order changed. Total reloaded."; return value.message || "The action could not be completed." }
