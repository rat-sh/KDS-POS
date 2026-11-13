package com.kitchenflow.kds.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class OrderRequests {
    private OrderRequests() {}

    public record CreateOrderRequest(@NotBlank String orderType, String table,
            @NotEmpty List<@Valid OrderLineRequest> items, @Size(max = 500) String note,
            boolean priority, @Size(max = 250) String priorityReason, @Valid PaymentRequest payment) {}
    public record OrderLineRequest(@Min(1) long itemId, @Min(1) int quantity, List<Long> modifierIds) {}
    public record ActionRequest(Long expectedVersion) {}
    public record PaymentRequest(@NotBlank String method, @NotNull @DecimalMin("0.00") BigDecimal amount,
            @DecimalMin("0.00") BigDecimal tendered, @Size(max = 120) String reference) {}
    public record CancelRequest(@NotBlank String reasonCode, @Size(max = 500) String reasonText, Long expectedVersion) {}
    public record PriorityRequest(Boolean priority, String reason, Long expectedVersion) {}
    public record RushRequest(Boolean rush, Long expectedVersion) {}
    public record ManagerTicketActionRequest(@NotBlank String step, @NotBlank String reason, Long expectedVersion) {}
    public record TicketRecord(long ticketId, long orderId, String orderNumber, int token, long stationId,
            String state, java.time.Instant acceptedAt, String acceptedByName, java.time.Instant startedAt,
            java.time.Instant doneAt, java.time.Instant servedAt, String servedByName, long acceptToStartSeconds,
            long cookSeconds, long pickupSeconds, int recallCount) {}
    public record CreateOrderResult(Map<String, Object> order, boolean replayed) {}
}
