import type { MenuData } from "../../api/menuData.types"
export const menuState: { current: MenuData | null; loading: boolean; error: string } = { current: null, loading: true, error: "" }
