# KitchenFlow KDS

**KitchenFlow** is a Kitchen Display System (KDS) for Quick Service Restaurants (QSRs) — built from scratch as a full-stack application that replaces paper dockets, verbal ticket handoffs, and passive display boards with a live, role-driven operations platform.

---

## The Real-World Problem This Solves

A QSR kitchen runs on speed and coordination. Every second between an order being placed and food leaving the pass costs the restaurant money and customer satisfaction. The traditional approach — paper dockets, shouted orders, whiteboards — creates compounding failures:

| Pain Point | Real Impact |
|---|---|
| Paper dockets lost or illegible | Order remade or delayed; customer complaint |
| No visibility across stations | Fry finishes before Grill; food sits and cools |
| No SLA enforcement | Rush orders treated the same as regular ones |
| Staff can't see what's overdue | Manager only knows when customer complains |
| Cancellations not broadcast | Kitchen cooks food for a cancelled order |
| No shift data | Management has no throughput or bottleneck data |
| New menu items require reprinting | Manual process, prone to error |

**KitchenFlow solves every one of these** with a live, multi-station digital display, role-based access control, automated ticket routing, SLA timers, real-time push, and a full admin console — all configured by the restaurant to match their own kitchen model, not ours.

---

## What The System Can Do

### For the Kitchen (Station Staff)
- Each station (e.g., Wok, Tandoor, Cold, Grill) sees only the tickets it needs to cook
- Tickets auto-route to the correct station based on menu item assignments
- Live **SLA traffic-light timer** turns amber at a configurable threshold, red at another — staff can see at a glance what's late before the customer knows
- **Accept → Start → Mark Ready** workflow enforces discipline; timestamps every step
- **Recall** brings a bumped ticket back within a window (accidental bump recovery)
- **Priority** and **Rush** orders float to the top of the queue automatically
- VIP and cancelled tickets have distinct visual treatment
- Station Inventory sidebar shows today's item throughput by station
- Offline banner appears when connectivity drops — board shows last known state; Done is disabled until reconnection

### For the Counter (Cashier / POS)
- Full menu browsing — categories and search derived from your live menu, not hardcoded
- Cart with quantity steppers, real-time per-line and grand-total calculation
- **Dine-in / Takeaway / Delivery** order types with table number for dine-in
- Stock checks prevent selling out-of-stock items
- Simulated payment recording (Cash, UPI, Card) with change-due calculation
- Idempotency-keyed order creation — double-tap on PAY never creates a duplicate
- Printed bill with outlet name, GSTIN, and per-line gross amounts

### For the Floor Manager
- **Needs Attention** dashboard — surfaces late, rush, or long-waiting-ready orders instantly
- All open orders table with status, type, stations, and time
- Drawer per order: step into any station ticket (START / MARK READY / SERVE) with an audit reason
- Cancel orders with reason from the floor
- Set or clear Priority with reason — appears on kitchen board immediately
- Toggle Rush flag per order

### For the Admin (Org Setup)
- **Categories** — create your own (e.g., "Starters", "Mains", "Desserts", "Drinks") with GST rate per category; nothing pre-defined
- **Stations** — create your kitchen stations (e.g., "Wok", "Cold", "Grill", "Expo"); nothing pre-defined
- **Menu** — add items with name, category, prep time target, price, stock level, and station assignments; full modifier group support (single-select / multi-select option groups)
- **Users** — create staff accounts with roles (Cashier, Kitchen, Manager, Admin) and station bindings for Kitchen staff
- **Settings** — set your outlet name, GSTIN, and SLA amber/red percentage thresholds
- **Reports** — daily order volume, ticket timing, throughput by station with configurable date range
- **Audit Log** — every write operation is logged with actor, role, entity, and reason; append-only, cannot be tampered with

### For the Kitchen Screen (Public Board)
- Public SSE stream contains token numbers only: preparing and ready tokens
- The public board reconnects with backoff after stream errors; JSON polling continues every five seconds
- Kitchen and cashier screens use five-second polling and do not receive the public SSE stream
- Useful for a secondary display at the pass or expediter station

---

## Architecture

```
Browser ──► Nginx Gateway (:8080) ──► Spring Boot API (:8081) ──► PostgreSQL (:5432)
                │                              │
         Rate limiting                   Flyway migrations
         SPA fallback                    HMAC-SHA256 JWT
         SSE proxy                       Caffeine rate limiter
         Actuator shielded               Append-only audit log
```

- **Frontend**: React 19, TypeScript, Vite, Bun — single-page app, zero mock data
- **Backend**: Spring Boot 3.5.16, Java 21, plain JDBC (no ORM), stateless JWT
- **Gateway**: Nginx — login rate limit (1 req/min), API rate limit (20 req/s burst), SSE buffering off, SPA hash-routing fallback
- **Database**: PostgreSQL 16, Flyway schema versioning
- **Deployment**: single `docker compose up --build` command; three-container chain

