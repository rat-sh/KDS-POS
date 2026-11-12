# Frontend and API contract

The React client uses the versioned Spring Boot API at `/api/v1`. In development, Vite proxies `/api` and `/actuator` to the backend. In deployment, serve the frontend and proxy those paths to the API over HTTPS.

Every API request carries `X-Correlation-Id`. Authenticated requests use a short-lived bearer token held in memory. The API rotates an opaque, hashed refresh token in an HttpOnly, SameSite=Strict cookie. Set `REFRESH_COOKIE_SECURE=true` behind HTTPS.

## Main endpoints

| Purpose | Endpoint | Access |
| --- | --- | --- |
| Health check | `GET /actuator/health` | Public |
| Login, refresh, logout | `POST /api/v1/auth/login`, `/auth/refresh`, `/auth/logout` | Public; refresh uses cookie |
| Menu and stations | `GET /api/v1/menu`, `/stations` | Signed-in staff |
| Orders and payments | `GET/POST /api/v1/orders`, `POST /orders/{id}/payments`, `/orders/{id}/cancel` | Role checked |
| Kitchen tickets | `GET /stations/{id}/tickets`; POST `/tickets/{id}/accept`, `/start`, `/bump`, `/serve`, `/recall`, `/acknowledge-cancel` | Assigned kitchen station or manager/admin |
| Manager controls | `PATCH /orders/{id}/priority`, `/rush`; `POST /tickets/{id}/manager-action` | Manager/admin |
| Availability | `PATCH /items/{id}/availability` | Kitchen, manager, or admin |
| Outlet administration | `/admin/users`, `/admin/menu/items`, `/admin/stations`, `/admin/config` | Manager/admin; settings write is admin-only |
| Audit | `GET /api/v1/audit` | Manager/admin |
| Public board | `GET /api/v1/board`, `GET /api/v1/stream/board`, `GET /api/v1/config/public` | Public; token-only board data |

Mutating order creation accepts an `Idempotency-Key`. Ticket and order actions accept the version read by the client and return `409 VERSION_CONFLICT` when another device has already changed the record. Errors use a stable `{ code, message }` body and include the correlation ID when available.

## Chef ticket lifecycle

Each menu item is assigned to one or more active kitchen stations. A paid order creates a durable `NEW` ticket per station. Chef actions persist `ACCEPTED` with `acceptedAt`/`acceptedBy`, `IN_PROGRESS` with `startedAt`, `DONE` with `doneAt`, and `SERVED` with `servedAt`/`servedBy`. Recall changes a ready or served ticket back to cooking. Manager floor overrides require a reason and are audited. The ready shelf and served history read these persisted states, so a reload or another kitchen device sees the same ticket lifecycle.

The kitchen refreshes order snapshots every five seconds. The public token board receives server-sent updates and polls as a fallback. Ticket transitions are committed transactionally with their audit row.

## Data setup

Flyway creates the database tables and the four station records plus Food and Beverage tax categories. No sample menu items, users, or orders are inserted. Set the first administrator in the backend `.env`; after login, enter the outlet's actual catalog and create staff users in Admin.

The backend validates prices and tax totals before recording payment. The current payment endpoint records a staff-entered Cash, UPI, or Card payment; it does not call or verify an external payment processor. Add a payment-provider integration before treating electronic payments as settled.
