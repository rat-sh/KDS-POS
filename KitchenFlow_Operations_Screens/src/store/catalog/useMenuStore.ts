import { useEffect, useState } from "react"
import { refreshMenu } from "./refreshMenu"
import { menuState } from "./menu.state"
export function useMenuStore() { const [, rerender] = useState(0); useEffect(() => { const refresh = () => { void refreshMenu().finally(() => rerender((value) => value + 1)) }; refresh(); const timer = window.setInterval(refresh, 15_000); return () => window.clearInterval(timer) }, []); return { ...menuState, data: menuState.current } }
