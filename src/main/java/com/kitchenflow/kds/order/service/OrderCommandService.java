package com.kitchenflow.kds.order.service;

import com.kitchenflow.kds.common.security.ApiActor;
import com.kitchenflow.kds.order.api.OrderRequests.ActionRequest;
import com.kitchenflow.kds.order.api.OrderRequests.CancelRequest;
import com.kitchenflow.kds.order.api.OrderRequests.CreateOrderRequest;
import com.kitchenflow.kds.order.api.OrderRequests.CreateOrderResult;
import com.kitchenflow.kds.order.api.OrderRequests.PriorityRequest;
import com.kitchenflow.kds.order.api.OrderRequests.RushRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCommandService {
    private final OrderService operations;

    public OrderCommandService(OrderService operations) { this.operations = operations; }

    @Transactional
    public CreateOrderResult create(CreateOrderRequest request, String key, ApiActor actor, String correlation) {
        return operations.create(request, key, actor, correlation);
    }
    @Transactional
    public void cancel(long id, CancelRequest request, ApiActor actor, String correlation) { operations.cancel(id, request, actor, correlation); }
    @Transactional
    public void complete(long id, ActionRequest request, ApiActor actor, String correlation) { operations.complete(id, request, actor, correlation); }
    @Transactional
    public void setPriority(long id, PriorityRequest request, ApiActor actor, String correlation) {
        operations.setPriority(id, Boolean.TRUE.equals(request.priority()), request.reason(), request.expectedVersion(), actor, correlation);
    }
    @Transactional
    public void setRush(long id, RushRequest request, ApiActor actor, String correlation) {
        operations.setRush(id, Boolean.TRUE.equals(request.rush()), request.expectedVersion(), actor, correlation);
    }
}
