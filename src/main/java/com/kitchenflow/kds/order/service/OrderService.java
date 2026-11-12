package com.kitchenflow.kds.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitchenflow.kds.common.error.ApiFailure;
import com.kitchenflow.kds.order.api.OrderRequests.ActionRequest;
import com.kitchenflow.kds.order.api.OrderRequests.CancelRequest;
import com.kitchenflow.kds.order.api.OrderRequests.CreateOrderRequest;
import com.kitchenflow.kds.order.api.OrderRequests.CreateOrderResult;
import com.kitchenflow.kds.order.api.OrderRequests.OrderLineRequest;
import com.kitchenflow.kds.order.api.OrderRequests.PaymentRequest;
import com.kitchenflow.kds.common.security.ApiActor;
import com.kitchenflow.kds.common.time.BusinessDayService;
import com.kitchenflow.kds.order.api.OrderRequests.TicketRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final BusinessDayService businessDays;

    public OrderService(JdbcTemplate jdbc, ObjectMapper mapper, BusinessDayService businessDays) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.businessDays = businessDays;
    }

    @Transactional
    public CreateOrderResult create(CreateOrderRequest request, String idempotencyKey, ApiActor actor, String correlationId) {
        String requestHash = sha256(writeJson(request));
        boolean keyed = idempotencyKey != null;
        if (keyed) {
            if (!idempotencyKey.matches("[A-Za-z0-9_-]{1,80}")) {
                throw new ApiFailure(422, "VALIDATION_FAILED", "Idempotency-Key must contain 1 to 80 letters, digits, underscores, or hyphens.",
                        Map.of("field", "Idempotency-Key", "reason", "INVALID_FORMAT"));
            }
            jdbc.execute("SET LOCAL lock_timeout = '2s'");
            jdbc.query("SELECT pg_advisory_xact_lock(hashtext(?))", (org.springframework.jdbc.core.RowCallbackHandler) rs -> { }, actor.id() + ":" + idempotencyKey);
            var prior = jdbc.query("SELECT request_hash, response_body::text AS response_body, created_at FROM idempotency_record WHERE user_id = ? AND idempotency_key = ? AND created_at > now() - INTERVAL '24 hours'",
                    (rs, row) -> new CachedResponse(rs.getString("request_hash"), rs.getString("response_body"), rs.getTimestamp("created_at").toInstant().toString()), actor.id(), idempotencyKey);
            if (!prior.isEmpty()) {
                if (!prior.getFirst().requestHash().equals(requestHash)) throw new ApiFailure(422, "IDEMPOTENCY_KEY_REUSED",
                        "This request key was already used for a different order.", Map.of("firstSeenAt", prior.getFirst().createdAt()));
                try { return new CreateOrderResult(mapper.readValue(prior.getFirst().responseBody(), mapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)), true); }
                catch (Exception exception) { throw new IllegalStateException("Stored order response could not be read.", exception); }
            }
        }
        LocalDate businessDate = businessDays.currentBusinessDay();
        Integer tokenValue = jdbc.queryForObject("INSERT INTO token_counter (business_day, next_token) VALUES (?, 2) ON CONFLICT (business_day) DO UPDATE SET next_token = token_counter.next_token + 1 RETURNING next_token - 1", Integer.class, businessDate);
        int token = tokenValue == null ? 1 : tokenValue;
        String orderNumber = "KF-" + businessDate.toString().replace("-", "") + "-" + String.format("%04d", token);
        String orderType = request.orderType().toUpperCase();
        if (!List.of("DINE_IN", "TAKEAWAY", "DELIVERY").contains(orderType)) throw bad("INVALID_ORDER_TYPE", "Choose a valid order type.");
        if (orderType.equals("DINE_IN") && (request.table() == null || request.table().isBlank())) throw bad("TABLE_REQUIRED", "Enter a table for dine-in orders.");

        Long orderId = jdbc.queryForObject("""
                INSERT INTO order_header (order_number, business_date, token, order_type, table_name, status, rush, priority, priority_reason, note, created_by)
                VALUES (?, ?, ?, ?, ?, 'CREATED', FALSE, ?, ?, ?, ?) RETURNING id
                """, Long.class, orderNumber, businessDate, token, orderType, blankToNull(request.table()),
                    request.priority(), request.priority() ? blankToNull(request.priorityReason()) : null,
                blankToNull(request.note()), actor.id());

        Map<Long, List<Long>> linesByStation = new LinkedHashMap<>();
        Map<Long, Integer> targetByStation = new LinkedHashMap<>();
        for (OrderLineRequest line : request.items()) {
            var itemRows = jdbc.query("""
                    SELECT i.id, i.name, i.price, i.available, i.track_stock, i.stock_quantity, i.target_prep_seconds,
                           c.gst_rate, c.id AS category_id
                    FROM menu_item i JOIN menu_category c ON c.id = i.category_id
                    WHERE i.id = ? AND i.archived = false FOR UPDATE OF i
                    """, (rs, row) -> new OrderItem(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("price"),
                    rs.getBoolean("available"), rs.getBoolean("track_stock"), rs.getObject("stock_quantity", Integer.class),
                    rs.getInt("target_prep_seconds"), rs.getBigDecimal("gst_rate")), line.itemId());
            if (itemRows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "An order item no longer exists.");
            OrderItem item = itemRows.getFirst();
            if (!item.available() || (item.trackStock() && item.stockQuantity() != null && item.stockQuantity() < line.quantity())) {
                throw conflict("ITEM_UNAVAILABLE", item.name() + " is not available in the requested quantity.");
            }
            Long lineId = jdbc.queryForObject("""
                    INSERT INTO order_line (order_id, item_id, item_name, quantity, unit_price, gst_rate, target_prep_seconds)
                    VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, orderId, item.id(), item.name(), line.quantity(), item.price(), item.gstRate(), item.targetSeconds());

            List<Long> stationIds = jdbc.queryForList("SELECT station_id FROM menu_item_station WHERE item_id = ? ORDER BY station_id", Long.class, item.id());
            if (stationIds.isEmpty()) throw conflict("ITEM_HAS_NO_STATION", item.name() + " is not assigned to a kitchen station.");
            for (long stationId : stationIds) {
                linesByStation.computeIfAbsent(stationId, ignored -> new ArrayList<>()).add(lineId);
                targetByStation.merge(stationId, item.targetSeconds(), Math::max);
            }
            saveLineModifiers(lineId, item.id(), line.modifierIds());
            if (item.trackStock()) jdbc.update("UPDATE menu_item SET stock_quantity = stock_quantity - ?, version = version + 1, available = (stock_quantity - ?) > 0 WHERE id = ?",
                    line.quantity(), line.quantity(), item.id());
        }

        for (var stationEntry : linesByStation.entrySet()) {
            Long ticketId = jdbc.queryForObject("INSERT INTO kitchen_ticket (order_id, station_id, status, target_seconds) VALUES (?, ?, 'NEW', ?) RETURNING id",
                    Long.class, orderId, stationEntry.getKey(), targetByStation.get(stationEntry.getKey()));
            for (long lineId : stationEntry.getValue()) jdbc.update("INSERT INTO ticket_line (ticket_id, order_line_id) VALUES (?, ?)", ticketId, lineId);
        }
        audit(actor, "ORDER", orderId, "ORDER_CREATED", null, correlationId);
        Map<String, Object> response = order(orderId);
        if (keyed) {
            jdbc.update("INSERT INTO idempotency_record (user_id, idempotency_key, request_hash, response_body) VALUES (?, ?, ?, ?::jsonb)", actor.id(), idempotencyKey, requestHash, writeJson(response));
        }
        return new CreateOrderResult(response, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> orders(int page, int size, String role) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 200);
        String visibility = role.equals("KITCHEN") ? "WHERE status <> 'CREATED' " : "";
        List<Long> ids = jdbc.queryForList("SELECT id FROM order_header " + visibility + "ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?", Long.class, safeSize, (long) safePage * safeSize);
        List<Map<String, Object>> content = ids.stream().map(this::order).toList();
        Integer total = jdbc.queryForObject("SELECT count(*) FROM order_header " + visibility, Integer.class);
        return map("content", content, "totalElements", total == null ? 0 : total, "totalPages", (total == null ? 0 : (int) Math.ceil((double) total / safeSize)));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> order(long id) {
        var rows = jdbc.query("SELECT * FROM order_header WHERE id = ?", (rs, row) -> {
            long orderId = rs.getLong("id");
            List<Map<String, Object>> lines = jdbc.query("SELECT id, item_id, quantity FROM order_line WHERE order_id = ? ORDER BY id",
                    (lineRs, lineRow) -> map("id", lineRs.getLong("id"), "itemId", lineRs.getLong("item_id"),
                            "quantity", lineRs.getInt("quantity"), "modifiers", jdbc.queryForList("SELECT modifier_id FROM order_line_modifier WHERE order_line_id = ? ORDER BY modifier_id", Long.class, lineRs.getLong("id"))), orderId);
            List<Map<String, Object>> tickets = jdbc.query("SELECT id, station_id, status, accepted_at, served_at, started_at, done_at, cancelled_acknowledged_at, version FROM kitchen_ticket WHERE order_id = ? ORDER BY station_id",
                    (ticketRs, ticketRow) -> map("id", ticketRs.getLong("id"), "stationId", ticketRs.getLong("station_id"),
                            "status", ticketRs.getString("status"), "acceptedAt", ticketRs.getObject("accepted_at"),
                            "servedAt", ticketRs.getObject("served_at"), "startedAt", ticketRs.getObject("started_at"),
                            "doneAt", ticketRs.getObject("done_at"), "cancelledAcknowledgedAt", ticketRs.getObject("cancelled_acknowledged_at"),
                            "version", ticketRs.getLong("version")), orderId);
            Map<String, Object> result = map("id", orderId, "orderNumber", rs.getString("order_number"),
                    "token", rs.getInt("token"), "table", rs.getString("table_name"), "status", rs.getString("status"),
                    "orderType", rs.getString("order_type"), "createdAt", rs.getObject("created_at"),
                    "paidAt", rs.getObject("paid_at"), "readyAt", rs.getObject("ready_at"),
                    "completedAt", rs.getObject("completed_at"), "rush", rs.getBoolean("rush"),
                    "priority", rs.getBoolean("priority"), "priorityReason", rs.getString("priority_reason"),
                    "note", rs.getString("note"), "lines", lines, "tickets", tickets,
                    "version", rs.getLong("version"));
            if (rs.getObject("cancelled_at") != null) {
                result.put("cancellation", map("reason", rs.getString("cancellation_reason"), "detail", "",
                        "actor", rs.getObject("cancelled_by"), "cancelledAt", rs.getObject("cancelled_at")));
            }
            var payments = jdbc.query("SELECT method, reference FROM payment WHERE order_id = ?", (paymentRs, paymentRow) -> map(
                    "method", paymentRs.getString("method"), "reference", paymentRs.getString("reference")), orderId);
            result.put("payment", payments.isEmpty() ? null : payments.getFirst());
            return result;
        }, id);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Order not found.");
        return rows.getFirst();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> payment(long orderId) {
        order(orderId);
        var rows = jdbc.query("SELECT id, method, amount, tendered, reference, paid_at, paid_by FROM payment WHERE order_id = ?",
                (rs, row) -> map("id", rs.getLong("id"), "method", rs.getString("method"),
                        "amount", rs.getBigDecimal("amount").toPlainString(),
                        "tendered", rs.getBigDecimal("tendered") == null ? null : rs.getBigDecimal("tendered").toPlainString(),
                        "reference", rs.getString("reference"), "paidAt", rs.getObject("paid_at"),
                        "paidBy", rs.getLong("paid_by")), orderId);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "No payment is recorded for this order.");
        return rows.getFirst();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> bill(long orderId) {
        Map<String, Object> order = order(orderId);
        List<Map<String, Object>> lines = jdbc.query("""
                SELECT l.id, l.item_name, l.quantity, l.unit_price, l.gst_rate,
                       coalesce(sum(m.price_delta), 0) AS modifier_total
                FROM order_line l LEFT JOIN order_line_modifier m ON m.order_line_id = l.id
                WHERE l.order_id = ? GROUP BY l.id ORDER BY l.id
                """, (rs, row) -> {
            BigDecimal unit = rs.getBigDecimal("unit_price").add(rs.getBigDecimal("modifier_total"));
            BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(rs.getInt("quantity"))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = subtotal.multiply(rs.getBigDecimal("gst_rate")).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return map("lineId", rs.getLong("id"), "name", rs.getString("item_name"),
                    "quantity", rs.getInt("quantity"), "unitPrice", unit.toPlainString(),
                    "gstRate", rs.getBigDecimal("gst_rate").toPlainString(), "subtotal", subtotal.toPlainString(),
                    "tax", tax.toPlainString(), "total", subtotal.add(tax).toPlainString());
        }, orderId);
        BigDecimal subtotal = lines.stream().map(line -> new BigDecimal((String) line.get("subtotal"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = lines.stream().map(line -> new BigDecimal((String) line.get("tax"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        return map("orderId", orderId, "orderNumber", order.get("orderNumber"), "token", order.get("token"),
                "outlet", jdbc.queryForObject("SELECT outlet_name FROM outlet_config WHERE id = 1", String.class),
                "lines", lines, "subtotal", subtotal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "tax", tax.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "total", subtotal.add(tax).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "payment", order.get("payment"));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> orderTickets(long orderId) {
        if (jdbc.queryForObject("SELECT count(*) FROM order_header WHERE id = ?", Integer.class, orderId) == 0) {
            throw new ApiFailure(404, "NOT_FOUND", "Order not found.");
        }
        return jdbc.queryForList("SELECT id FROM kitchen_ticket WHERE order_id = ? ORDER BY station_id", Long.class, orderId)
                .stream().map(this::ticket).toList();
    }

    @Transactional
    public void recordPayment(long orderId, PaymentRequest request, ApiActor actor, String correlationId) {
        Map<String, Object> order = order(orderId);
        BigDecimal due = orderTotal(orderId);
        String method = request.method().toUpperCase();
        if ("IN_KITCHEN".equals(order.get("status")) && order.get("payment") instanceof Map<?, ?> payment
                && method.equals(payment.get("method")) && request.amount().setScale(2, RoundingMode.HALF_UP).compareTo(due) == 0) return;
        if (!"CREATED".equals(order.get("status"))) throw conflict("ORDER_ALREADY_PAID", "This order is not waiting for payment.");
        if (request.amount().setScale(2, RoundingMode.HALF_UP).compareTo(due) != 0) {
            throw new ApiFailure(422, "AMOUNT_MISMATCH", "The recorded amount does not match the server-calculated order total.", Map.of("expectedAmount", due.toPlainString()));
        }
        if (!List.of("CASH", "UPI", "CARD").contains(method)) throw bad("INVALID_PAYMENT_METHOD", "Choose cash, UPI, or card.");
        if (method.equals("CASH") && request.tendered() != null && request.tendered().compareTo(due) < 0) {
            throw new ApiFailure(422, "VALIDATION_FAILED", "Cash tendered must cover the amount due.", Map.of("field", "tendered", "reason", "CASH_TENDERED_TOO_LOW"));
        }
        jdbc.update("INSERT INTO payment (order_id, method, amount, tendered, reference, paid_by) VALUES (?, ?, ?, ?, ?, ?)",
                orderId, method, due, request.tendered(), request.reference(), actor.id());
        jdbc.update("UPDATE order_header SET status = 'IN_KITCHEN', paid_at = now(), version = version + 1 WHERE id = ?", orderId);
        audit(actor, "ORDER", orderId, "PAYMENT_RECORDED", method, correlationId);
    }

    @Transactional
    public void cancel(long orderId, CancelRequest request, ApiActor actor, String correlationId) {
        Map<String, Object> current = order(orderId);
        checkVersion(request.expectedVersion(), ((Number) current.get("version")).longValue());
        if (List.of("CANCELLED", "COMPLETED").contains(current.get("status"))) throw conflict("ORDER_CLOSED", "This order is already closed.");
        if (!actor.role().equals("MANAGER") && !actor.role().equals("ADMIN") && !"CREATED".equals(current.get("status"))) {
            throw new ApiFailure(403, "FORBIDDEN", "Only a manager can cancel a paid order.");
        }
        String reason = request.reasonCode() + (request.reasonText() == null || request.reasonText().isBlank() ? "" : ": " + request.reasonText().trim());
        if ("CREATED".equals(current.get("status"))) restoreReservedStock(orderId);
        Integer paymentCount = jdbc.queryForObject("SELECT count(*) FROM payment WHERE order_id = ?", Integer.class, orderId);
        if (paymentCount != null && paymentCount > 0) {
            Long paymentId = jdbc.queryForObject("SELECT id FROM payment WHERE order_id = ?", Long.class, orderId);
            jdbc.update("INSERT INTO payment_refund (order_id, payment_id, amount, status, reason, initiated_by) VALUES (?, ?, ?, 'REFUND_PENDING', ?, ?)",
                    orderId, paymentId, orderTotal(orderId), reason, actor.id());
        }
        jdbc.update("UPDATE order_header SET status = 'CANCELLED', cancelled_at = now(), cancelled_by = ?, cancellation_reason = ?, version = version + 1 WHERE id = ?",
                actor.id(), reason, orderId);
        jdbc.update("UPDATE kitchen_ticket SET status = 'CANCELLED', version = version + 1 WHERE order_id = ? AND status IN ('NEW', 'ACCEPTED', 'IN_PROGRESS')", orderId);
        audit(actor, "ORDER", orderId, "ORDER_CANCELLED", reason, correlationId);
    }

    private void restoreReservedStock(long orderId) {
        List<Map<String, Object>> reserved = jdbc.query("SELECT l.item_id, l.quantity FROM order_line l JOIN menu_item i ON i.id = l.item_id WHERE l.order_id = ? AND i.track_stock = true",
                (rs, row) -> map("itemId", rs.getLong("item_id"), "quantity", rs.getInt("quantity")), orderId);
        for (Map<String, Object> item : reserved) {
            jdbc.update("UPDATE menu_item SET stock_quantity = coalesce(stock_quantity, 0) + ?, available = true, version = version + 1 WHERE id = ?",
                    item.get("quantity"), item.get("itemId"));
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> refunds(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, 100);
        return jdbc.query("""
                SELECT r.id, r.order_id, r.payment_id, r.amount, r.status, r.reason, r.created_at,
                       r.completed_at, r.version, u.display_name AS initiated_by_name
                FROM payment_refund r JOIN app_user u ON u.id = r.initiated_by
                ORDER BY r.created_at DESC, r.id DESC LIMIT ? OFFSET ?
                """, (rs, row) -> map("id", rs.getLong("id"), "orderId", rs.getLong("order_id"),
                "paymentId", rs.getLong("payment_id"), "amount", rs.getBigDecimal("amount").toPlainString(),
                "status", rs.getString("status"), "reason", rs.getString("reason"),
                "createdAt", rs.getObject("created_at"), "completedAt", rs.getObject("completed_at"),
                "version", rs.getLong("version"), "initiatedByName", rs.getString("initiated_by_name")),
                safeSize, (long) safePage * safeSize);
    }

    @Transactional
    public void completeRefund(long refundId, Long expectedVersion, ApiActor actor, String correlationId) {
        var rows = jdbc.query("SELECT status, version FROM payment_refund WHERE id = ? FOR UPDATE",
                (rs, row) -> new RefundRow(rs.getString("status"), rs.getLong("version")), refundId);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Refund not found.");
        RefundRow refund = rows.getFirst();
        checkVersion(expectedVersion, refund.version());
        if (!refund.status().equals("REFUND_PENDING")) throw conflict("INVALID_TRANSITION", "Only pending refunds can be completed.");
        jdbc.update("UPDATE payment_refund SET status = 'COMPLETED', completed_by = ?, completed_at = now(), version = version + 1 WHERE id = ?",
                actor.id(), refundId);
        audit(actor, "REFUND", refundId, "REFUND_COMPLETED", null, correlationId);
    }

    @Transactional
    public void complete(long orderId, ActionRequest request, ApiActor actor, String correlationId) {
        Map<String, Object> current = order(orderId);
        checkVersion(request.expectedVersion(), ((Number) current.get("version")).longValue());
        if (!"READY".equals(current.get("status"))) throw conflict("ORDER_NOT_READY", "Only a ready order can be closed.");
        Integer outstanding = jdbc.queryForObject("SELECT count(*) FROM kitchen_ticket WHERE order_id = ? AND status NOT IN ('SERVED', 'CANCELLED')", Integer.class, orderId);
        if (outstanding != null && outstanding > 0) throw new ApiFailure(409, "INVALID_TRANSITION",
                "Every active station ticket must be served before closing this order.",
                Map.of("currentStatus", current.get("status"), "requestedOperation", "COMPLETE", "reason", "TICKETS_NOT_SERVED"));
        jdbc.update("UPDATE order_header SET status = 'COMPLETED', completed_at = now(), version = version + 1 WHERE id = ?", orderId);
        audit(actor, "ORDER", orderId, "ORDER_COMPLETED", null, correlationId);
    }

    @Transactional
    public void setPriority(long orderId, boolean priority, String reason, Long expectedVersion, ApiActor actor, String correlationId) {
        Map<String, Object> current = order(orderId);
        checkVersion(expectedVersion, ((Number) current.get("version")).longValue());
        if (List.of("CANCELLED", "COMPLETED").contains(current.get("status"))) throw conflict("ORDER_CLOSED", "Closed orders cannot be reprioritized.");
        if (priority && (reason == null || reason.isBlank())) throw bad("REASON_REQUIRED", "Enter a reason for priority service.");
        jdbc.update("UPDATE order_header SET priority = ?, priority_reason = ?, version = version + 1 WHERE id = ?",
                priority, priority ? reason.trim() : null, orderId);
        audit(actor, "ORDER", orderId, priority ? "PRIORITY_SET" : "PRIORITY_CLEARED", reason, correlationId);
    }

    @Transactional
    public void setRush(long orderId, boolean rush, Long expectedVersion, ApiActor actor, String correlationId) {
        Map<String, Object> current = order(orderId);
        checkVersion(expectedVersion, ((Number) current.get("version")).longValue());
        if (List.of("CANCELLED", "COMPLETED").contains(current.get("status"))) throw conflict("ORDER_CLOSED", "Closed orders cannot be rushed.");
        jdbc.update("UPDATE order_header SET rush = ?, version = version + 1 WHERE id = ?", rush, orderId);
        audit(actor, "ORDER", orderId, rush ? "RUSH_SET" : "RUSH_CLEARED", null, correlationId);
    }

    @Transactional
    public void managerTicketAction(long ticketId, String step, String reason, ActionRequest request, ApiActor actor, String correlationId) {
        if (!List.of("START", "DONE", "SERVED").contains(step) || reason == null || reason.isBlank()) throw bad("INVALID_OVERRIDE", "Choose a ticket step and enter a reason.");
        var records = jdbc.query("SELECT id, order_id, station_id, status, version FROM kitchen_ticket WHERE id = ? FOR UPDATE",
                (rs, row) -> new TicketRow(rs.getLong("id"), rs.getLong("order_id"), rs.getLong("station_id"), rs.getString("status"), rs.getLong("version")), ticketId);
        if (records.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Kitchen ticket not found.");
        TicketRow ticket = records.getFirst();
        checkVersion(request.expectedVersion(), ticket.version());
        ensureOrderPayable(ticket.orderId());
        switch (step) {
            case "START" -> {
                if (!List.of("NEW", "ACCEPTED").contains(ticket.status())) throw conflict("INVALID_TRANSITION", "This ticket cannot be started from its current state.");
                jdbc.update("UPDATE kitchen_ticket SET status = 'IN_PROGRESS', accepted_at = coalesce(accepted_at, now()), accepted_by = coalesce(accepted_by, ?), started_at = now(), version = version + 1 WHERE id = ?", actor.id(), ticketId);
            }
            case "DONE" -> {
                if (!List.of("ACCEPTED", "IN_PROGRESS").contains(ticket.status())) throw conflict("INVALID_TRANSITION", "This ticket cannot be marked ready from its current state.");
                jdbc.update("UPDATE kitchen_ticket SET status = 'DONE', started_at = coalesce(started_at, now()), done_at = now(), version = version + 1 WHERE id = ?", ticketId);
                updateOrderReady(ticket.orderId());
            }
            case "SERVED" -> {
                requireTicketStatus(ticket.status(), "DONE");
                jdbc.update("UPDATE kitchen_ticket SET status = 'SERVED', served_at = now(), served_by = ?, version = version + 1 WHERE id = ?", actor.id(), ticketId);
                updateOrderReady(ticket.orderId());
            }
        }
        audit(actor, "TICKET", ticketId, "MANAGER_STEP_IN_" + step, reason.trim(), correlationId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> stationTickets(long stationId, ApiActor actor) {
        if (actor.role().equals("KITCHEN") && !jdbc.queryForList("SELECT station_id FROM user_station WHERE user_id = ? AND station_id = ?", Long.class, actor.id(), stationId).contains(stationId)) {
            throw new ApiFailure(403, "STATION_FORBIDDEN", "Your account is not assigned to this station.");
        }
        return jdbc.query("SELECT t.id FROM kitchen_ticket t JOIN order_header o ON o.id = t.order_id WHERE t.station_id = ? AND o.status <> 'CREATED' ORDER BY t.created_at, t.id",
                (rs, row) -> ticket(rs.getLong("id")), stationId);
    }

    @Transactional(readOnly = true)
    public List<TicketRecord> stationTicketRecords(long stationId, String requestedState, String token, ApiActor actor) {
        if (actor.role().equals("KITCHEN") && !jdbc.queryForList(
                "SELECT station_id FROM user_station WHERE user_id = ? AND station_id = ?", Long.class,
                actor.id(), stationId).contains(stationId)) {
            throw new ApiFailure(403, "FORBIDDEN", "Your account is not assigned to this station.");
        }
        String state = requestedState == null || requestedState.isBlank() ? "ALL" : requestedState.trim().toUpperCase();
        if (!List.of("ACCEPTED", "SERVED", "ALL").contains(state)) {
            throw new ApiFailure(400, "INVALID_FORMAT", "State must be ACCEPTED, SERVED, or ALL.");
        }
        String tokenQuery = token == null ? "" : token.trim();
        return jdbc.query("""
                SELECT t.id, t.order_id, o.order_number, o.token, t.station_id, t.status,
                       t.accepted_at, accepted.display_name AS accepted_by_name, t.started_at,
                       t.done_at, t.served_at, served.display_name AS served_by_name, t.recall_count,
                       coalesce(floor(extract(epoch FROM (t.started_at - t.accepted_at))), 0)::bigint AS accept_to_start_seconds,
                       coalesce(floor(extract(epoch FROM (t.done_at - t.started_at))), 0)::bigint AS cook_seconds,
                       coalesce(floor(extract(epoch FROM (t.served_at - t.done_at))), 0)::bigint AS pickup_seconds
                FROM kitchen_ticket t
                JOIN order_header o ON o.id = t.order_id
                LEFT JOIN app_user accepted ON accepted.id = t.accepted_by
                LEFT JOIN app_user served ON served.id = t.served_by
                WHERE t.station_id = ? AND o.business_date = ?
                  AND (? = 'ALL' OR (? = 'ACCEPTED' AND t.accepted_at IS NOT NULL AND t.served_at IS NULL AND t.status <> 'CANCELLED')
                       OR (? = 'SERVED' AND t.served_at IS NOT NULL))
                  AND (? = '' OR CAST(o.token AS text) LIKE ?)
                ORDER BY coalesce(t.served_at, t.done_at, t.accepted_at, t.created_at) DESC, t.id DESC
                """, (rs, row) -> new TicketRecord(rs.getLong("id"), rs.getLong("order_id"),
                rs.getString("order_number"), rs.getInt("token"), rs.getLong("station_id"), rs.getString("status"),
                instant(rs.getTimestamp("accepted_at")), rs.getString("accepted_by_name"),
                instant(rs.getTimestamp("started_at")), instant(rs.getTimestamp("done_at")),
                instant(rs.getTimestamp("served_at")), rs.getString("served_by_name"),
                rs.getLong("accept_to_start_seconds"), rs.getLong("cook_seconds"),
                rs.getLong("pickup_seconds"), rs.getInt("recall_count")), stationId,
                businessDays.currentBusinessDay(), state, state, state, tokenQuery, "%" + tokenQuery + "%");
    }

    private java.time.Instant instant(java.sql.Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ticket(long id) {
        var rows = jdbc.query("""
                SELECT t.*, o.order_number, o.token, o.order_type, o.table_name, o.rush, o.note
                FROM kitchen_ticket t JOIN order_header o ON o.id = t.order_id WHERE t.id = ?
                """, (rs, row) -> map("id", rs.getLong("id"), "orderId", rs.getLong("order_id"),
                "orderNumber", rs.getString("order_number"), "token", rs.getInt("token"), "stationId", rs.getLong("station_id"),
                "status", rs.getString("status"), "orderType", rs.getString("order_type"), "table", rs.getString("table_name"),
                "rush", rs.getBoolean("rush"), "orderNote", rs.getString("note"), "items", ticketItems(rs.getLong("id")),
                "targetSeconds", rs.getInt("target_seconds"), "createdAt", rs.getObject("created_at"),
                "acceptedAt", rs.getObject("accepted_at"), "acceptedBy", rs.getObject("accepted_by"),
                "startedAt", rs.getObject("started_at"), "doneAt", rs.getObject("done_at"),
                "servedAt", rs.getObject("served_at"), "servedBy", rs.getObject("served_by"),
                "cancelledAcknowledgedAt", rs.getObject("cancelled_acknowledged_at"),
                "ageSeconds", jdbc.queryForObject("SELECT greatest(0, floor(extract(epoch FROM (now() - created_at)))::bigint) FROM kitchen_ticket WHERE id = ?", Long.class, rs.getLong("id")),
                "version", rs.getLong("version")), id);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Kitchen ticket not found.");
        return rows.getFirst();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ticketForActor(long id, ApiActor actor) {
        Map<String, Object> ticket = ticket(id);
        long stationId = ((Number) ticket.get("stationId")).longValue();
        if (actor.role().equals("KITCHEN") && !jdbc.queryForList(
                "SELECT station_id FROM user_station WHERE user_id = ? AND station_id = ?", Long.class,
                actor.id(), stationId).contains(stationId)) {
            throw new ApiFailure(403, "FORBIDDEN", "Your account is not assigned to this station.");
        }
        return ticket;
    }

    @Transactional
    public void ticketAction(long ticketId, String action, ActionRequest request, ApiActor actor, String correlationId) {
        var records = jdbc.query("SELECT id, order_id, station_id, status, version FROM kitchen_ticket WHERE id = ? FOR UPDATE",
                (rs, row) -> new TicketRow(rs.getLong("id"), rs.getLong("order_id"), rs.getLong("station_id"), rs.getString("status"), rs.getLong("version")), ticketId);
        if (records.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Kitchen ticket not found.");
        TicketRow ticket = records.getFirst();
        checkVersion(request.expectedVersion(), ticket.version());
        if (action.equals("acknowledge-cancel")) ensureOrderCancelled(ticket.orderId());
        else ensureOrderPayable(ticket.orderId());
        if (actor.role().equals("KITCHEN") && !jdbc.queryForList("SELECT station_id FROM user_station WHERE user_id = ?", Long.class, actor.id()).contains(ticket.stationId())) {
            throw new ApiFailure(403, "STATION_FORBIDDEN", "Your account is not assigned to this station.");
        }
        String next;
        switch (action) {
            case "accept" -> {
                requireTicketStatus(ticket.status(), "NEW"); next = "ACCEPTED";
                jdbc.update("UPDATE kitchen_ticket SET status = ?, accepted_at = now(), accepted_by = ?, version = version + 1 WHERE id = ?", next, actor.id(), ticketId);
            }
            case "start" -> {
                requireTicketStatus(ticket.status(), "ACCEPTED"); next = "IN_PROGRESS";
                jdbc.update("UPDATE kitchen_ticket SET status = ?, started_at = now(), version = version + 1 WHERE id = ?", next, ticketId);
            }
            case "bump" -> {
                if (!List.of("IN_PROGRESS", "ACCEPTED").contains(ticket.status())) throw conflict("INVALID_TRANSITION", "Accept and start this ticket before marking it done.");
                next = "DONE";
                jdbc.update("UPDATE kitchen_ticket SET status = ?, started_at = coalesce(started_at, now()), done_at = now(), version = version + 1 WHERE id = ?", next, ticketId);
                updateOrderReady(ticket.orderId());
            }
            case "serve" -> {
                requireTicketStatus(ticket.status(), "DONE"); next = "SERVED";
                jdbc.update("UPDATE kitchen_ticket SET status = ?, served_at = now(), served_by = ?, version = version + 1 WHERE id = ?", next, actor.id(), ticketId);
                updateOrderReady(ticket.orderId());
            }
            case "recall", "undo" -> {
                if (!List.of("DONE", "SERVED").contains(ticket.status())) throw conflict("INVALID_TRANSITION", "Only a ready or served ticket can be recalled.");
                next = "IN_PROGRESS";
                jdbc.update("UPDATE kitchen_ticket SET status = ?, started_at = coalesce(started_at, now()), done_at = null, served_at = null, served_by = null, recall_count = recall_count + 1, version = version + 1 WHERE id = ?", next, ticketId);
                jdbc.update("UPDATE order_header SET status = 'IN_KITCHEN', ready_at = null, version = version + 1 WHERE id = ? AND status = 'READY'", ticket.orderId());
            }
            case "acknowledge-cancel" -> {
                requireTicketStatus(ticket.status(), "CANCELLED"); next = ticket.status();
                jdbc.update("UPDATE kitchen_ticket SET cancelled_acknowledged_at = now(), version = version + 1 WHERE id = ?", ticketId);
            }
            default -> throw new ApiFailure(404, "NOT_FOUND", "Ticket action not found.");
        }
        audit(actor, "TICKET", ticketId, "TICKET_" + action.toUpperCase().replace('-', '_'), null, correlationId);
    }

    public Map<String, Object> publicBoard() {
        List<Integer> preparing = jdbc.queryForList("SELECT DISTINCT o.token FROM order_header o JOIN kitchen_ticket t ON t.order_id = o.id WHERE o.status = 'IN_KITCHEN' AND t.status IN ('ACCEPTED', 'IN_PROGRESS') ORDER BY o.token", Integer.class);
        List<Map<String, Object>> ready = jdbc.query("SELECT DISTINCT o.token, o.order_number, greatest(0, floor(extract(epoch FROM (now() - coalesce(o.paid_at, o.created_at))))::bigint) AS age_seconds FROM order_header o JOIN kitchen_ticket t ON t.order_id = o.id WHERE o.status = 'READY' ORDER BY o.token",
                (rs, row) -> map("token", rs.getInt("token"), "orderNumber", rs.getString("order_number"), "ageSeconds", rs.getLong("age_seconds")));
        return map("preparing", preparing, "ready", ready);
    }

    private List<Map<String, Object>> ticketItems(long ticketId) {
        return jdbc.query("""
                SELECT l.id AS line_id, l.item_id, l.item_name, l.quantity
                FROM ticket_line tl JOIN order_line l ON l.id = tl.order_line_id
                WHERE tl.ticket_id = ? ORDER BY l.id
                """, (rs, row) -> map("orderLineId", rs.getLong("line_id"), "itemId", rs.getLong("item_id"),
                "name", rs.getString("item_name"), "quantity", rs.getInt("quantity"),
                "modifierNames", jdbc.queryForList("SELECT modifier_name FROM order_line_modifier WHERE order_line_id = ? ORDER BY modifier_id", String.class, rs.getLong("line_id"))), ticketId);
    }

    private void saveLineModifiers(long lineId, long itemId, List<Long> modifierIds) {
        List<Long> selected = modifierIds == null ? List.of() : modifierIds.stream().distinct().toList();
        if (modifierIds != null && selected.size() != modifierIds.size()) throw bad("DUPLICATE_MODIFIER", "A modifier can only be selected once.");
        List<Map<String, Object>> groups = jdbc.query("SELECT g.id, g.min_select, g.max_select FROM item_modifier_group link JOIN modifier_group g ON g.id = link.group_id WHERE link.item_id = ?",
                (rs, row) -> map("id", rs.getLong("id"), "min", rs.getInt("min_select"), "max", rs.getInt("max_select")), itemId);
        Map<Long, Integer> groupCounts = new LinkedHashMap<>();
        for (long modifierId : selected) {
            var matches = jdbc.query("SELECT m.name, m.price_delta, m.group_id FROM menu_modifier m JOIN item_modifier_group link ON link.group_id = m.group_id WHERE m.id = ? AND link.item_id = ? AND m.active = true",
                    (rs, row) -> map("name", rs.getString("name"), "price", rs.getBigDecimal("price_delta"), "group", rs.getLong("group_id")), modifierId, itemId);
            if (matches.isEmpty()) throw bad("INVALID_MODIFIER", "A selected modifier is not available for this item.");
            Map<String, Object> modifier = matches.getFirst();
            long groupId = ((Number) modifier.get("group")).longValue();
            groupCounts.merge(groupId, 1, Integer::sum);
            jdbc.update("INSERT INTO order_line_modifier (order_line_id, modifier_id, modifier_name, price_delta) VALUES (?, ?, ?, ?)",
                    lineId, modifierId, modifier.get("name"), modifier.get("price"));
        }
        for (Map<String, Object> group : groups) {
            long groupId = ((Number) group.get("id")).longValue();
            int count = groupCounts.getOrDefault(groupId, 0);
            if (count < (int) group.get("min") || count > (int) group.get("max")) throw bad("INVALID_MODIFIER_SELECTION", "Choose the required number of options for each modifier group.");
        }
    }

    private BigDecimal orderTotal(long orderId) {
        List<BigDecimal> totals = jdbc.query("""
                SELECT (l.unit_price + coalesce(sum(m.price_delta), 0)) * l.quantity AS net,
                       l.gst_rate
                FROM order_line l LEFT JOIN order_line_modifier m ON m.order_line_id = l.id
                WHERE l.order_id = ? GROUP BY l.id
                """, (rs, row) -> {
                    BigDecimal net = rs.getBigDecimal("net");
                    BigDecimal tax = net.multiply(rs.getBigDecimal("gst_rate")).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    return net.add(tax);
                }, orderId);
        return totals.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private void updateOrderReady(long orderId) {
        Integer open = jdbc.queryForObject("SELECT count(*) FROM kitchen_ticket WHERE order_id = ? AND status NOT IN ('DONE', 'SERVED', 'CANCELLED')", Integer.class, orderId);
        if (open != null && open == 0) {
            jdbc.update("UPDATE order_header SET status = 'READY', ready_at = coalesce(ready_at, now()), version = version + 1 WHERE id = ? AND status = 'IN_KITCHEN'", orderId);
        }
    }

    private void requireTicketStatus(String current, String required) {
        if (!current.equals(required)) throw conflict("INVALID_TRANSITION", "This ticket has changed. Refresh the kitchen board and try again.");
    }

    private void ensureOrderPayable(long orderId) {
        List<String> statuses = jdbc.queryForList("SELECT status FROM order_header WHERE id = ?", String.class, orderId);
        if (statuses.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Order not found.");
        if ("CREATED".equals(statuses.getFirst())) throw conflict("ORDER_NOT_PAID", "A kitchen ticket cannot be changed before payment is recorded.");
        if ("CANCELLED".equals(statuses.getFirst()) || "COMPLETED".equals(statuses.getFirst())) throw conflict("ORDER_CLOSED", "Tickets on a closed order cannot be changed.");
    }

    private void ensureOrderCancelled(long orderId) {
        List<String> statuses = jdbc.queryForList("SELECT status FROM order_header WHERE id = ?", String.class, orderId);
        if (statuses.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Order not found.");
        if (!"CANCELLED".equals(statuses.getFirst())) throw conflict("ORDER_NOT_CANCELLED", "Only cancelled-order tickets can be acknowledged.");
    }

    private void checkVersion(Long expected, long actual) {
        if (expected != null && expected != actual) throw conflict("STALE_VERSION", "This record changed on another device. Refresh and try again.");
    }

    private void audit(ApiActor actor, String type, long id, String action, String reason, String correlationId) {
        jdbc.update("INSERT INTO audit_entry (actor_id, actor_role, entity_type, entity_id, action, correlation_id, reason) VALUES (?, ?, ?, ?, ?, ?, ?)", actor.id(), actor.role(), type, id, action, correlationId, reason);
    }

    private String writeJson(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Could not serialize API response.", exception); }
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("Could not hash API request.", exception); }
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ApiFailure bad(String code, String message) {
        return new ApiFailure(422, "VALIDATION_FAILED", message, Map.of("reason", code));
    }
    private ApiFailure conflict(String code, String message) {
        if (code.equals("STALE_VERSION") || code.equals("OUT_OF_STOCK") || code.equals("ITEM_UNAVAILABLE")) {
            return new ApiFailure(409, code, message);
        }
        String reason = code.equals("ORDER_ALREADY_PAID") ? "PAYMENT_ALREADY_RECORDED" : code;
        return new ApiFailure(409, "INVALID_TRANSITION", message, Map.of("reason", reason));
    }
    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) result.put((String) pairs[index], pairs[index + 1]);
        return result;
    }

    private record CachedResponse(String requestHash, String responseBody, String createdAt) {}
    private record OrderItem(long id, String name, BigDecimal price, boolean available, boolean trackStock,
            Integer stockQuantity, int targetSeconds, BigDecimal gstRate) {}
    private record TicketRow(long id, long orderId, long stationId, String status, long version) {}
    private record RefundRow(String status, long version) {}
}
