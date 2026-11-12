import { loadMenu } from "../../api/loadMenu"
import { menuState } from "./menu.state"
export async function refreshMenu() { menuState.loading = true; try { menuState.current = await loadMenu(); menuState.error = "" } catch (error) { menuState.error = error instanceof Error ? error.message : "Menu could not be loaded." } finally { menuState.loading = false } return menuState.current }