---

## Quick Start

### Prerequisites

| Tool | Version |
|---|---|
| Docker Desktop | 24+ |
| Java JDK | 21+ (for local dev only) |
| Bun | 1.4+ (for local dev only) |

### Production (Docker Compose)

```bash
# 1. Copy and configure secrets
cp .env.example .env
# Edit .env — set DB_PASSWORD, JWT_SECRET (32+ chars), ADMIN_PASSWORD (12+ chars)

# 2. Build and start all three services
docker compose up --build -d

# 3. Open the app
open http://localhost:8080

# 4. Sign in with the bootstrap admin credentials from your .env
```

The first time the database is empty, the application creates a single admin account from `ADMIN_USERNAME` and `ADMIN_PASSWORD` in `.env`. All stations, categories, menu items, and staff are created by that admin through the UI — nothing is pre-loaded.

### First-time Setup Checklist (after login as Admin)

1. **Admin → Categories** — Add your menu categories with GST rates (e.g., "Starters 5%", "Beverages 18%")
2. **Admin → Stations** — Add your kitchen stations (e.g., "Grill", "Cold Kitchen", "Expo")
3. **Admin → Menu** — Add items, assign each to a category and one or more stations
4. **Admin → Users** — Create cashier, kitchen, and manager accounts; bind kitchen staff to their stations
5. **Admin → Settings** — Set outlet name and GSTIN for bills; tune SLA thresholds if needed

### Local Development

```bash
# Start only the database
docker compose up -d database

# Backend (from project root)
./mvnw spring-boot:run

# Frontend (in a second terminal)
cd KitchenFlow_Operations_Screens
bun install
bun run dev
# → http://localhost:5173  (Vite proxies /api and /actuator to :8080)
```

---

## Running Checks

### Backend

```powershell
# From project root
.\mvnw.cmd -DskipTests compile      # Compile only
.\mvnw.cmd test                     # Integration tests (requires Docker for Testcontainers)
.\mvnw.cmd package                  # Build JAR
docker compose config               # Validate Compose file
```

### Frontend

```bash
cd KitchenFlow_Operations_Screens
bun run typecheck    # TypeScript strict type check (0 errors)
bun run lint         # ESLint
bun run test         # Vitest unit tests (17 tests, 9 files)
bun run build        # Vite production build
bun run test:e2e     # Playwright end-to-end (requires running stack)
```

---

## Project Structure

