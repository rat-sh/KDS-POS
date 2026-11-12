import type { Dispatch, SetStateAction } from "react"
import type { Order, OrderLine, OrderType, User } from "../domain/core.types"
import { canDo, canVisit, homeFor } from "../domain/permissions"
import { PosScreen, OrdersList } from "../features/orders"
import { Payment } from "../features/payments"
import { StationBoard } from "../features/kitchen"
import { ManagerBoard } from "../features/board"
import { Availability } from "../features/inventory"
import { AdminCategories, AdminMenu, AdminStations, SettingsPage, UsersPage } from "../features/admin"
import { Reports } from "../features/reports"
import { Audit } from "../features/audit"
import { Forbidden, SimplePage } from "../features/shell"

type Props = { user: User; page: string; allOrders: Order[]; orders: Order[]; draft: OrderLine[]; setDraft: Dispatch<SetStateAction<OrderLine[]>>; orderType: OrderType; setOrderType: (value: OrderType) => void; note: string; setNote: (value: string) => void; table: string; setTable: (value: string) => void; priority: boolean; setPriority: (value: boolean) => void; priorityReason: string; setPriorityReason: (value: string) => void; onPay: () => void; onPaid: () => void; onLogout: () => void }

export function AppContent(props: Props) {
  const { user, page, allOrders, orders } = props
  if (!canVisit(user.role, page)) return <Forbidden page={page} home={homeFor(user.role)} />
  if (page === "Station") return <StationBoard user={user} orders={orders} onLogout={props.onLogout} />
  if (page === "Floor") return <ManagerBoard orders={orders} />
  if (page === "Counter") return <PosScreen {...props} />
  if (page === "Payment") return <><PosScreen {...props} onPay={() => undefined} /><Payment {...props} orders={orders} onDone={props.onPaid} /></>
  if (page === "Orders") return <OrdersList orders={orders} canCancelUnpaid={canDo(user.role, "CANCEL_UNPAID")} />
  if (page === "Availability") return <Availability />
  if (page === "Menu") return <AdminMenu />
  if (page === "Categories") return <AdminCategories />
  if (page === "Users") return <UsersPage />
  if (page === "Stations") return <AdminStations />
  if (page === "Settings") return <SettingsPage />
  if (page === "Reports") return <Reports orders={allOrders} />
  if (page === "Audit") return <Audit orders={allOrders} />
  return <SimplePage title={page} />
}
