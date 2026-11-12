# Traceability register

Status values are Implemented, Partial, or Not done. Implemented requires both the specified behavior and a test; Partial means code exists but the contract or test evidence is incomplete. The rows below record the current evidence and remaining coverage gaps.

## API endpoints

| ID | Required behavior | Status | Code location | Test IDs |
| --- | --- | --- | --- | --- |
| A-01 | `POST /api/v1/auth/login` | Partial | KitchenFlowController.login / AuthService.login | No endpoint test |
| A-02 | `POST /api/v1/auth/refresh` | Partial | KitchenFlowController.refresh / AuthService.refresh | No endpoint test |
| A-03 | `POST /api/v1/auth/logout` | Partial | KitchenFlowController.logout / AuthService.logout | No endpoint test |
| A-04 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| A-05 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| U-01 | `GET /api/v1/admin/users` | Partial | KitchenFlowController.users / CatalogService.users | No endpoint test |
| U-02 | `POST /api/v1/admin/users` | Partial | KitchenFlowController.createUser / CatalogService.saveUser | No endpoint test |
| U-03 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| U-04 | `PATCH /api/v1/admin/users/{id}` (current code addresses username) | Partial | KitchenFlowController.updateUser / CatalogService.saveUser | Contract path mismatch; no endpoint test |
| U-05 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| S-01 | `GET /api/v1/stations` | Partial | KitchenFlowController.stations / CatalogService.stations | No endpoint test |
| S-02 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| S-03 | `PATCH /api/v1/admin/stations/{id}` | Partial | KitchenFlowController.updateStation / CatalogService.updateStation | No endpoint test |
| C-01 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| C-02 | `PATCH /api/v1/admin/config` | Partial | KitchenFlowController.updateConfig / CatalogService.updateSettings | No endpoint test |
| M-01 | `GET /api/v1/menu` | Partial | KitchenFlowController.menu / CatalogService.menu | No endpoint test |
| M-02 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-03 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-04 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-05 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-06 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-07 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-08 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-09 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-10 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-11 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-12 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-13 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-14 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-15 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-16 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-17 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-18 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-19 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-20 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| M-21 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| I-01 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| I-02 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| I-03 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| O-01 | `POST /api/v1/orders` with user-scoped idempotency and token allocation | Partial | KitchenFlowController.createOrder / OrderService.create | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| O-02 | `GET /api/v1/orders` | Partial | KitchenFlowController.orders / OrderService.orders | No endpoint test; filters/cursor incomplete |
| O-03 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| O-04 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| O-05 | `POST /api/v1/orders/{id}/cancel` | Partial | KitchenFlowController.cancel / OrderService.cancel | No endpoint test |
| O-06 | `POST /api/v1/orders/{id}/complete` | Partial | KitchenFlowController.complete / OrderService.complete | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| O-07 | `PATCH /api/v1/orders/{id}/rush` (current path is available) | Partial | KitchenFlowController.rush / OrderService.setRush | No endpoint test |
| O-08 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| P-01 | `POST /api/v1/orders/{id}/payments` (simulated records only) | Partial | KitchenFlowController.payment / OrderService.recordPayment | No endpoint test |
| P-02 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| P-03 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| P-04 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| P-05 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| P-06 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| K-01 | `GET /api/v1/stations/{id}/tickets` | Partial | KitchenFlowController.tickets / OrderService.stationTickets | No endpoint test |
| K-02 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| K-03 | `POST /api/v1/tickets/{id}/start` | Partial | KitchenFlowController.start / OrderService.ticketAction | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| K-04 | `POST /api/v1/tickets/{id}/bump` | Partial | KitchenFlowController.bump / OrderService.ticketAction | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| K-05 | `POST /api/v1/tickets/{id}/recall` | Partial | KitchenFlowController.recall / OrderService.ticketAction | TicketRecordIntegrationTests.recallingReadyTicketIncrementsRecallCountAndReopensOrder; recall window still absent |
| K-06 | `POST /api/v1/tickets/{id}/acknowledge-cancel` | Partial | KitchenFlowController.acknowledgeCancel / OrderService.ticketAction | No action test |
| K-07 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| K-08 | `POST /api/v1/tickets/{id}/accept` (path inferred; official revision endpoint table unavailable) | Partial | KitchenFlowController.accept / OrderService.ticketAction | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| K-09 | `POST /api/v1/tickets/{id}/serve` (path inferred; official revision endpoint table unavailable) | Partial | KitchenFlowController.serve / OrderService.ticketAction | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| K-10 | `GET /api/v1/stations/{stationId}/records` with `state`, `token`, and `format=csv` | Partial | KitchenFlowController.ticketRecords / OrderService.stationTicketRecords | TicketRecordIntegrationTests.stationRecordsFilterAndReturnAcceptedAndServedActorsAndDurations; CSV endpoint not yet exercised |
| B-01 | `GET /api/v1/board` | Partial | KitchenFlowController.board / OrderService.publicBoard | No endpoint test |
| X-01 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| X-02 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| X-03 | `GET /api/v1/stream/board` | Partial | KitchenFlowController.boardStream / BoardStream | No stream test; other stream endpoints absent |
| R-01 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| R-02 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| R-03 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| R-04 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| R-05 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |
| AU-01 | `GET /api/v1/audit` | Partial | KitchenFlowController.audit / CatalogService.audit | No endpoint test |
| H-01 | `/actuator/health` only; liveness/readiness are not exposed through the gateway | Partial | SecurityConfiguration / Actuator configuration | No health endpoint test |
| H-02 | Actuator Prometheus endpoint is configured and admin protected in the API | Partial | SecurityConfiguration / application.properties | No authorization test; gateway blocks external access |
| H-03 | See KF-API-001 endpoint index; revision 0.2 adds lifecycle/record endpoints | Not done | TBD | TBD |

