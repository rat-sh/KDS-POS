package com.kitchenflow.kds.api;

import com.kitchenflow.kds.order.api.OrderRequests.ActionRequest;
import com.kitchenflow.kds.order.api.OrderRequests.CancelRequest;
import com.kitchenflow.kds.order.api.OrderRequests.CreateOrderRequest;
import com.kitchenflow.kds.auth.api.AuthModels.LoginRequest;
import com.kitchenflow.kds.auth.api.AuthModels.LoginResponse;
import com.kitchenflow.kds.auth.api.AuthModels.LoginResult;
import com.kitchenflow.kds.menu.api.MenuItemRequest;
import com.kitchenflow.kds.menu.api.MenuAdminRequests.CategoryRequest;
import com.kitchenflow.kds.menu.api.MenuAdminRequests.ModifierGroupRequest;
import com.kitchenflow.kds.menu.api.MenuAdminRequests.ModifierRequest;
import com.kitchenflow.kds.menu.api.MenuAdminRequests.ItemStationsRequest;
import com.kitchenflow.kds.order.api.OrderRequests.ManagerTicketActionRequest;
import com.kitchenflow.kds.order.api.OrderRequests.PaymentRequest;
import com.kitchenflow.kds.order.api.OrderRequests.PriorityRequest;
import com.kitchenflow.kds.order.api.OrderRequests.RushRequest;
import com.kitchenflow.kds.order.api.OrderRequests.TicketRecord;
import com.kitchenflow.kds.order.api.OrderViews;
import com.kitchenflow.kds.config.api.ConfigRequests.SettingsRequest;
import com.kitchenflow.kds.config.api.ConfigRequests.StationPatch;
import com.kitchenflow.kds.config.api.ConfigRequests.StationCreateRequest;
import com.kitchenflow.kds.user.api.UserRequest;
import com.kitchenflow.kds.inventory.api.StockAdjustmentRequest;
import com.kitchenflow.kds.report.service.ReportService;
import com.kitchenflow.kds.board.BoardStream;
import com.kitchenflow.kds.common.config.KitchenFlowProperties;
import com.kitchenflow.kds.common.security.ApiActor;
import com.kitchenflow.kds.menu.service.CatalogService;
import com.kitchenflow.kds.order.service.OrderQueryService;
import com.kitchenflow.kds.order.service.OrderCommandService;
import com.kitchenflow.kds.order.service.TicketService;
import com.kitchenflow.kds.order.service.PaymentService;
import com.kitchenflow.kds.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
public class KitchenFlowController {
    private final AuthService auth;
    private final CatalogService catalog;
    private final OrderQueryService orderQueries;
    private final OrderCommandService orderCommands;
    private final TicketService tickets;
    private final PaymentService payments;
    private final BoardStream boardStream;
    private final KitchenFlowProperties properties;
    private final ReportService reports;

