package com.kitchenflow.kds.board;

import com.kitchenflow.kds.order.service.OrderService;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class BoardStream {
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final OrderService orders;

    public BoardStream(OrderService orders) {
        this.orders = orders;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        send(emitter);
        return emitter;
    }

    @Scheduled(fixedDelay = 3000)
    public void publishBoardSnapshot() {
        for (SseEmitter emitter : emitters) send(emitter);
    }

    private void send(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("board").data(orders.publicBoard()));
        } catch (IOException | IllegalStateException exception) {
            emitters.remove(emitter);
            emitter.completeWithError(exception);
        }
    }
}