## Functional requirements, business rules, and quality requirements

| ID | Requirement | Status | Code location | Test IDs |
| --- | --- | --- | --- | --- |
| BR-01 | Token numbers are unique per business day, start at 1, increment by 1 per order, restart at the cutoff (BR-16). | Partial | OrderService.create / token_counter upsert | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| BR-02 | All money uses decimal arithmetic in INR with two decimals; order lines snapshot unit price and GST rate so later menu changes do not alter existing orders. | Partial | OrderService.orderTotal / Money type | TicketRecordIntegrationTests (pricing assertions) |
| BR-03 | GST per line = (unit price + modifier prices) × quantity × category rate, rounded half-up to two decimals; order total = sum of line amounts + line GST; menu prices are GST-exclusive. | Partial | OrderService.bill / orderTotal | TicketRecordIntegrationTests (bill assertions) |
| BR-04 | An unavailable or inactive item cannot be added to a new order; orders already created are unaffected. | Partial | OrderService.create (available check) | No dedicated test |
| BR-05 | A CREATED order may be cancelled by Cashier or Manager; IN_KITCHEN only by Manager; READY and COMPLETED cannot be cancelled; a reason is always required. | Partial | OrderService.cancel (role + status checks) | No test |
| BR-06 | An order is READY exactly when all its non-cancelled tickets are DONE; recalling a ticket of a READY order returns it to IN_KITCHEN. | Partial | OrderService.updateOrderReady / ticketAction recall | TicketRecordIntegrationTests.recallingReadyTicketIncrementsRecallCountAndReopensOrder |
| BR-07 | Payment is taken before the kitchen sees the order (pay-first); partial and split payments are not supported in v1. | Partial | OrderService.recordPayment (status gate) | No explicit test |
| BR-08 | The payment amount must equal the order total. | Partial | OrderService.recordPayment (AMOUNT_MISMATCH check) | No explicit test |
| BR-09 | Every item maps to at least one station; a multi-station item appears on each station's ticket; a combo expands into component stations. | Partial | OrderService.create (station routing) / CatalogService.saveMenuItem (station validation) | No explicit test |
| BR-10 | Station queues are ordered by RUSH first, then by payment time, oldest first. | Not done | OrderService.stationTickets (ordering incomplete) | TBD |
| BR-11 | Ticket colour: green < 80 %, amber 80–100 %, red > 100 % of target time; a ticket's target is the longest among its items; thresholds are configurable. | Not done | TBD | TBD |
| BR-12 | Stock reserved on order creation; restored on CREATED-state cancellation; IN_KITCHEN cancellation does not restore stock; item becomes unavailable at zero. | Partial | OrderService.create (stock deduction) / cancel (restoreReservedStock) | No explicit test |
| BR-13 | Every state change, payment, price change, availability change, and user/role change creates an audit record in the same transaction. | Partial | CatalogService.audit / OrderService.audit (called in every mutation) | TicketRecordIntegrationTests (audit assertions) |
| BR-14 | An order can be edited only while CREATED. | Partial | OrderService (edit not fully implemented) | No test |
| BR-15 | A repeated Idempotency-Key within 24 hours with the same payload returns the original response; a different payload returns 422 IDEMPOTENCY_KEY_REUSED. | Partial | OrderService.create (idempotency check + advisory lock) | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| BR-16 | The business day runs from the configured cutoff (default 04:00 Asia/Kolkata) to the next cutoff; tokens and reports use the business day. | Partial | BusinessDayService / KitchenFlowProperties | No test |
| BR-17 | A DONE ticket can be recalled only within the configured recall window (default 10 min) and only while its order is not COMPLETED. | Not done | OrderService.ticketAction recall (no window check implemented) | TBD |
| BR-18 | Timestamps are stored in UTC and displayed in Asia/Kolkata. | Partial | ClockConfiguration / DB session UTC | No explicit test |
| FR-AUD-01 | Record actor, role, time, entity, action, prev/new state, and reason for every state change, payment, price/availability/stock/user change. | Partial | CatalogService.audit / OrderService.audit (called in all mutations) | TicketRecordIntegrationTests (audit assertions) |
| FR-AUD-02 | Audit records are append-only; the API offers no update or delete; the app DB user has no UPDATE/DELETE on the audit table. | Partial | DB migration (append-only trigger expected); SecurityConfiguration (no audit write endpoint) | No explicit test |
| FR-AUD-03 | Manager and Admin shall search audit log by entity, actor, action, and date range with pagination; p95 < 300 ms with 500 k rows. | Partial | CatalogService.audit (basic list, no filters yet) | No test |
| FR-AUD-04 | The audit record must be written in the same transaction as the change it describes; a forced audit failure rolls back the business change. | Partial | CatalogService.audit / OrderService.audit (same @Transactional) | No explicit rollback test |
| FR-AUTH-01 | Authenticate users by username + password; issue a signed access token valid for 15 minutes; wrong credentials return 401 with the same message. | Partial | AuthService.login / AccessTokenService.issue | No endpoint test |
| FR-AUTH-02 | Store passwords only as BCrypt hashes; never log or return them. | Partial | SecurityConfiguration (BCryptPasswordEncoder cost 12) / CatalogService.saveUser | No test verifying log/response absence |
| FR-AUTH-03 | Enforce role-based authorization on every endpoint except login, health, and the public board; unauthenticated → 401, unauthorized → 403. | Partial | SecurityConfiguration / AccessTokenFilter / @PreAuthorize annotations | No role-matrix test |
| FR-AUTH-04 | Bind each Kitchen user to one or more stations; allow that user to view and act only on those stations' tickets. | Partial | OrderService.stationTickets / ticketAction (station binding check) | No explicit station-binding test |
| FR-AUTH-05 | Admin shall create, deactivate, and reactivate users and assign one role per user; a deactivated user cannot log in. | Partial | CatalogService.saveUser / AuthService.login (active check) | No test |
| FR-AUTH-06 | Lock an account for 15 minutes after 5 consecutive failed logins within 10 minutes; the sixth attempt returns 429 even with the correct password. | Not done | AuthService.login (no lockout logic found) | TBD |
| FR-AUTH-07 | Refresh tokens rotate on every use; reusing a consumed refresh token returns 401. | Partial | AuthService.refresh (rotateRefresh + lockValidRefresh) | No test |
| FR-BRD-01 | Provide a public, read-only status board listing token numbers in PREPARING (IN_KITCHEN) and READY groups; no login required; no personal data exposed. | Partial | OrderService.publicBoard / SecurityConfiguration (permitAll for /api/v1/board) | No test |
| FR-BRD-02 | The board shall update within 2 seconds of a status change using SSE with polling as fallback. | Partial | BoardStream (3-second scheduled push; no event-driven push on state change) | No stream test |
| FR-BRD-03 | Remove a token from the board when its order becomes COMPLETED or CANCELLED. | Partial | OrderService.publicBoard (filters by IN_KITCHEN / READY) | No test |
| FR-BRD-04 | Highlight tokens that have been READY longer than a configured time (default 5 min). | Not done | TBD | TBD |
| FR-CFG-01 | Admin shall configure stations, GST rates, colour thresholds, recall window, business-day cutoff, and unpaid-order timeout; changes take effect without a restart. | Partial | CatalogService.updateSettings / updateStation | No test |
| FR-CFG-02 | Load seed data with one command: ≥5 categories, 30 items with modifiers, 4 stations, one user per role; completes in < 60 s. | Not done | TBD | TBD |
| FR-CFG-03 | Provide a repeatable script generating 100,000 synthetic historical orders for performance testing. | Not done | TBD | TBD |
| FR-INV-01 | Admin should enable stock tracking per item and set an integer quantity ≥ 0; untracked items are never blocked by stock. | Partial | CatalogService.saveMenuItem (track_stock / stock_quantity columns) | No explicit test |
| FR-INV-02 | Reserve stock atomically at order creation; if any item lacks stock, fail with 409 OUT_OF_STOCK listing the items. | Partial | OrderService.create (conditional stock decrement) | No concurrency test |
| FR-INV-03 | Mark an item unavailable when stock reaches zero; mark it available again when stock is added. | Partial | OrderService.create (available = stock > 0) / CatalogService.adjustStock | No test |
| FR-INV-04 | Restore reserved stock when an order is cancelled while CREATED (BR-12). | Partial | OrderService.restoreReservedStock | No test |
| FR-INV-05 | Manager and Admin should adjust stock with a mandatory reason; every adjustment is audited. | Partial | CatalogService.adjustStock (reason required, audit written) | No test |
| FR-KDS-01 | When an order enters IN_KITCHEN, create one ticket per station with items, quantities, modifiers, note, token, order type, rush flag, and creation time. | Partial | OrderService.create (ticket creation after payment) | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| FR-KDS-02 | A station screen shall list that station's open tickets (NEW and IN_PROGRESS) in BR-10 order, with an age timer and BR-11 colour. | Not done | OrderService.stationTickets (ordering and colour not implemented) | TBD |
| FR-KDS-03 | Kitchen user shall START a NEW ticket and BUMP an IN_PROGRESS ticket at a bound station; Manager may do so at any station. | Partial | OrderService.ticketAction (accept/start/bump) | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| FR-KDS-04 | Kitchen user or Manager shall RECALL a DONE ticket within the recall window (BR-17); ticket returns to IN_PROGRESS. | Partial | OrderService.ticketAction recall (no window enforcement) | TicketRecordIntegrationTests.recallingReadyTicketIncrementsRecallCountAndReopensOrder |
| FR-KDS-05 | Push ticket creation, change, and cancellation to subscribed station screens in real time via SSE; a reconnecting screen reloads full current state. | Partial | BoardStream (board only; no per-station SSE push on ticket change) | No stream test |
| FR-KDS-06 | Set an order to READY exactly once, in the same transaction as the bump of its last open ticket (BR-06). | Partial | OrderService.updateOrderReady (atomic READY update with status = IN_KITCHEN guard) | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| FR-KDS-07 | Detect two users changing the same ticket concurrently; accept one, reject the other with 409 STALE_VERSION and current ticket state. | Partial | OrderService.ticketAction (checkVersion) | No concurrency test |
| FR-KDS-08 | When an order is cancelled, remove open tickets from station queues and show a CANCELLED notice until acknowledged. | Partial | OrderService.cancel (cancels NEW/ACCEPTED/IN_PROGRESS tickets) / ticketAction acknowledge-cancel | No test |
| FR-KDS-09 | The manager board shall show every open order with the status of each of its tickets and list tickets past their target time. | Not done | TBD | TBD |
| FR-KDS-10 | Show a distinct rush marker on rush tickets; rush tickets sort first. | Not done | TBD | TBD |
| FR-MENU-01 | Admin shall create, edit, and deactivate categories with name, display order, and GST rate (0–28%); duplicate name returns 422. | Partial | CatalogService.createCategory / updateCategory | No test |
| FR-MENU-02 | Admin shall create, edit, and deactivate items with name, description, category, base price, ≥1 station, target prep time, and optional stock tracking; item with no station rejected. | Partial | CatalogService.saveMenuItem (station validation) | No test |
| FR-MENU-03 | Admin shall define modifier groups per item with selection type (SINGLE/MULTIPLE), min/max selections, and options with optional price change. | Partial | CatalogService.createModifierGroup / createModifier / createItemModifierGroup | No test |
| FR-MENU-04 | Manager and Admin shall mark an item unavailable or available, effective for new orders immediately. | Partial | CatalogService.setAvailability | No test |
| FR-MENU-05 | Never hard-delete an item that appears in any order; deactivate it instead; a delete of a used item returns 409. | Partial | CatalogService.archiveMenuItem (409 when referenced by order_line) | No test |
| FR-MENU-06 | Provide a POS menu endpoint returning active items grouped by category with availability and modifier groups; inactive items excluded; p95 < 300 ms. | Partial | CatalogService.menu (no ETag / caching yet) | No test |
| FR-MENU-07 | Support combos: a named bundle of items at a fixed price that expands into component items for routing. | Not done | TBD | TBD |
| FR-MENU-08 | Apply a price change only to orders created after it; existing order lines retain their snapshot prices. | Partial | OrderService.create (snapshot written at order time) | TicketRecordIntegrationTests (pricing assertions) |
| FR-ORD-01 | Cashier and Manager shall create an order with order type (DINE_IN/TAKEAWAY/DELIVERY), ≥1 line (item, qty 1–50, modifiers), and optional note ≤200 chars; valid request returns 201. | Partial | OrderService.create | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| FR-ORD-02 | Validate every order and return all validation errors together, each with its field path; a request with an inactive item and invalid qty returns one 422 listing both. | Partial | OrderService.create / saveLineModifiers (partial field-level errors) | No test |
| FR-ORD-03 | Calculate line totals, GST per line, and order total using BR-02 and BR-03; return the breakdown. | Partial | OrderService.bill / orderTotal | TicketRecordIntegrationTests (bill assertions) |
| FR-ORD-04 | Generate a unique token (BR-01) and a unique human-readable order number; 1,000 concurrent creations yield unique tokens. | Partial | OrderService.create (token_counter upsert) | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| FR-ORD-05 | Enforce the order lifecycle (section 6.1); reject every illegal transition with 409 INVALID_TRANSITION. | Partial | OrderService.cancel / complete / recordPayment (status guards) | No transition matrix test |
| FR-ORD-06 | Cashier and Manager shall edit order lines or note while CREATED; edits recalculate totals and adjust stock. | Not done | TBD | TBD |
| FR-ORD-07 | Cancel an order under BR-05 with a mandatory reason; cancel open tickets; handle stock under BR-12. | Partial | OrderService.cancel | No test |
| FR-ORD-08 | Cashier and Manager shall mark a READY order COMPLETED when it is handed over. | Partial | OrderService.complete | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| FR-ORD-09 | Search orders by status, date range, token, order number, and type; pagination default 20 max 100; newest-first; p95 < 300 ms with 10 k orders. | Not done | OrderService.orders (no filter params yet) | TBD |
| FR-ORD-10 | Make order creation idempotent via Idempotency-Key header (BR-15); 50 concurrent identical requests produce one order and identical responses. | Partial | OrderService.create (advisory lock + idempotency_record) | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| FR-ORD-11 | Manager shall flag or unflag an open order as RUSH; a flagged order sorts first on every station queue. | Partial | OrderService.setRush (rush flag update) | No test |
| FR-ORD-12 | Cancel orders left unpaid in CREATED longer than the configured timeout (default 15 min) and restore their stock. | Not done | TBD | TBD |
| FR-PAY-01 | Cashier and Manager shall record a payment for a CREATED order with method (CASH/UPI/CARD), amount, and optional reference ≤64 chars; success returns 201 and order moves to PAID. | Partial | OrderService.recordPayment | TicketRecordIntegrationTests.createsIdempotentOrderAndCompletesPaidKitchenLifecycle |
| FR-PAY-02 | On successful payment, create kitchen tickets and move the order to IN_KITCHEN in the same transaction; a ticket-creation fault leaves the order CREATED with no payment. | Partial | OrderService.recordPayment (IN_KITCHEN update + ticket creation in create; not in recordPayment) | No fault injection test |
| FR-PAY-03 | Allow exactly one successful payment per order; two concurrent payment requests give one 201 and one 409 PAYMENT_ALREADY_RECORDED; unique DB constraint exists. | Partial | OrderService.recordPayment (status gate) | No concurrency test |
| FR-PAY-04 | Reject a payment whose amount differs from the order total with 422 AMOUNT_MISMATCH; no state change. | Partial | OrderService.recordPayment (AMOUNT_MISMATCH check) | No test |
| FR-PAY-05 | Return a bill with items, modifiers, GST breakdown, total, token, order number, time, and payment method. | Partial | OrderService.bill | TicketRecordIntegrationTests (bill assertions) |
| FR-PAY-06 | For cash payments, accept the tendered amount and return the change; tendered below the total returns 422. | Partial | OrderService.recordPayment (tendered < due check) | No test |
| FR-PAY-07 | Record a refund entry (status REFUND_PENDING) when a paid order is cancelled. | Partial | OrderService.cancel (payment_refund insert when payment exists) | No test |
| FR-PAY-08 | The system may render the bill as a printable PDF. | Not done | TBD | TBD |
| FR-REC-01 | Record ticket lifecycle events (accept, start, bump, serve, recall) with actor, timestamp, and durations. | Partial | OrderService.ticketAction (audit write per action) | TicketRecordIntegrationTests.stationRecordsFilterAndReturnAcceptedAndServedActorsAndDurations |
| FR-REC-02 | Provide per-station ticket record endpoint filtered by state (ACCEPTED/SERVED/ALL) and token. | Partial | OrderService.stationTicketRecords | TicketRecordIntegrationTests.stationRecordsFilterAndReturnAcceptedAndServedActorsAndDurations |
| FR-REC-03 | Record accept-to-start, cook, and pickup durations in seconds for each ticket. | Partial | OrderService.stationTicketRecords (computed columns) | TicketRecordIntegrationTests.stationRecordsFilterAndReturnAcceptedAndServedActorsAndDurations |
| FR-REC-04 | Record recall count per ticket. | Partial | OrderService.ticketAction recall (recall_count + 1) | TicketRecordIntegrationTests.recallingReadyTicketIncrementsRecallCountAndReopensOrder |
| FR-REC-05 | Export ticket records as CSV. | Not done | TBD | TBD |
| FR-REC-06 | Record the actor who accepted and served each ticket. | Partial | OrderService.ticketAction (accepted_by / served_by columns) | TicketRecordIntegrationTests.stationRecordsFilterAndReturnAcceptedAndServedActorsAndDurations |
| FR-REC-07 | Filter ticket records for the current business day. | Partial | OrderService.stationTicketRecords (business_date filter) | No explicit date-boundary test |
| FR-REC-08 | Station binding enforced on ticket record endpoint (Kitchen sees only own stations). | Partial | OrderService.stationTicketRecords (KITCHEN station check) | No test |
| FR-REC-09 | Ticket records support token-prefix search. | Partial | OrderService.stationTicketRecords (LIKE token filter) | No test |
| FR-RPT-01 | Manager and Admin shall obtain a sales summary (order count, gross, GST, total grouped by payment method and order type) for a business day or date range. | Not done | TBD | TBD |
| FR-RPT-02 | Report top N items by quantity and revenue for a date range; ties broken by name. | Not done | TBD | TBD |
| FR-RPT-03 | Report preparation time per station and per item (avg, median, p90) and the share of tickets exceeding target. | Not done | TBD | TBD |
| FR-RPT-04 | Report orders per hour of day for a date range (24 hourly buckets). | Not done | TBD | TBD |
| FR-RPT-05 | Report cancelled orders grouped by reason and by cancelling role. | Not done | TBD | TBD |
| FR-RPT-06 | Compute all reports in the database with SQL aggregation; accept a date range of at most 92 days. | Not done | TBD | TBD |
| FR-RPT-07 | The system may export any report as CSV; exported rows equal on-screen rows. | Not done | TBD | TBD |
| FR-SYS-01 | The API shall be versioned under /api/v1, use JSON, and return every error in one consistent envelope. | Partial | SecurityConfiguration / edge/error/ApiExceptionHandler | No envelope test |
| FR-SYS-02 | Generate OpenAPI documentation from the code and serve an interactive page. | Not done | TBD | TBD |
| FR-SYS-03 | List endpoints shall support pagination, sorting, and filtering with the same parameter names everywhere. | Not done | TBD | TBD |
| FR-SYS-04 | Assign a correlation ID to every request, return it in a response header, and include it in every log line. | Partial | CorrelationIdFilter | No test |
| FR-SYS-05 | Expose a public health endpoint and an Admin-only metrics endpoint; health reports database connectivity. | Partial | SecurityConfiguration (actuator/health public; actuator/** ADMIN) | No test |
| NFR-CONC-01 | 50 concurrent identical idempotent create requests create exactly one order. | Partial | OrderService.create (advisory lock + idempotency_record) | No concurrency test |
| NFR-CONC-02 | 100 concurrent orders for an item with stock 10 succeed exactly 10 times; stock never goes below zero. | Partial | OrderService.create (conditional UPDATE for stock) | No concurrency test |
| NFR-CONC-03 | Two concurrent updates to one ticket: one succeeds, the other returns 409 STALE_VERSION; the ticket stays valid. | Partial | OrderService.ticketAction (checkVersion) | No concurrency test |
| NFR-CONC-04 | Concurrent bumps of the last two tickets of one order produce exactly one READY transition. | Partial | OrderService.updateOrderReady (status = IN_KITCHEN guard) | No concurrency test |
| NFR-DOC-01 | The README documents how to run, test, and demo the system; API documentation is generated, not hand-written. | Not done | TBD | TBD |
| NFR-MNT-01 | Code is packaged by feature; controllers never access repositories directly; an automated architecture test enforces this. | Partial | Package structure (api/auth/board/catalog/order/edge packages) | No ArchUnit test found |
| NFR-MNT-02 | Line coverage is at least 70% on service and domain layers; the build fails below it. | Not done | TBD | TBD |
| NFR-MNT-03 | Static analysis and formatting checks run in CI and must pass. | Not done | TBD | TBD |
| NFR-OBS-01 | Logs are structured and carry the correlation ID and order number on every order-related line. | Partial | CorrelationIdFilter (correlation ID propagated) | No log-content test |
| NFR-OBS-02 | Metrics include orders created, ticket cycle time, open stream connections, and error count. | Not done | TBD | TBD |
| NFR-PERF-01 | Order creation including pricing and stock reservation completes within 300 ms at p95 under 50 concurrent users with 100 k orders in the DB. | Not done | TBD | TBD |
| NFR-PERF-02 | A new ticket appears on a connected station screen within 1 s at p95 after the payment transaction commits, with 10 screens connected. | Not done | TBD | TBD |
| NFR-PERF-03 | List and search endpoints respond within 300 ms at p95 with 100 k orders. | Not done | TBD | TBD |
| NFR-PERF-04 | Each report responds within 2 s at p95 over a 31-day range with 100 k orders. | Not done | TBD | TBD |
| NFR-PERF-05 | Reports limited to 2 concurrent queries (REPORT_CONCURRENCY rate class). | Not done | TBD | TBD |
| NFR-POR-01 | One `docker compose` command starts the application and database with seed data on Windows, Linux, and macOS. | Not done | TBD | TBD |
| NFR-REL-01 | State-changing operations are atomic; a failure leaves no partial state. | Partial | @Transactional on all mutating methods | No fault injection test |
| NFR-REL-02 | After an application restart no committed data is lost; station screens reconnect and reload within 3 s. | Not done | TBD | TBD |
| NFR-REL-03 | Schema changes are applied only through versioned Flyway migrations. | Partial | src/main/resources/db/migration/ (V1 migration present) | No migration-fail test |
| NFR-SEC-01 | Tokens are signed with a key supplied through the environment, never committed, and expire after 15 minutes. | Partial | AccessTokenService / KitchenFlowProperties (jwt key from config) | No expiry test |
| NFR-SEC-02 | All input is validated on the server; every query is parameterised; no SQL is built from user text. | Partial | OrderService / CatalogService (JdbcTemplate parameterised queries) | No SQL-injection test |
| NFR-SEC-03 | CORS is limited to configured origins; standard security headers are set; HTTPS required outside local development. | Partial | SecurityConfiguration (CORS configured) | No CORS test |
| NFR-SEC-04 | The system collects no customer personal data; staff data is limited to username, role, and password hash. | Partial | Schema / CatalogService.saveUser (no personal data columns) | Inspection only |
| NFR-SEC-05 | A review against the OWASP Top 10 is completed and its findings are logged. | Not done | TBD | TBD |
| NFR-SEC-06 | Rate limits are set above the design workload so normal operations produce zero 429 responses. | Partial | ApplicationRateLimitFilter (rate limiter present) | No load test |
| NFR-USE-01 | Station and board text is readable from 1.5 m: item names ≥ 24 px, status shown by text and icon as well as colour, touch targets ≥ 44 px. | Not done | TBD | TBD |
| NFR-USE-02 | A person new to the POS screen enters a 3-item order and records payment within 60 seconds after a 5-minute walkthrough. | Not done | TBD | TBD |

## Revision 0.2 test identifiers

The revision 0.2 test IDs (UT-18, UT-19, PC-15, PC-16, CC-16, CC-17, RM-06, SS-20, PF-17, AT-12) are proposed additions from the consolidated change register in KF-TPL-001 section 17. They are not defined in the v0.1 test plan; exact scenarios will be specified when SRS v0.2, SAD v0.2, and the other pending document revisions (changes 1–19) are applied. Associate them with concrete test methods when the full revision text is available.

| Test ID | Scenario (from change register context) | Status | Code location |
| --- | --- | --- | --- |
| UT-18 | Unit test added by change register (exact scenario TBD from v0.2 documents) | Not done | TBD |
| UT-19 | Unit test added by change register (exact scenario TBD from v0.2 documents) | Not done | TBD |
| PC-15 | Post-condition suite added by change register (exact scenario TBD from v0.2 documents) | Not done | TBD |
| PC-16 | Post-condition suite added by change register (exact scenario TBD from v0.2 documents) | Not done | TBD |
| CC-16 | Concurrency/mutation test added by change register (exact scenario TBD from v0.2 documents) | Not done | TBD |
| CC-17 | Concurrency/mutation test added by change register (exact scenario TBD from v0.2 documents) | Not done | TBD |
| RM-06 | Role-matrix test added by change register (exact scenario TBD from v0.2 documents) | Not done | TBD |
| SS-20 | Stream test added by change register (exact scenario TBD from v0.2 documents) | Not done | TBD |
| PF-17 | Performance test added by change register (exact scenario TBD from v0.2 documents) | Not done | TBD |
| AT-12 | Acceptance scenario added by change register (exact scenario TBD from v0.2 documents) | Not done | TBD |
