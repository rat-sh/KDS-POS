package com.kitchenflow.kds.order.service;

import com.kitchenflow.kds.common.security.ApiActor;
import com.kitchenflow.kds.common.error.ApiFailure;
import com.kitchenflow.kds.order.api.OrderRequests.ActionRequest;
import com.kitchenflow.kds.order.api.OrderRequests.PaymentRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final JdbcTemplate jdbc;

    public PaymentService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public Map<String, Object> payment(long orderId) {
        if (jdbc.queryForObject("SELECT count(*) FROM order_header WHERE id = ?", Integer.class, orderId) == 0) {
            throw new ApiFailure(404, "NOT_FOUND", "Order not found.");
        }
        var rows = jdbc.query("SELECT id, method, amount, tendered, reference, paid_at, paid_by FROM payment WHERE order_id = ?",
                (rs, row) -> {
                    Map<String, Object> payment = new java.util.LinkedHashMap<>();
                    payment.put("id", rs.getLong("id"));
                    payment.put("method", rs.getString("method"));
                    payment.put("amount", rs.getBigDecimal("amount").toPlainString());
                    payment.put("tendered", rs.getBigDecimal("tendered") == null ? null : rs.getBigDecimal("tendered").toPlainString());
                    payment.put("reference", rs.getString("reference"));
                    payment.put("paidAt", rs.getObject("paid_at"));
                    payment.put("paidBy", rs.getLong("paid_by"));
                    return payment;
                }, orderId);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "No payment is recorded for this order.");
        return rows.getFirst();
    }
    @Transactional
    public void recordPayment(long orderId, PaymentRequest request, ApiActor actor, String correlation) {
        OrderLock order = lockOrder(orderId);
        BigDecimal due = orderTotal(orderId);
        String method = request.method().toUpperCase();
        String priorPaymentMethod = jdbc.query("SELECT method FROM payment WHERE order_id = ?", rs -> rs.next() ? rs.getString(1) : null, orderId);
        if ("IN_KITCHEN".equals(order.status()) && method.equals(priorPaymentMethod)
                && request.amount().setScale(2, RoundingMode.HALF_UP).compareTo(due) == 0) return;
        if (!"CREATED".equals(order.status())) throw conflict("ORDER_ALREADY_PAID", "This order is not waiting for payment.");
        if (request.amount().setScale(2, RoundingMode.HALF_UP).compareTo(due) != 0) {
            throw new ApiFailure(422, "AMOUNT_MISMATCH", "The recorded amount does not match the server-calculated order total.", Map.of("expectedAmount", due.toPlainString()));
        }
        if (!List.of("CASH", "UPI", "CARD").contains(method)) throw bad("INVALID_PAYMENT_METHOD", "Choose cash, UPI, or card.");
        if (method.equals("CASH") && request.tendered() != null && request.tendered().compareTo(due) < 0) {
            throw new ApiFailure(422, "VALIDATION_FAILED", "Cash tendered must cover the amount due.", Map.of("field", "tendered", "reason", "CASH_TENDERED_TOO_LOW"));
        }
        jdbc.update("INSERT INTO payment (order_id, method, amount, tendered, reference, paid_by) VALUES (?, ?, ?, ?, ?, ?)",
                orderId, method, due, request.tendered(), request.reference(), actor.id());
        updateOrder("UPDATE order_header SET status = 'IN_KITCHEN', paid_at = now(), version = version + 1 WHERE id = ? AND version = ?", orderId, order.version());
        audit(actor, "ORDER", orderId, "PAYMENT_RECORDED", method, correlation);
    }
    @Transactional
    public void completeRefund(long refundId, ActionRequest request, ApiActor actor, String correlation) {
        var rows = jdbc.query("SELECT status, version FROM payment_refund WHERE id = ? FOR UPDATE",
                (rs, row) -> new RefundState(rs.getString("status"), rs.getLong("version")), refundId);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Refund not found.");
        RefundState refund = rows.getFirst();
        checkVersion(request.expectedVersion(), refund.version());
        if (!refund.status().equals("REFUND_PENDING")) throw conflict("INVALID_TRANSITION", "Only pending refunds can be completed.");
        jdbc.update("UPDATE payment_refund SET status = 'COMPLETED', completed_by = ?, completed_at = now(), version = version + 1 WHERE id = ?",
                actor.id(), refundId);
        audit(actor, "REFUND", refundId, "REFUND_COMPLETED", null, correlation);
    }

    private OrderLock lockOrder(long id) {
        var rows = jdbc.query("SELECT id, status, version FROM order_header WHERE id = ? FOR UPDATE",
                (rs, row) -> new OrderLock(rs.getLong("id"), rs.getString("status"), rs.getLong("version")), id);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Order not found.");
        return rows.getFirst();
    }

    private BigDecimal orderTotal(long orderId) {
        List<BigDecimal> totals = jdbc.query("""
                SELECT (l.unit_price + coalesce(sum(m.price_delta), 0)) * l.quantity AS net, l.gst_rate
                FROM order_line l LEFT JOIN order_line_modifier m ON m.order_line_id = l.id
                WHERE l.order_id = ? GROUP BY l.id
                """, (rs, row) -> {
                    BigDecimal net = rs.getBigDecimal("net");
                    BigDecimal tax = net.multiply(rs.getBigDecimal("gst_rate")).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    return net.add(tax);
                }, orderId);
        return totals.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private void updateOrder(String sql, Object... args) {
        if (jdbc.update(sql, args) == 0) throw conflict("STALE_VERSION", "This record changed on another device. Refresh and try again.");
    }
    private void audit(ApiActor actor, String type, long id, String action, String reason, String correlation) {
        jdbc.update("INSERT INTO audit_entry (actor_id, actor_role, entity_type, entity_id, action, correlation_id, reason) VALUES (?, ?, ?, ?, ?, ?, ?)",
                actor.id(), actor.role(), type, id, action, correlation, reason);
    }
    private void checkVersion(Long expected, long actual) {
        if (expected != null && expected != actual) throw conflict("STALE_VERSION", "This record changed on another device. Refresh and try again.");
    }
    private ApiFailure bad(String code, String message) { return new ApiFailure(422, "VALIDATION_FAILED", message, Map.of("reason", code)); }
    private ApiFailure conflict(String code, String message) {
        if (code.equals("STALE_VERSION")) return new ApiFailure(409, code, message);
        String reason = code.equals("ORDER_ALREADY_PAID") ? "PAYMENT_ALREADY_RECORDED" : code;
        return new ApiFailure(409, "INVALID_TRANSITION", message, Map.of("reason", reason));
    }
    private record OrderLock(long id, String status, long version) {}
    private record RefundState(String status, long version) {}
}
