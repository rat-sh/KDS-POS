package com.kitchenflow.kds.order.service;

import com.kitchenflow.kds.common.security.ApiActor;
import com.kitchenflow.kds.order.api.OrderRequests.ActionRequest;
import com.kitchenflow.kds.order.api.OrderRequests.ManagerTicketActionRequest;
import com.kitchenflow.kds.order.api.OrderRequests.TicketRecord;
import com.kitchenflow.kds.order.api.OrderViews;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {
    private final OrderService operations;

    public TicketService(OrderService operations) { this.operations = operations; }

    @Transactional(readOnly = true)
    public List<OrderViews.TicketResponse> stationTickets(long stationId, ApiActor actor) {
        return operations.stationTickets(stationId, actor).stream().map(OrderViews::ticket).toList();
    }
    @Transactional(readOnly = true)
    public OrderViews.TicketResponse ticket(long id, ApiActor actor) { return OrderViews.ticket(operations.ticketForActor(id, actor)); }
    @Transactional(readOnly = true)
    public List<TicketRecord> records(long stationId, String state, String token, ApiActor actor) {
        return operations.stationTicketRecords(stationId, state, token, actor);
    }
    @Transactional
    public void action(long id, String action, ActionRequest request, ApiActor actor, String correlation) {
        operations.ticketAction(id, action, request, actor, correlation);
    }
    @Transactional
    public void managerAction(long id, ManagerTicketActionRequest request, ActionRequest action, ApiActor actor, String correlation) {
        operations.managerTicketAction(id, request.step().toUpperCase(), request.reason(), action, actor, correlation);
    }
}
