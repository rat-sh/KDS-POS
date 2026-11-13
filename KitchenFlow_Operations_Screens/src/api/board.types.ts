export type ApiBoard = { preparing: number[]; ready: number[] }
export type BoardData = { preparing: number[]; ready: number[] }
export type BoardUpdate = (value: BoardData) => void
