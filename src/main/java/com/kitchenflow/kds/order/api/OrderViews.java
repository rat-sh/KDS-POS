package com.kitchenflow.kds.order.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public final class OrderViews {
    private OrderViews() {}

    public record OrderPage(List<?> content, int totalElements, int totalPages) {}
    public record OrderResponse(Long id, String orderNumber, Integer token, String table, String status,
            String orderType, Object createdAt, Object paidAt, Object readyAt, Object completedAt,
            Boolean rush, Boolean priority, String priorityReason, String note, List<Map<String, Object>> lines,
            List<Map<String, Object>> tickets, Long version,
            @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> cancellation,
            Map<String, Object> payment) {}
    public record KitchenOrderResponse(Long id, String orderNumber, Integer token, String table, String status,
            String orderType, Object createdAt, Object paidAt, Object readyAt, Object completedAt,
            Boolean rush, Boolean priority, String priorityReason, String note, List<Map<String, Object>> lines,
            List<Map<String, Object>> tickets, Long version,
            @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> cancellation) {}
    public record TicketResponse(Long id, Long orderId, String orderNumber, Integer token, Long stationId,
            String status, String orderType, String table, Boolean rush, String orderNote,
            List<Map<String, Object>> items, Integer targetSeconds, Object createdAt, Object acceptedAt,
            Object acceptedBy, Object startedAt, Object doneAt, Object servedAt, Object servedBy,
            Object cancelledAcknowledgedAt, Long ageSeconds, Long version) {}

    public static OrderPage page(Map<String, Object> source, boolean kitchen) {
        List<?> rows = rows(source.get("content")).stream().map(row -> kitchen ? kitchenOrder(row) : order(row)).toList();
        return new OrderPage(rows, number(source.get("totalElements")).intValue(), number(source.get("totalPages")).intValue());
    }

    public static OrderResponse order(Map<String, Object> row) {
        return new OrderResponse(longValue(row.get("id")), string(row.get("orderNumber")), integer(row.get("token")),
                string(row.get("table")), string(row.get("status")), string(row.get("orderType")), row.get("createdAt"),
                row.get("paidAt"), row.get("readyAt"), row.get("completedAt"), bool(row.get("rush")), bool(row.get("priority")),
                string(row.get("priorityReason")), string(row.get("note")), rows(row.get("lines")), rows(row.get("tickets")),
                longValue(row.get("version")), object(row.get("cancellation")), object(row.get("payment")));
    }

    public static KitchenOrderResponse kitchenOrder(Map<String, Object> row) {
        return new KitchenOrderResponse(longValue(row.get("id")), string(row.get("orderNumber")), integer(row.get("token")),
                string(row.get("table")), string(row.get("status")), string(row.get("orderType")), row.get("createdAt"),
                row.get("paidAt"), row.get("readyAt"), row.get("completedAt"), bool(row.get("rush")), bool(row.get("priority")),
                string(row.get("priorityReason")), string(row.get("note")), rows(row.get("lines")), rows(row.get("tickets")),
                longValue(row.get("version")), object(row.get("cancellation")));
    }

    public static TicketResponse ticket(Map<String, Object> row) {
        return new TicketResponse(longValue(row.get("id")), longValue(row.get("orderId")), string(row.get("orderNumber")),
                integer(row.get("token")), longValue(row.get("stationId")), string(row.get("status")), string(row.get("orderType")),
                string(row.get("table")), bool(row.get("rush")), string(row.get("orderNote")), rows(row.get("items")),
                integer(row.get("targetSeconds")), row.get("createdAt"), row.get("acceptedAt"), row.get("acceptedBy"),
                row.get("startedAt"), row.get("doneAt"), row.get("servedAt"), row.get("servedBy"),
                row.get("cancelledAcknowledgedAt"), longValue(row.get("ageSeconds")), longValue(row.get("version")));
    }

    private static List<Map<String, Object>> rows(Object value) {
        if (!(value instanceof List<?> values)) return List.of();
        return values.stream().map(row -> object(row)).toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> ? new LinkedHashMap<>((Map<String, Object>) value) : null;
    }

    private static Number number(Object value) { return value instanceof Number n ? n : null; }
    private static Long longValue(Object value) { return number(value) == null ? null : number(value).longValue(); }
    private static Integer integer(Object value) { return number(value) == null ? null : number(value).intValue(); }
    private static String string(Object value) { return value == null ? null : value.toString(); }
    private static Boolean bool(Object value) { return value instanceof Boolean b ? b : null; }
}