```
kds/
├── src/                                    # Spring Boot backend
│   └── main/
│       ├── java/com/kitchenflow/kds/
│       │   ├── api/
│       │   │   └── KitchenFlowController.java      # All HTTP endpoints (~530 lines)
│       │   ├── auth/
│       │   │   ├── service/AuthService.java         # Login, refresh, logout, JWT
│       │   │   ├── service/AccessTokenService.java  # HMAC-SHA256 JWT sign/verify
│       │   │   └── service/AuthBootstrap.java       # First-run admin seed
│       │   ├── board/
│       │   │   └── BoardStream.java                 # SSE push (3s scheduler)
│       │   ├── common/
│       │   │   ├── config/KitchenFlowProperties.java
│       │   │   ├── error/ApiFailure.java            # Structured error type
│       │   │   ├── security/                        # JWT filter, actor model
│       │   │   └── time/BusinessDayService.java
│       │   ├── config/api/ConfigRequests.java       # Settings + station PATCH DTOs
│       │   ├── edge/
│       │   │   ├── error/ApiExceptionHandler.java
│       │   │   └── security/
│       │   │       ├── AccessTokenFilter.java       # JWT extraction + validation
│       │   │       ├── ApplicationRateLimitFilter.java  # Caffeine sliding-window
│       │   │       └── SecurityConfiguration.java   # Stateless Spring Security
│       │   ├── inventory/api/StockAdjustmentRequest.java
│       │   ├── menu/
│       │   │   ├── api/MenuItemRequest.java
│       │   │   ├── api/MenuAdminRequests.java       # Category, modifier DTOs
│       │   │   └── service/CatalogService.java      # All menu, station, user logic
│       │   ├── order/
│       │   │   ├── api/OrderRequests.java           # Order + ticket action DTOs
│       │   │   └── service/OrderService.java        # Order lifecycle, ticket FSM
│       │   └── user/api/UserRequest.java
│       └── resources/
│           ├── application.properties               # All config (env-driven)
│           └── db/migration/
│               ├── V1__kitchenflow_core.sql         # All tables + indexes
│               ├── V2__manager_priority_and_outlet.sql
│               ├── V3__ticket_records_and_idempotency.sql
│               ├── V4__refunds_and_stock_adjustments.sql
│               └── V5__remove_seeded_data.sql       # Clean slate — no pre-seeded stations/categories
│
├── KitchenFlow_Operations_Screens/                 # React/TypeScript frontend
│   └── src/
│       ├── api/                                    # All backend API calls (no mock data)
│       │   ├── index.ts                            # Barrel export
│       │   ├── request.ts                          # Shared fetch wrapper + JWT attach
│       │   ├── stationRegistry.ts                  # Boot-time station name↔id registry
│       │   ├── mapApiOrder.ts / mapApiTicket.ts / mapApiMenu.ts / mapApiUser.ts
│       │   ├── loadStationConfigs.ts / loadMenu.ts / loadOrders.ts
│       │   ├── manageCategories.ts                 # Category CRUD (create, patch, list)
│       │   ├── catalogTotals.ts                    # Order total + dynamic GST breakdown
│       │   └── ...all other API calls
│       ├── app/
│       │   ├── BootGate.tsx                        # Health check → station registry → render
│       │   ├── RoleApp.tsx                         # Auth state, page routing
│       │   └── AppContent.tsx                      # Page-to-component dispatch
│       ├── domain/                                 # Pure business logic (no I/O)
│       │   ├── core.types.ts                       # Order, Ticket, User, Station types
│       │   ├── catalog.types.ts                    # MenuItem, ModifierGroup types
│       │   ├── permissions/                        # Role → nav/routes/capabilities
│       │   ├── queue/stationQueue.ts               # Ticket filtering + sort for a station
│       │   ├── ticket-state/                       # Ticket FSM (bump, recall, serve)
│       │   ├── sla/ticketSla.ts                    # SLA colour from elapsed time
│       │   ├── pricing/                            # GST calculation, order totals
│       │   └── time/                               # Business day key
│       ├── features/
│       │   ├── admin/                              # Admin console (Menu, Categories, Stations, Users, Settings)
│       │   ├── board/ManagerBoard.tsx              # Floor manager view
│       │   ├── kitchen/StationBoard.tsx            # Kitchen station display
│       │   ├── orders/                             # POS counter + order list
│       │   ├── payments/                           # Payment collection + bill
│       │   ├── inventory/Availability.tsx          # Stock availability toggle
│       │   ├── reports/Reports.tsx                 # Daily throughput report
│       │   ├── audit/Audit.tsx                     # Audit log viewer
│       │   └── shell/                             # Nav shell, login, error pages
│       ├── store/                                  # Zustand stores (orders, catalog, settings)
│       └── shared/                                 # Hooks, UI atoms, shared logic
│
├── docs/
│   ├── BASELINE.md                                 # Verification results log
│   ├── DEVIATIONS.md                               # Design decisions and scope gaps
│   ├── KF-REV-002.md                               # Revision 0.2 scope document
│   ├── TRACEABILITY.md                             # Requirement ↔ code traceability matrix
│   └── text/                                       # Extracted specification text (7 documents)
│       ├── 1_Product Development Document.txt
│       ├── 2_Software Requirements Specification.txt
│       ├── 3_System Architecture Document.txt
│       ├── 4_Database Design Document.txt
│       ├── 5_API Specification.txt
│       ├── 6_Low-Level Design.txt
│       └── 7_Test Plan and Test Cases.txt
│
├── compose.yaml                                    # Three-service Docker Compose stack
├── Dockerfile                                      # Spring Boot app image (layered JAR)
├── Dockerfile.gateway                              # Nginx gateway image
├── nginx.conf                                      # Rate limits, SSE proxy, SPA fallback
├── .env.example                                    # Required environment variable template
└── pom.xml                                         # Spring Boot 3.5.16, Java 21, JDBC, Flyway
```

---

