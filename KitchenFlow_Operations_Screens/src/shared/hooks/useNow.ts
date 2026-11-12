import { useContext } from "react"
import { clockContext } from "./ClockProvider"
export function useNow() { return useContext(clockContext) }
