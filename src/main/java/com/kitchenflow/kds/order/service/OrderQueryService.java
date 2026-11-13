package com.kitchenflow.kds.order.service;

import com.kitchenflow.kds.order.api.OrderViews;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {
    private final OrderService operations;

    public OrderQueryService(OrderService operations) { this.operations = operations; }

    @Transactional(readOnly = true)
    public OrderViews.OrderPage orders(int page, int size, String role) {
        return OrderViews.page(operations.orders(page, size, role), role.equals("KITCHEN"));
    }
    @Transactional(readOnly = true)
    public OrderViews.OrderResponse order(long id) { return OrderViews.order(operations.order(id)); }
    @Transactional(readOnly = true)
    public Map<String, Object> bill(long id) { return operations.bill(id); }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> orderTickets(long id) { return operations.orderTickets(id); }
    @Transactional(readOnly = true)
    public Map<String, Object> refunds(int page, int size) { return Map.of("content", operations.refunds(page, size)); }
    public Map<String, Object> publicBoard() { return operations.publicBoard(); }
}
