package com.kitchenflow.kds;

import static org.assertj.core.api.Assertions.assertThat;

import com.kitchenflow.kds.common.security.ApiActor;
import com.kitchenflow.kds.common.time.BusinessDayService;
import com.kitchenflow.kds.order.api.OrderRequests.TicketRecord;
import com.kitchenflow.kds.order.api.OrderRequests.ActionRequest;
import com.kitchenflow.kds.order.service.OrderService;
import com.kitchenflow.kds.menu.service.CatalogService;
import com.kitchenflow.kds.menu.api.MenuItemRequest;
import com.kitchenflow.kds.order.api.OrderRequests.CreateOrderRequest;
import com.kitchenflow.kds.order.api.OrderRequests.OrderLineRequest;
import com.kitchenflow.kds.order.api.OrderRequests.PaymentRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TicketRecordIntegrationTests {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private OrderService orders;
    @Autowired private CatalogService catalog;
    @Autowired private BusinessDayService businessDays;

    @Test
    @Transactional
    void stationRecordsFilterAndReturnAcceptedAndServedActorsAndDurations() {
        Long managerId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'test-admin'", Long.class);
        int token = 80000 + Math.floorMod(UUID.randomUUID().hashCode(), 19999);
        Long orderId = jdbc.queryForObject("""
                INSERT INTO order_header (order_number, business_date, token, order_type, status, created_by)
                VALUES (?, ?, ?, 'TAKEAWAY', 'IN_KITCHEN', ?) RETURNING id
                """, Long.class, "KF-TEST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), businessDays.currentBusinessDay(), token, managerId);
        Instant now = Instant.now().minusSeconds(1);
        Long ticketId = jdbc.queryForObject("""
                INSERT INTO kitchen_ticket (order_id, station_id, status, target_seconds, accepted_at, accepted_by,
                    started_at, done_at, served_at, served_by, recall_count)
                VALUES (?, 1, 'SERVED', 600, ?, ?, ?, ?, ?, ?, 2) RETURNING id
                """, Long.class, orderId, Timestamp.from(now.minusSeconds(300)), managerId,
                Timestamp.from(now.minusSeconds(240)), Timestamp.from(now.minusSeconds(60)), Timestamp.from(now), managerId);
        ApiActor manager = new ApiActor(managerId, "test-admin", "MANAGER", 0);

        List<TicketRecord> served = orders.stationTicketRecords(1, "SERVED", Integer.toString(token), manager);
        List<TicketRecord> accepted = orders.stationTicketRecords(1, "ACCEPTED", Integer.toString(token), manager);
        List<TicketRecord> all = orders.stationTicketRecords(1, "ALL", Integer.toString(token), manager);

        assertThat(served).hasSize(1);
        assertThat(served.getFirst().ticketId()).isEqualTo(ticketId);
        assertThat(served.getFirst().acceptedByName()).isEqualTo("Test Administrator");
        assertThat(served.getFirst().servedByName()).isEqualTo("Test Administrator");
        assertThat(served.getFirst().acceptToStartSeconds()).isEqualTo(60);
        assertThat(served.getFirst().cookSeconds()).isEqualTo(180);
        assertThat(served.getFirst().pickupSeconds()).isEqualTo(60);
        assertThat(served.getFirst().recallCount()).isEqualTo(2);
        assertThat(accepted).isEmpty();
        assertThat(all).hasSize(1);
        assertThat(orders.stationTicketRecords(1, "ALL", "missing", manager)).isEmpty();
    }

    @Test
    @Transactional
    void servingKeepsOrderReadyUntilExplicitCompletion() {
        Long managerId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'test-admin'", Long.class);
        int token = 80000 + Math.floorMod(UUID.randomUUID().hashCode(), 19999);
        Long orderId = jdbc.queryForObject("""
                INSERT INTO order_header (order_number, business_date, token, order_type, status, created_by)
                VALUES (?, ?, ?, 'TAKEAWAY', 'IN_KITCHEN', ?) RETURNING id
                """, Long.class, "KF-TEST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), businessDays.currentBusinessDay(), token, managerId);
        Instant now = Instant.now().minusSeconds(1);
        Long ticketId = jdbc.queryForObject("""
                INSERT INTO kitchen_ticket (order_id, station_id, status, target_seconds, accepted_at, accepted_by,
                    started_at, done_at)
                VALUES (?, 1, 'DONE', 600, ?, ?, ?, ?) RETURNING id
                """, Long.class, orderId, Timestamp.from(now.minusSeconds(300)), managerId,
                Timestamp.from(now.minusSeconds(240)), Timestamp.from(now.minusSeconds(60)));
        ApiActor manager = new ApiActor(managerId, "test-admin", "MANAGER", 0);

        orders.ticketAction(ticketId, "serve", new ActionRequest(0L), manager, "served-test-123");
        String afterServe = jdbc.queryForObject("SELECT status FROM order_header WHERE id = ?", String.class, orderId);
        long readyVersion = jdbc.queryForObject("SELECT version FROM order_header WHERE id = ?", Long.class, orderId);
        orders.complete(orderId, new ActionRequest(readyVersion), manager, "complete-test-123");
        String afterComplete = jdbc.queryForObject("SELECT status FROM order_header WHERE id = ?", String.class, orderId);

        assertThat(afterServe).isEqualTo("READY");
        assertThat(afterComplete).isEqualTo("COMPLETED");
    }

    @Test
    @Transactional
    void createsIdempotentOrderAndCompletesPaidKitchenLifecycle() {
        Long managerId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'test-admin'", Long.class);
        ApiActor manager = new ApiActor(managerId, "test-admin", "MANAGER", 0);
        String uniqueName = "Smoke item " + UUID.randomUUID().toString().substring(0, 8);
        long itemId = catalog.saveMenuItem(null, new MenuItemRequest(uniqueName, "Food", new BigDecimal("10.00"),
                List.of("Fry"), 240, true, 0, false, List.of()), manager, "order-smoke-123");
        CreateOrderRequest request = new CreateOrderRequest("TAKEAWAY", null,
                List.of(new OrderLineRequest(itemId, 1, List.of())), "order lifecycle integration", false, null);
        String idempotencyKey = "order-smoke-" + UUID.randomUUID().toString().replace("-", "");

        var created = orders.create(request, idempotencyKey, manager, "order-smoke-123");
        var replay = orders.create(request, idempotencyKey, manager, "order-smoke-123");
        long orderId = ((Number) created.order().get("id")).longValue();
        assertThat(created.replayed()).isFalse();
        assertThat(replay.replayed()).isTrue();
        assertThat(((Number) replay.order().get("id")).longValue()).isEqualTo(orderId);

        orders.recordPayment(orderId, new PaymentRequest("CASH", new BigDecimal("10.50"),
                new BigDecimal("20.00"), null), manager, "order-smoke-123");
        Long ticketId = jdbc.queryForObject("SELECT id FROM kitchen_ticket WHERE order_id = ?", Long.class, orderId);
        orders.ticketAction(ticketId, "accept", new ActionRequest(0L), manager, "order-smoke-123");
        orders.ticketAction(ticketId, "start", new ActionRequest(1L), manager, "order-smoke-123");
        orders.ticketAction(ticketId, "bump", new ActionRequest(2L), manager, "order-smoke-123");
        assertThat(jdbc.queryForObject("SELECT status FROM order_header WHERE id = ?", String.class, orderId)).isEqualTo("READY");
        orders.ticketAction(ticketId, "serve", new ActionRequest(3L), manager, "order-smoke-123");
        long version = jdbc.queryForObject("SELECT version FROM order_header WHERE id = ?", Long.class, orderId);
        orders.complete(orderId, new ActionRequest(version), manager, "order-smoke-123");

        assertThat(jdbc.queryForObject("SELECT status FROM order_header WHERE id = ?", String.class, orderId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT status FROM kitchen_ticket WHERE id = ?", String.class, ticketId)).isEqualTo("SERVED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_record WHERE user_id = ? AND idempotency_key = ?",
                Integer.class, managerId, idempotencyKey)).isEqualTo(1);
    }

    @Test
    @Transactional
    void recallingReadyTicketIncrementsRecallCountAndReopensOrder() {
        Long managerId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'test-admin'", Long.class);
        int token = 80000 + Math.floorMod(UUID.randomUUID().hashCode(), 19999);
        Long orderId = jdbc.queryForObject("""
                INSERT INTO order_header (order_number, business_date, token, order_type, status, created_by)
                VALUES (?, ?, ?, 'TAKEAWAY', 'READY', ?) RETURNING id
                """, Long.class, "KF-TEST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), businessDays.currentBusinessDay(), token, managerId);
        Long ticketId = jdbc.queryForObject("""
                INSERT INTO kitchen_ticket (order_id, station_id, status, target_seconds, started_at, done_at)
                VALUES (?, 1, 'DONE', 600, now() - interval '3 minutes', now()) RETURNING id
                """, Long.class, orderId);
        ApiActor manager = new ApiActor(managerId, "test-admin", "MANAGER", 0);

        orders.ticketAction(ticketId, "recall", new ActionRequest(0L), manager, "recall-test-123");

        assertThat(jdbc.queryForObject("SELECT status FROM order_header WHERE id = ?", String.class, orderId)).isEqualTo("IN_KITCHEN");
        assertThat(jdbc.queryForObject("SELECT status FROM kitchen_ticket WHERE id = ?", String.class, ticketId)).isEqualTo("IN_PROGRESS");
        assertThat(jdbc.queryForObject("SELECT recall_count FROM kitchen_ticket WHERE id = ?", Integer.class, ticketId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT done_at FROM kitchen_ticket WHERE id = ?", Timestamp.class, ticketId)).isNull();
    }
}
