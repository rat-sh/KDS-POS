package com.kitchenflow.kds.report.service;

import com.kitchenflow.kds.common.error.ApiFailure;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private final JdbcTemplate jdbc;

    public ReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(LocalDate from, LocalDate to, int topPage, int topSize,
            int cancellationPage, int cancellationSize) {
        if (to.isBefore(from)) throw new ApiFailure(422, "VALIDATION_FAILED", "End date must be on or after the start date.");
        if (ChronoUnit.DAYS.between(from, to) > 91) throw new ApiFailure(422, "VALIDATION_FAILED", "Reports cover a maximum of 92 days.");
        Timestamp start = Timestamp.from(from.atStartOfDay(BUSINESS_ZONE).toInstant());
        Timestamp end = Timestamp.from(to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant());
        int safeTopPage = Math.max(topPage, 0);
        int safeTopSize = Math.clamp(topSize, 1, 100);
        int safeCancellationPage = Math.max(cancellationPage, 0);
        int safeCancellationSize = Math.clamp(cancellationSize, 1, 100);

        Map<String, Object> totals = jdbc.queryForObject("""
                SELECT count(*) AS orders,
                       count(*) FILTER (WHERE status = 'COMPLETED') AS completed,
                       count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled
                FROM order_header WHERE created_at >= ? AND created_at < ?
                """, (rs, row) -> map("orders", rs.getLong("orders"), "completed", rs.getLong("completed"),
                        "cancelled", rs.getLong("cancelled")), start, end);
        Map<String, Object> sales = jdbc.queryForObject("""
                SELECT coalesce(sum(net + gst), 0)::numeric(14,2) AS sales,
                       coalesce(sum(gst), 0)::numeric(14,2) AS gst
                FROM (
                    SELECT round((l.unit_price + coalesce(sum(m.price_delta), 0)) * l.quantity, 2) AS net,
                           round(((l.unit_price + coalesce(sum(m.price_delta), 0)) * l.quantity) * l.gst_rate / 100, 2) AS gst
                    FROM order_header o JOIN order_line l ON l.order_id = o.id
                    LEFT JOIN order_line_modifier m ON m.order_line_id = l.id
                    WHERE o.status = 'COMPLETED' AND o.created_at >= ? AND o.created_at < ?
                    GROUP BY l.id
                ) line_totals
                """, (rs, row) -> map("sales", rs.getBigDecimal("sales").toPlainString(),
                        "gst", rs.getBigDecimal("gst").toPlainString()), start, end);

        Integer topItemCount = jdbc.queryForObject("""
                SELECT count(DISTINCT l.item_id) FROM order_header o JOIN order_line l ON l.order_id = o.id
                WHERE o.status = 'COMPLETED' AND o.created_at >= ? AND o.created_at < ?
                """, Integer.class, start, end);
        List<Map<String, Object>> topItems = jdbc.query("""
                SELECT l.item_id, min(l.item_name) AS name, sum(l.quantity) AS quantity
                FROM order_header o JOIN order_line l ON l.order_id = o.id
                WHERE o.status = 'COMPLETED' AND o.created_at >= ? AND o.created_at < ?
                GROUP BY l.item_id ORDER BY quantity DESC, l.item_id
                LIMIT ? OFFSET ?
                """, (rs, row) -> map("itemId", rs.getLong("item_id"), "name", rs.getString("name"),
                        "quantity", rs.getLong("quantity")), start, end, safeTopSize, (long) safeTopPage * safeTopSize);

        List<Map<String, Object>> prepByStation = jdbc.query("""
                SELECT s.id AS station_id, s.name AS station_name,
                       round(avg(extract(epoch FROM (t.done_at - t.started_at))))::bigint AS average_seconds
                FROM order_header o JOIN kitchen_ticket t ON t.order_id = o.id
                JOIN station s ON s.id = t.station_id
                WHERE o.status = 'COMPLETED' AND o.created_at >= ? AND o.created_at < ?
                  AND t.started_at IS NOT NULL AND t.done_at IS NOT NULL
                GROUP BY s.id, s.name ORDER BY s.display_order, s.id
                """, (rs, row) -> map("stationId", rs.getLong("station_id"), "stationName", rs.getString("station_name"),
                        "averageSeconds", rs.getLong("average_seconds")), start, end);

        List<Map<String, Object>> ordersPerHour = jdbc.query("""
                SELECT hours.hour, coalesce(count(o.id), 0) AS count
                FROM generate_series(0, 23) AS hours(hour)
                LEFT JOIN order_header o ON o.paid_at >= ? AND o.paid_at < ?
                    AND extract(hour FROM o.paid_at AT TIME ZONE 'Asia/Kolkata') = hours.hour
                GROUP BY hours.hour ORDER BY hours.hour
                """, (rs, row) -> map("hour", rs.getInt("hour"), "count", rs.getLong("count")), start, end);

        Integer cancellationCount = jdbc.queryForObject("""
                SELECT count(*) FROM order_header
                WHERE status = 'CANCELLED' AND cancelled_at >= ? AND cancelled_at < ?
                """, Integer.class, start, end);
        List<Map<String, Object>> cancellations = jdbc.query("""
                SELECT id, token, cancellation_reason, cancelled_at FROM order_header
                WHERE status = 'CANCELLED' AND cancelled_at >= ? AND cancelled_at < ?
                ORDER BY cancelled_at DESC, id DESC LIMIT ? OFFSET ?
                """, (rs, row) -> map("id", rs.getLong("id"), "token", rs.getInt("token"),
                        "reason", rs.getString("cancellation_reason"), "cancelledAt", rs.getObject("cancelled_at")),
                start, end, safeCancellationSize, (long) safeCancellationPage * safeCancellationSize);

        return map("sales", sales.get("sales"), "gst", sales.get("gst"), "orders", totals.get("orders"),
                "completed", totals.get("completed"), "cancelled", totals.get("cancelled"),
                "topItems", page(topItems, safeTopPage, safeTopSize, topItemCount),
                "prepTimeByStation", prepByStation, "ordersPerHour", ordersPerHour,
                "cancellations", page(cancellations, safeCancellationPage, safeCancellationSize, cancellationCount));
    }

    private Map<String, Object> page(List<Map<String, Object>> content, int page, int size, Integer total) {
        int count = total == null ? 0 : total;
        return map("content", content, "page", page, "size", size, "totalElements", count,
                "totalPages", count == 0 ? 0 : (int) Math.ceil((double) count / size));
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) result.put((String) pairs[index], pairs[index + 1]);
        return result;
    }
}