## API Surface (Key Endpoints)

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Public | Username + password → access + refresh tokens |
| POST | `/api/v1/auth/refresh` | Cookie | Rotate refresh token → new access token |
| POST | `/api/v1/auth/logout` | Any | Revoke refresh token |
| GET | `/api/v1/menu` | Public | Full menu (categories, items, modifiers, GST rates) |
| GET | `/api/v1/stations` | Public | All active stations |
| GET | `/api/v1/orders` | Cashier/Manager | Paginated order list |
| POST | `/api/v1/orders` | Cashier | Create order (idempotency-keyed) |
| POST | `/api/v1/orders/{id}/payments` | Cashier | Record payment |
| POST | `/api/v1/orders/{id}/cancel` | Cashier/Manager | Cancel with reason |
| POST | `/api/v1/tickets/{id}/accept` | Kitchen | Accept ticket |
| POST | `/api/v1/tickets/{id}/start` | Kitchen | Start cooking |
| POST | `/api/v1/tickets/{id}/bump` | Kitchen | Mark ready |
| POST | `/api/v1/tickets/{id}/serve` | Kitchen | Mark served |
| POST | `/api/v1/tickets/{id}/recall` | Kitchen | Recall bumped ticket |
| GET | `/api/v1/stations/{id}/records` | Kitchen/Manager | Timing records (JSON or CSV) |
| GET | `/api/v1/board` | Public | Current preparing and ready tokens (JSON) |
| GET | `/api/v1/stream/board` | Public (SSE) | Preparing and ready tokens (SSE stream) |
| GET | `/api/v1/admin/categories` | Admin | List menu categories |
| POST | `/api/v1/admin/categories` | Admin | Create category |
| PATCH | `/api/v1/admin/categories/{id}` | Admin | Update name/GST/active |
| POST | `/api/v1/admin/items` | Admin | Create menu item |
| PATCH | `/api/v1/admin/items/{id}` | Admin | Update menu item |
| POST | `/api/v1/admin/stations` | Admin | Create station |
| PATCH | `/api/v1/admin/stations/{id}` | Admin | Rename / reorder / deactivate |
| POST | `/api/v1/admin/users` | Admin | Create staff account |
| PATCH | `/api/v1/admin/users/{id}` | Admin | Update role / password / active |
| GET | `/api/v1/admin/audit` | Admin | Last N audit entries |
| GET | `/actuator/health` | Internal | Health check (Nginx uses this) |

---

## Security Model

- **JWT (HMAC-SHA256)**: stateless access tokens; refresh tokens stored hashed in DB and revoked on logout or role change
- **Role-based access**: every endpoint is guarded by `@PreAuthorize`; frontend enforces the same role-to-route table in `permissions.constants.ts`
- **Rate limiting**: login endpoint: 1 req/min per IP (Nginx); API: 20 req/s burst (Nginx + Caffeine in-process)
- **Audit log**: every write (order, ticket, user, menu, stock) is logged with actor ID, role, entity, action, and reason; append-only trigger prevents deletion or update
- **No secrets in code**: all passwords, JWT secret, and DB credentials are environment variables

---

## Data Model Highlights

```
outlet_config (1 row)        ← outlet name, GSTIN, SLA thresholds
menu_category                ← org-defined (e.g., Starters, Mains)
station                      ← org-defined (e.g., Wok, Grill, Expo)
menu_item → menu_category    ← items belong to categories
menu_item_station            ← many-to-many: items routed to stations
app_user → user_station      ← kitchen staff bound to stations
order_header                 ← one order per business-day token
order_line → menu_item       ← lines snapshot item name + price
kitchen_ticket → station     ← one ticket per station per order
  ↳ accepted_at, started_at, done_at, served_at, recall_count
payment → order_header       ← simulated Cash/UPI/Card record
audit_entry                  ← append-only write log
```

---

## Scaling Considerations

This system is intentionally **monolithic** (D-002 decision) — no Kafka, Redis, or microservices. It is designed for a single-outlet QSR deployment. Horizontal scaling paths if needed:

- The stateless JWT design means any number of API replicas can run behind a load balancer with no session affinity
- Nginx is already the entry point; adding upstream replicas is a one-line change
- SSE board connections are long-lived but cheap; a single API instance handles hundreds concurrently
- PostgreSQL connection pool (HikariCP defaults) handles peak kitchen load comfortably at single-outlet scale
- For multi-outlet: the schema is single-tenant; run a separate stack per outlet

---

## Design Decisions

See [`docs/DEVIATIONS.md`](docs/DEVIATIONS.md) for the full list. Key ones:

| ID | Decision |
|---|---|
| D-001 | Payments are simulated records only — no payment provider |
| D-002 | Monolithic deployment — no Kafka, Redis, Spring Cloud Gateway |
| D-007 | Spring Boot 3.5.16 (spec named 3.x; initial repo used 4.1.2 — corrected) |
| D-009 | Plain JDBC — no JPA/Hibernate; all queries explicit SQL |

---

## Contributing

1. Schema changes → new Flyway migration file (`V6__description.sql`) — never edit existing migrations
2. New API endpoints → add to `KitchenFlowController.java`, add service method to `CatalogService` or `OrderService`, add corresponding frontend API file in `src/api/`, export from `src/api/index.ts`
3. New admin page → add component in `src/features/admin/components/`, export from `src/features/admin/index.ts`, add route to `permissions.constants.ts` (both `navByRole` and `routeByRole`), add `if (page === "...")` to `AppContent.tsx`
4. Run all checks before committing — `bun run typecheck && bun run lint && bun run test` and `.\mvnw.cmd -DskipTests compile`
