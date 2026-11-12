export type ApiBoardToken = { token: number; orderNumber?: string; ageSeconds?: number }
export type ApiBoard = { preparing: number[]; ready: ApiBoardToken[] }
export type BoardData = { preparing: number[]; ready: number[] }
export type BoardUpdate = (value: BoardData) => void