    public KitchenFlowController(AuthService auth, CatalogService catalog, OrderQueryService orderQueries,
            OrderCommandService orderCommands, TicketService tickets, PaymentService payments,
            BoardStream boardStream, KitchenFlowProperties properties, ReportService reports) {
        this.auth = auth;
        this.catalog = catalog;
        this.orderQueries = orderQueries;
        this.orderCommands = orderCommands;
        this.tickets = tickets;
        this.payments = payments;
        this.boardStream = boardStream;
        this.properties = properties;
        this.reports = reports;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(auth.login(request));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        return withRefreshCookie(auth.refresh(cookie(request, "kitchenflow_refresh")));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        auth.logout(cookie(request, "kitchenflow_refresh"));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString()).build();
    }

    @GetMapping("/auth/me")
    public com.kitchenflow.kds.auth.api.AuthModels.UserView currentUser(@AuthenticationPrincipal ApiActor actor) {
        return auth.view(actor.id());
    }

    @GetMapping("/menu")
    public Map<String, Object> menu(@RequestParam(defaultValue = "false") boolean includeUnavailable) {
        return catalog.menu(includeUnavailable);
    }

    @GetMapping("/stations")
    public List<Map<String, Object>> stations(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return catalog.stations(includeInactive);
    }

    @PostMapping("/admin/stations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createStation(@Valid @RequestBody StationCreateRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        long id = catalog.createStation(request, actor, correlation(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/admin/stations/" + id)).body(Map.of("id", id));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('CASHIER','KITCHEN','MANAGER','ADMIN')")
    public OrderViews.OrderPage orders(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size,
            @AuthenticationPrincipal ApiActor actor) {
        return orderQueries.orders(page, size, actor.role());
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Map<String, Object> reportSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int topPage,
            @RequestParam(defaultValue = "8") int topSize,
            @RequestParam(defaultValue = "0") int cancellationPage,
            @RequestParam(defaultValue = "20") int cancellationSize) {
        return reports.summary(from, to, topPage, topSize, cancellationPage, cancellationSize);
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER')")
    public OrderViews.OrderResponse order(@PathVariable long id) { return orderQueries.order(id); }

    @GetMapping("/orders/{id}/payment")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER')")
    public Map<String, Object> paymentDetails(@PathVariable long id) { return payments.payment(id); }

    @GetMapping("/orders/{id}/bill")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER')")
    public Map<String, Object> bill(@PathVariable long id) { return orderQueries.bill(id); }

    @GetMapping("/orders/{id}/tickets")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER')")
    public Map<String, Object> orderTickets(@PathVariable long id) {
        return Map.of("content", orderQueries.orderTickets(id));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        var created = orderCommands.create(request, idempotencyKey, actor, correlation(servletRequest));
        ResponseEntity.BodyBuilder response = ResponseEntity.created(URI.create("/api/v1/orders/" + created.order().get("id")));
        if (created.replayed()) response.header("Idempotency-Replayed", "true");
        return response.body(created.order());
    }

    @PostMapping("/orders/{id}/payments")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public ResponseEntity<Void> payment(@PathVariable long id, @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        payments.recordPayment(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable long id, @Valid @RequestBody CancelRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        orderCommands.cancel(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/orders/{id}/priority")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Void> priority(@PathVariable long id, @RequestBody PriorityRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        orderCommands.setPriority(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/orders/{id}/rush")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Void> rush(@PathVariable long id, @RequestBody RushRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        orderCommands.setRush(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{id}/complete")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Void> complete(@PathVariable long id, @RequestBody ActionRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        orderCommands.complete(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/refunds")
    @PreAuthorize("hasRole('MANAGER')")
    public Map<String, Object> refunds(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderQueries.refunds(page, size);
    }

    @PostMapping("/refunds/{id}/complete")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> completeRefund(@PathVariable long id, @RequestBody ActionRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        payments.completeRefund(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stations/{stationId}/tickets")
    @PreAuthorize("hasAnyRole('KITCHEN','MANAGER','ADMIN')")
    public List<OrderViews.TicketResponse> tickets(@PathVariable long stationId, @AuthenticationPrincipal ApiActor actor) {
        return tickets.stationTickets(stationId, actor);
    }

    @GetMapping("/tickets/{id}")
    @PreAuthorize("hasAnyRole('KITCHEN','MANAGER')")
    public OrderViews.TicketResponse ticket(@PathVariable long id, @AuthenticationPrincipal ApiActor actor) {
        return tickets.ticket(id, actor);
    }

    @GetMapping("/stations/{stationId}/records")
    @PreAuthorize("hasAnyRole('KITCHEN','MANAGER','ADMIN')")
    public ResponseEntity<?> ticketRecords(@PathVariable long stationId,
            @RequestParam(defaultValue = "ALL") String state, @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "json") String format, @AuthenticationPrincipal ApiActor actor) {
        List<TicketRecord> records = tickets.records(stationId, state, token, actor);
        if (format.equalsIgnoreCase("csv")) {
            String csv = "ticketId,orderId,orderNumber,token,stationId,state,acceptedAt,acceptedBy,startedAt,doneAt,servedAt,servedBy,acceptToStartSeconds,cookSeconds,pickupSeconds,recallCount\n"
                    + records.stream().map(KitchenFlowController::csvRecord).collect(java.util.stream.Collectors.joining("\n"));
            return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=station-" + stationId + "-records.csv")
                    .body(csv);
        }
        if (!format.equalsIgnoreCase("json")) {
            throw new com.kitchenflow.kds.common.error.ApiFailure(400, "INVALID_FORMAT", "Format must be json or csv.");
        }
        return ResponseEntity.ok(Map.of("content", records));
    }

    private static String csvRecord(TicketRecord record) {
        return java.util.stream.Stream.of(record.ticketId(), record.orderId(), record.orderNumber(), record.token(),
                record.stationId(), record.state(), record.acceptedAt(), record.acceptedByName(), record.startedAt(),
                record.doneAt(), record.servedAt(), record.servedByName(), record.acceptToStartSeconds(),
                record.cookSeconds(), record.pickupSeconds(), record.recallCount())
                .map(value -> csvCell(value == null ? "" : value.toString()))
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String csvCell(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @PostMapping("/tickets/{id}/accept")
    @PreAuthorize("hasAnyRole('KITCHEN','MANAGER','ADMIN')")
    public ResponseEntity<Void> accept(@PathVariable long id, @RequestBody ActionRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        tickets.action(id, "accept", request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tickets/{id}/start")
    @PreAuthorize("hasAnyRole('KITCHEN','MANAGER','ADMIN')")
    public ResponseEntity<Void> start(@PathVariable long id, @RequestBody ActionRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        tickets.action(id, "start", request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tickets/{id}/bump")
    @PreAuthorize("hasAnyRole('KITCHEN','MANAGER','ADMIN')")
    public ResponseEntity<Void> bump(@PathVariable long id, @RequestBody ActionRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        tickets.action(id, "bump", request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tickets/{id}/serve")
    @PreAuthorize("hasAnyRole('KITCHEN','MANAGER','ADMIN')")
    public ResponseEntity<Void> serve(@PathVariable long id, @RequestBody ActionRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        tickets.action(id, "serve", request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tickets/{id}/recall")
    @PreAuthorize("hasAnyRole('KITCHEN','MANAGER','ADMIN')")
    public ResponseEntity<Void> recall(@PathVariable long id, @RequestBody ActionRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        tickets.action(id, "recall", request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tickets/{id}/manager-action")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Void> managerAction(@PathVariable long id, @Valid @RequestBody ManagerTicketActionRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        tickets.managerAction(id, request, new ActionRequest(request.expectedVersion()), actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tickets/{id}/acknowledge-cancel")
    @PreAuthorize("hasAnyRole('KITCHEN','MANAGER','ADMIN')")
    public ResponseEntity<Void> acknowledgeCancel(@PathVariable long id, @RequestBody ActionRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        tickets.action(id, "acknowledge-cancel", request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/items/{id}/availability")
    @PreAuthorize("hasAnyRole('KITCHEN','MANAGER','ADMIN')")
    public ResponseEntity<Void> availability(@PathVariable long id, @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        catalog.setAvailability(id, Boolean.TRUE.equals(body.get("available")),
                body.get("expectedVersion") instanceof Number version ? version.longValue() : null,
                actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> categories() { return catalog.categories(); }

    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createCategory(@Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        long id = catalog.createCategory(request, actor, correlation(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/admin/categories/" + id)).body(Map.of("id", id));
    }

    @PatchMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateCategory(@PathVariable long id, @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        catalog.updateCategory(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/items/{id}/stations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> itemStations(@PathVariable long id, @Valid @RequestBody ItemStationsRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        catalog.replaceItemStations(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/items/{id}/modifier-groups")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createModifierGroup(@PathVariable long id,
            @Valid @RequestBody ModifierGroupRequest request, @AuthenticationPrincipal ApiActor actor,
            HttpServletRequest servletRequest) {
        long groupId = catalog.createItemModifierGroup(id, request, actor, correlation(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/admin/modifier-groups/" + groupId)).body(Map.of("id", groupId));
    }

    @PatchMapping("/admin/modifier-groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateModifierGroup(@PathVariable long id,
            @Valid @RequestBody ModifierGroupRequest request, @AuthenticationPrincipal ApiActor actor,
            HttpServletRequest servletRequest) {
        catalog.updateModifierGroup(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/modifier-groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModifierGroup(@PathVariable long id, @AuthenticationPrincipal ApiActor actor,
            HttpServletRequest servletRequest) {
        catalog.deleteModifierGroup(id, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/modifier-groups/{id}/modifiers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createModifier(@PathVariable long id,
            @Valid @RequestBody ModifierRequest request, @AuthenticationPrincipal ApiActor actor,
            HttpServletRequest servletRequest) {
        long modifierId = catalog.createModifier(id, request, actor, correlation(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/admin/modifiers/" + modifierId)).body(Map.of("id", modifierId));
    }

    @PatchMapping("/admin/modifiers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateModifier(@PathVariable long id, @Valid @RequestBody ModifierRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        catalog.updateModifier(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/modifiers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModifier(@PathVariable long id, @AuthenticationPrincipal ApiActor actor,
            HttpServletRequest servletRequest) {
        catalog.deleteModifier(id, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Map<String, Object> stock() { return Map.of("content", catalog.stockInventory()); }

    @PostMapping("/items/{id}/stock-adjustments")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Map<String, Object>> adjustStock(@PathVariable long id,
            @Valid @RequestBody StockAdjustmentRequest request, @AuthenticationPrincipal ApiActor actor,
            HttpServletRequest servletRequest) {
        Map<String, Object> adjustment = catalog.adjustStock(id, request, actor, correlation(servletRequest));
        return ResponseEntity.status(201).body(adjustment);
    }

    @GetMapping("/items/{id}/stock-adjustments")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Map<String, Object> stockAdjustments(@PathVariable long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Map.of("content", catalog.stockAdjustments(id, page, size));
    }

    @GetMapping("/admin/items")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> items() { return catalog.itemList(); }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> users(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        List<Map<String, Object>> all = catalog.users();
        int from = Math.min(Math.max(page, 0) * Math.clamp(size, 1, 200), all.size());
        int to = Math.min(from + Math.clamp(size, 1, 200), all.size());
        return Map.of("content", all.subList(from, to), "totalElements", all.size(), "totalPages", (int) Math.ceil((double) all.size() / Math.clamp(size, 1, 200)));
    }

    @GetMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> user(@PathVariable long id) { return catalog.user(id); }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody UserRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        long id = catalog.saveUser(null, request, actor, correlation(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/admin/users/" + id)).body(Map.of("id", id));
    }

    @PatchMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUser(@PathVariable long id, @Valid @RequestBody UserRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        catalog.saveUser(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> itemAdmin(@PathVariable long id) { return catalog.item(id); }

    @PostMapping("/admin/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createItem(@Valid @RequestBody MenuItemRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        long id = catalog.saveMenuItem(null, request, actor, correlation(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/admin/items/" + id)).body(Map.of("id", id));
    }

    @PatchMapping("/admin/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateItem(@PathVariable long id, @Valid @RequestBody MenuItemRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        catalog.saveMenuItem(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable long id, @AuthenticationPrincipal ApiActor actor,
            HttpServletRequest servletRequest) {
        catalog.archiveMenuItem(id, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/stations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStation(@PathVariable long id, @RequestBody StationPatch request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        catalog.updateStation(id, request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> config() { return catalog.settings(); }

    @GetMapping("/config/public")
    public Map<String, Object> publicConfig() { return Map.of("outletName", catalog.settings().get("outletName")); }

    @PatchMapping("/admin/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateConfig(@Valid @RequestBody SettingsRequest request,
            @AuthenticationPrincipal ApiActor actor, HttpServletRequest servletRequest) {
        catalog.updateSettings(request, actor, correlation(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Map<String, Object> audit(@RequestParam(defaultValue = "100") int size) {
        return Map.of("content", catalog.audit(size));
    }

    @GetMapping("/board")
    public Map<String, Object> board() { return orderQueries.publicBoard(); }

    @GetMapping("/stream/board")
    public SseEmitter boardStream() { return boardStream.subscribe(); }

    private ResponseEntity<LoginResponse> withRefreshCookie(LoginResult result) {
        ResponseCookie cookie = ResponseCookie.from("kitchenflow_refresh", result.refreshToken())
                .httpOnly(true).secure(properties.refreshCookieSecure()).sameSite("Strict")
                .path("/api/v1/auth").maxAge(java.time.Duration.ofDays(properties.refreshTokenDays())).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result.response());
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from("kitchenflow_refresh", "").httpOnly(true).secure(properties.refreshCookieSecure())
                .sameSite("Strict").path("/api/v1/auth").maxAge(0).build();
    }

    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies()).filter(cookie -> cookie.getName().equals(name))
                .map(jakarta.servlet.http.Cookie::getValue).findFirst().orElse(null);
    }

    private String correlation(HttpServletRequest request) { return (String) request.getAttribute("correlationId"); }
}
