package com.kitchenflow.kds.menu.service;

import com.kitchenflow.kds.common.error.ApiFailure;
import com.kitchenflow.kds.menu.api.MenuItemRequest;
import com.kitchenflow.kds.menu.api.MenuAdminRequests.CategoryRequest;
import com.kitchenflow.kds.menu.api.MenuAdminRequests.ModifierGroupRequest;
import com.kitchenflow.kds.menu.api.MenuAdminRequests.ModifierRequest;
import com.kitchenflow.kds.menu.api.MenuAdminRequests.ItemStationsRequest;
import com.kitchenflow.kds.config.api.ConfigRequests.SettingsRequest;
import com.kitchenflow.kds.config.api.ConfigRequests.StationPatch;
import com.kitchenflow.kds.config.api.ConfigRequests.StationCreateRequest;
import com.kitchenflow.kds.user.api.UserRequest;
import com.kitchenflow.kds.inventory.api.StockAdjustmentRequest;
import com.kitchenflow.kds.common.security.ApiActor;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;

    public CatalogService(JdbcTemplate jdbc, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.passwords = passwords;
    }

    public Map<String, Object> menu(boolean includeUnavailable) {
        List<Map<String, Object>> categories = jdbc.query(
                "SELECT id, name, gst_rate FROM menu_category WHERE active = true ORDER BY id", (rs, row) -> {
                    long id = rs.getLong("id");
                    List<Map<String, Object>> items = jdbc.query(
                            "SELECT * FROM menu_item WHERE category_id = ? AND archived = false " + (includeUnavailable ? "" : "AND available = true ") + "ORDER BY name",
                            (itemRs, itemRow) -> menuItem(itemRs), id);
                    return map("id", id, "name", rs.getString("name"), "gstRate", rs.getBigDecimal("gst_rate").toPlainString(), "items", items);
                });
        return map("menuVersion", 1, "categories", categories);
    }

    public List<Map<String, Object>> categories() {
        return jdbc.query("SELECT id, name, gst_rate, active FROM menu_category ORDER BY name",
                (rs, row) -> map("id", rs.getLong("id"), "name", rs.getString("name"),
                        "gstRate", rs.getBigDecimal("gst_rate").toPlainString(), "active", rs.getBoolean("active")));
    }

    @Transactional
    public long createCategory(CategoryRequest request, ApiActor actor, String correlationId) {
        if (request.name() == null || request.name().isBlank()) throw new ApiFailure(422, "VALIDATION_FAILED", "Category name is required.", Map.of("field", "name", "reason", "REQUIRED"));
        BigDecimal gstRate = request.gstRate() == null ? BigDecimal.ZERO : request.gstRate();
        if (gstRate.signum() < 0 || gstRate.compareTo(BigDecimal.valueOf(100)) > 0) throw new ApiFailure(422, "VALIDATION_FAILED", "GST rate must be between 0 and 100.", Map.of("field", "gstRate", "reason", "OUT_OF_RANGE"));
        Long id = jdbc.queryForObject("INSERT INTO menu_category (name, gst_rate, active) VALUES (?, ?, coalesce(?, true)) RETURNING id",
                Long.class, request.name().trim(), gstRate, request.active());
        audit(actor, "MENU_CATEGORY", id, "CATEGORY_CREATED", null, correlationId);
        return id;
    }

    @Transactional
    public void updateCategory(long id, CategoryRequest request, ApiActor actor, String correlationId) {
        requireFound("SELECT count(*) FROM menu_category WHERE id = ?", id, "Category");
        if (request.name() != null && request.name().isBlank()) throw new ApiFailure(422, "VALIDATION_FAILED", "Category name cannot be blank.", Map.of("field", "name", "reason", "INVALID_FORMAT"));
        if (request.gstRate() != null && (request.gstRate().signum() < 0 || request.gstRate().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new ApiFailure(422, "VALIDATION_FAILED", "GST rate must be between 0 and 100.", Map.of("field", "gstRate", "reason", "OUT_OF_RANGE"));
        }
        jdbc.update("UPDATE menu_category SET name = coalesce(?, name), gst_rate = coalesce(?, gst_rate), active = coalesce(?, active) WHERE id = ?",
                blankToNull(request.name()), request.gstRate(), request.active(), id);
        audit(actor, "MENU_CATEGORY", id, "CATEGORY_UPDATED", null, correlationId);
    }

    public List<Map<String, Object>> modifierGroups() {
        return jdbc.query("SELECT id, name, selection_type, min_select, max_select FROM modifier_group ORDER BY name",
                (rs, row) -> map("id", rs.getLong("id"), "name", rs.getString("name"),
                        "selectionType", rs.getString("selection_type"), "minSelect", rs.getInt("min_select"),
                        "maxSelect", rs.getInt("max_select")));
    }

    @Transactional
    public long createModifierGroup(ModifierGroupRequest request, ApiActor actor, String correlationId) {
        if (request.name() == null || request.name().isBlank() || request.selectionType() == null || request.selectionType().isBlank()) {
            throw new ApiFailure(422, "VALIDATION_FAILED", "Modifier group name and selection type are required.", Map.of("reason", "REQUIRED"));
        }
        String selection = request.selectionType().trim().toUpperCase();
        int minimum = request.minSelect() == null ? 0 : request.minSelect();
        int maximum = request.maxSelect() == null ? 1 : request.maxSelect();
        validateModifierLimits(selection, minimum, maximum);
        Long id = jdbc.queryForObject("INSERT INTO modifier_group (name, selection_type, min_select, max_select) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, request.name().trim(), selection, minimum, maximum);
        audit(actor, "MODIFIER_GROUP", id, "MODIFIER_GROUP_CREATED", null, correlationId);
        return id;
    }

    @Transactional
    public long createItemModifierGroup(long itemId, ModifierGroupRequest request, ApiActor actor, String correlationId) {
        requireFound("SELECT count(*) FROM menu_item WHERE id = ? AND archived = false", itemId, "Menu item");
        long groupId = createModifierGroup(request, actor, correlationId);
        jdbc.update("INSERT INTO item_modifier_group (item_id, group_id) VALUES (?, ?)", itemId, groupId);
        audit(actor, "MENU_ITEM", itemId, "MODIFIER_GROUP_ASSIGNED", request.name().trim(), correlationId);
        return groupId;
    }

    @Transactional
    public void updateModifierGroup(long id, ModifierGroupRequest request, ApiActor actor, String correlationId) {
        var rows = jdbc.query("SELECT name, selection_type, min_select, max_select FROM modifier_group WHERE id = ? FOR UPDATE",
                (rs, row) -> new GroupState(rs.getString("name"), rs.getString("selection_type"), rs.getInt("min_select"), rs.getInt("max_select")), id);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Modifier group not found.");
        GroupState current = rows.getFirst();
        String name = request.name() == null ? current.name() : request.name().trim();
        String selection = request.selectionType() == null ? current.selectionType() : request.selectionType().trim().toUpperCase();
        int minimum = request.minSelect() == null ? current.minSelect() : request.minSelect();
        int maximum = request.maxSelect() == null ? current.maxSelect() : request.maxSelect();
        validateModifierLimits(selection, minimum, maximum);
        jdbc.update("UPDATE modifier_group SET name = ?, selection_type = ?, min_select = ?, max_select = ? WHERE id = ?",
                name, selection, minimum, maximum, id);
        audit(actor, "MODIFIER_GROUP", id, "MODIFIER_GROUP_UPDATED", null, correlationId);
    }

    @Transactional
    public void deleteModifierGroup(long id, ApiActor actor, String correlationId) {
        requireFound("SELECT count(*) FROM modifier_group WHERE id = ?", id, "Modifier group");
        Integer references = jdbc.queryForObject("SELECT count(*) FROM item_modifier_group WHERE group_id = ?", Integer.class, id);
        if (references != null && references > 0) throw new ApiFailure(409, "RESOURCE_IN_USE", "This modifier group is assigned to menu items.", Map.of("referenceCount", references));
        jdbc.update("DELETE FROM modifier_group WHERE id = ?", id);
        audit(actor, "MODIFIER_GROUP", id, "MODIFIER_GROUP_DELETED", null, correlationId);
    }

    @Transactional
    public long createModifier(long groupId, ModifierRequest request, ApiActor actor, String correlationId) {
        requireFound("SELECT count(*) FROM modifier_group WHERE id = ?", groupId, "Modifier group");
        if (request.name() == null || request.name().isBlank()) throw new ApiFailure(422, "VALIDATION_FAILED", "Modifier name is required.", Map.of("reason", "REQUIRED"));
        Long id = jdbc.queryForObject("INSERT INTO menu_modifier (group_id, name, price_delta, active) VALUES (?, ?, ?, coalesce(?, true)) RETURNING id",
                Long.class, groupId, request.name().trim(), request.priceDelta() == null ? BigDecimal.ZERO : request.priceDelta(), request.active());
        audit(actor, "MODIFIER", id, "MODIFIER_CREATED", null, correlationId);
        return id;
    }

    @Transactional
    public void updateModifier(long id, ModifierRequest request, ApiActor actor, String correlationId) {
        requireFound("SELECT count(*) FROM menu_modifier WHERE id = ?", id, "Modifier");
        jdbc.update("UPDATE menu_modifier SET name = coalesce(?, name), price_delta = coalesce(?, price_delta), active = coalesce(?, active) WHERE id = ?",
                blankToNull(request.name()), request.priceDelta(), request.active(), id);
        audit(actor, "MODIFIER", id, "MODIFIER_UPDATED", null, correlationId);
    }

    @Transactional
    public void deleteModifier(long id, ApiActor actor, String correlationId) {
        requireFound("SELECT count(*) FROM menu_modifier WHERE id = ?", id, "Modifier");
        Integer references = jdbc.queryForObject("SELECT count(*) FROM order_line_modifier WHERE modifier_id = ?", Integer.class, id);
        if (references != null && references > 0) throw new ApiFailure(409, "RESOURCE_IN_USE", "This modifier is used by an order.", Map.of("referenceCount", references));
        jdbc.update("DELETE FROM menu_modifier WHERE id = ?", id);
        audit(actor, "MODIFIER", id, "MODIFIER_DELETED", null, correlationId);
    }

    private void validateModifierLimits(String selection, int minimum, int maximum) {
        if (!List.of("SINGLE", "MULTIPLE").contains(selection) || minimum < 0 || maximum < minimum || maximum > 20) {
            throw new ApiFailure(422, "VALIDATION_FAILED", "Modifier group selection limits are invalid.", Map.of("reason", "INVALID_SELECTION_LIMITS"));
        }
    }

    @Transactional
    public void replaceItemStations(long itemId, ItemStationsRequest request, ApiActor actor, String correlationId) {
        var items = jdbc.query("SELECT version FROM menu_item WHERE id = ? AND archived = false FOR UPDATE",
                (rs, row) -> rs.getLong("version"), itemId);
        if (items.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Menu item not found.");
        checkVersion(request.expectedVersion(), items.getFirst());
        List<Long> stationIds = request.stationIds().stream().distinct().toList();
        if (stationIds.isEmpty() || stationIds.size() > 10) {
            throw new ApiFailure(422, "VALIDATION_FAILED", "Select between one and ten active stations.", Map.of("reason", "INVALID_STATIONS"));
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(stationIds.size(), "?"));
        Integer activeCount = jdbc.queryForObject("SELECT count(*) FROM station WHERE active = true AND id IN (" + placeholders + ")",
                Integer.class, stationIds.toArray());
        if (activeCount == null || activeCount != stationIds.size()) {
            throw new ApiFailure(422, "VALIDATION_FAILED", "Select between one and ten active stations.", Map.of("reason", "INVALID_STATIONS"));
        }
        jdbc.update("DELETE FROM menu_item_station WHERE item_id = ?", itemId);
        for (long stationId : stationIds) jdbc.update("INSERT INTO menu_item_station (item_id, station_id) VALUES (?, ?)", itemId, stationId);
        jdbc.update("UPDATE menu_item SET version = version + 1 WHERE id = ?", itemId);
        audit(actor, "MENU_ITEM", itemId, "ITEM_STATIONS_UPDATED", null, correlationId);
    }

    public List<Map<String, Object>> stations(boolean includeInactive) {
        return jdbc.query("SELECT id, name, display_order, active FROM station " + (includeInactive ? "" : "WHERE active = true ") + "ORDER BY display_order, id",
                (rs, row) -> map("id", rs.getLong("id"), "name", rs.getString("name"),
                        "displayOrder", rs.getInt("display_order"), "active", rs.getBoolean("active")));
    }

    public Map<String, Object> settings() {
        return jdbc.queryForMap("SELECT outlet_name AS \"outletName\", gstin, amber_percent AS \"amberPercent\", red_percent AS \"redPercent\", version FROM outlet_config WHERE id = 1");
    }

    @Transactional
    public void updateSettings(SettingsRequest request, ApiActor actor, String correlationId) {
        if (request.redPercent() <= request.amberPercent()) throw new ApiFailure(422, "VALIDATION_FAILED", "The red threshold must be greater than the amber threshold.", Map.of("reason", "INVALID_SLA_THRESHOLDS"));
        jdbc.update("UPDATE outlet_config SET outlet_name = ?, gstin = ?, amber_percent = ?, red_percent = ?, version = version + 1 WHERE id = 1",
                request.outletName().trim(), request.gstin() == null ? "" : request.gstin().trim().toUpperCase(), request.amberPercent(), request.redPercent());
        audit(actor, "OUTLET_CONFIG", 1, "OUTLET_SETTINGS_UPDATED", null, correlationId);
    }

    @Transactional
    public void updateStation(long id, StationPatch patch, ApiActor actor, String correlationId) {
        requireFound("SELECT count(*) FROM station WHERE id = ?", id, "Station");
        jdbc.update("UPDATE station SET name = coalesce(?, name), display_order = coalesce(?, display_order), active = coalesce(?, active), version = version + 1 WHERE id = ?",
                blankToNull(patch.name()), patch.displayOrder(), patch.active(), id);
        audit(actor, "STATION", id, "STATION_UPDATED", null, correlationId);
    }

    @Transactional
    public long createStation(StationCreateRequest request, ApiActor actor, String correlationId) {
        Long id = jdbc.queryForObject("INSERT INTO station (name, display_order) VALUES (?, ?) RETURNING id",
                Long.class, request.name().trim(), request.displayOrder());
        audit(actor, "STATION", id, "STATION_CREATED", null, correlationId);
        return id;
    }

    @Transactional
    public long saveMenuItem(Long id, MenuItemRequest request, ApiActor actor, String correlationId) {
        List<Long> categoryIds = jdbc.queryForList(
                "SELECT id FROM menu_category WHERE name = ? AND active = true", Long.class, request.category());
        if (categoryIds.isEmpty()) throw new ApiFailure(422, "VALIDATION_FAILED",
                "Category not found. Create it in Admin → Categories first.",
                Map.of("field", "category", "reason", "NOT_FOUND"));
        long categoryId = categoryIds.getFirst();
        Long itemId = id;
        if (id == null) {
            itemId = jdbc.queryForObject("""
                    INSERT INTO menu_item (category_id, name, price, available, track_stock, stock_quantity, target_prep_seconds)
                    VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, categoryId, request.name().trim(), request.price(), request.available(),
                    request.stockTracking(), request.stockTracking() ? request.stock() : null, request.prepSeconds());
        } else {
            requireFound("SELECT count(*) FROM menu_item WHERE id = ? AND archived = false", id, "Menu item");
            jdbc.update("UPDATE menu_item SET category_id = ?, name = ?, price = ?, available = ?, track_stock = ?, stock_quantity = ?, target_prep_seconds = ?, version = version + 1 WHERE id = ?",
                    categoryId, request.name().trim(), request.price(), request.available(), request.stockTracking(),
                    request.stockTracking() ? request.stock() : null, request.prepSeconds(), id);
        }
        jdbc.update("DELETE FROM menu_item_station WHERE item_id = ?", itemId);
        for (String station : request.stations()) {
            jdbc.update("INSERT INTO menu_item_station (item_id, station_id) SELECT ?, id FROM station WHERE lower(name) = lower(?) AND active = true ON CONFLICT DO NOTHING", itemId, station);
        }
        Integer stationCount = jdbc.queryForObject("SELECT count(*) FROM menu_item_station WHERE item_id = ?", Integer.class, itemId);
        if (stationCount == null || stationCount == 0) throw new ApiFailure(422, "VALIDATION_FAILED", "Choose at least one active station.", Map.of("reason", "STATION_REQUIRED"));
        jdbc.update("DELETE FROM item_modifier_group WHERE item_id = ?", itemId);
        if (request.modifierGroups() != null) {
            for (Long groupId : request.modifierGroups()) jdbc.update("INSERT INTO item_modifier_group (item_id, group_id) VALUES (?, ?) ON CONFLICT DO NOTHING", itemId, groupId);
        }
        audit(actor, "MENU_ITEM", itemId, id == null ? "MENU_ITEM_CREATED" : "MENU_ITEM_UPDATED", null, correlationId);
        return itemId;
    }

    @Transactional
    public void setAvailability(long itemId, boolean available, Long expectedVersion, ApiActor actor, String correlationId) {
        var rows = jdbc.query("SELECT version, track_stock, stock_quantity FROM menu_item WHERE id = ? AND archived = false FOR UPDATE",
                (rs, row) -> new ItemAvailability(rs.getLong("version"), rs.getBoolean("track_stock"), rs.getObject("stock_quantity", Integer.class)), itemId);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Menu item not found.");
        ItemAvailability item = rows.getFirst();
        checkVersion(expectedVersion, item.version());
        if (available && item.trackStock() && item.stockQuantity() != null && item.stockQuantity() == 0) {
            throw new ApiFailure(409, "OUT_OF_STOCK", "An item with no stock cannot be marked available.");
        }
        jdbc.update("UPDATE menu_item SET available = ?, version = version + 1 WHERE id = ?", available, itemId);
        audit(actor, "MENU_ITEM", itemId, "AVAILABILITY_UPDATED", available ? "Available" : "Unavailable", correlationId);
    }

    public List<Map<String, Object>> stockInventory() {
        return jdbc.query("""
                SELECT i.id, i.name, i.stock_quantity, i.available, i.version
                FROM menu_item i WHERE i.archived = false AND i.track_stock = true ORDER BY i.name
                """, (rs, row) -> map("itemId", rs.getLong("id"), "name", rs.getString("name"),
                "quantity", rs.getObject("stock_quantity"), "available", rs.getBoolean("available"),
                "version", rs.getLong("version")));
    }

    @Transactional
    public Map<String, Object> adjustStock(long itemId, StockAdjustmentRequest request, ApiActor actor, String correlationId) {
        if (request.quantityDelta() == 0) {
            throw new ApiFailure(422, "VALIDATION_FAILED", "Stock adjustment cannot be zero.", Map.of("field", "quantityDelta", "reason", "OUT_OF_RANGE"));
        }
        var rows = jdbc.query("SELECT name, track_stock, stock_quantity, version FROM menu_item WHERE id = ? AND archived = false FOR UPDATE",
                (rs, row) -> new StockState(rs.getString("name"), rs.getBoolean("track_stock"),
                        rs.getObject("stock_quantity", Integer.class), rs.getLong("version")), itemId);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Menu item not found.");
        StockState item = rows.getFirst();
        checkVersion(request.expectedVersion(), item.version());
        if (!item.trackStock() || item.quantity() == null) {
            throw new ApiFailure(409, "INVALID_TRANSITION", "Stock tracking is not enabled for this item.", Map.of("reason", "NOT_TRACKED"));
        }
        int next = item.quantity() + request.quantityDelta();
        if (next < 0) throw new ApiFailure(409, "OUT_OF_STOCK", "The adjustment would make stock negative.", Map.of("available", item.quantity()));
        jdbc.update("UPDATE menu_item SET stock_quantity = ?, available = ?, version = version + 1 WHERE id = ?",
                next, next > 0, itemId);
        jdbc.update("INSERT INTO stock_adjustment (item_id, quantity_delta, stock_after, reason, adjusted_by) VALUES (?, ?, ?, ?, ?)",
                itemId, request.quantityDelta(), next, request.reason().trim(), actor.id());
        audit(actor, "STOCK", itemId, "STOCK_ADJUSTED", request.reason().trim(), correlationId);
        return map("itemId", itemId, "name", item.name(), "quantityDelta", request.quantityDelta(),
                "stockAfter", next, "version", item.version() + 1);
    }

    public List<Map<String, Object>> stockAdjustments(long itemId, int page, int size) {
        requireFound("SELECT count(*) FROM menu_item WHERE id = ?", itemId, "Menu item");
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, 100);
        return jdbc.query("""
                SELECT a.id, a.quantity_delta, a.stock_after, a.reason, a.created_at, a.adjusted_by,
                       u.display_name AS adjusted_by_name
                FROM stock_adjustment a JOIN app_user u ON u.id = a.adjusted_by
                WHERE a.item_id = ? ORDER BY a.created_at DESC, a.id DESC LIMIT ? OFFSET ?
                """, (rs, row) -> map("id", rs.getLong("id"), "quantityDelta", rs.getInt("quantity_delta"),
                "stockAfter", rs.getInt("stock_after"), "reason", rs.getString("reason"),
                "createdAt", rs.getObject("created_at"), "adjustedBy", rs.getLong("adjusted_by"),
                "adjustedByName", rs.getString("adjusted_by_name")), itemId, safeSize, (long) safePage * safeSize);
    }

    public List<Map<String, Object>> users() {
        return jdbc.query("SELECT id, username, role, active, version FROM app_user ORDER BY username", (rs, row) -> {
            long id = rs.getLong("id");
            return map("id", id, "username", rs.getString("username"), "role", rs.getString("role"),
                    "active", rs.getBoolean("active"), "version", rs.getLong("version"),
                    "stationIds", jdbc.queryForList("SELECT station_id FROM user_station WHERE user_id = ? ORDER BY station_id", Long.class, id));
        });
    }

    @Transactional
    public long saveUser(Long existingUserId, UserRequest request, ApiActor actor, String correlationId) {
        String role = request.role().toUpperCase();
        if (!List.of("CASHIER", "KITCHEN", "MANAGER", "ADMIN").contains(role)) {
            throw new ApiFailure(422, "VALIDATION_FAILED", "Choose a valid staff role.", Map.of("field", "role", "reason", "INVALID_ENUM"));
        }
        long userId;
        if (existingUserId == null) {
            if (request.password() == null || request.password().length() < 12) {
                throw new ApiFailure(422, "VALIDATION_FAILED", "New staff accounts require a password of at least 12 characters.", Map.of("field", "password", "reason", "REQUIRED"));
            }
            userId = jdbc.queryForObject("INSERT INTO app_user (username, display_name, password_hash, role) VALUES (?, ?, ?, ?) RETURNING id",
                    Long.class, request.username().trim(), request.username().trim(), passwords.encode(request.password()), role);
        } else {
            var userIds = jdbc.query("SELECT id FROM app_user WHERE id = ? FOR UPDATE", (rs, row) -> rs.getLong(1), existingUserId);
            if (userIds.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Staff account not found.");
            userId = userIds.getFirst();
            if (request.password() != null && !request.password().isBlank()) {
                if (request.password().length() < 12) throw new ApiFailure(422, "VALIDATION_FAILED", "Passwords must contain at least 12 characters.", Map.of("field", "password", "reason", "TOO_SHORT"));
                jdbc.update("UPDATE app_user SET role = ?, active = ?, password_hash = ?, version = version + 1 WHERE id = ?",
                        role, request.active() == null || request.active(), passwords.encode(request.password()), userId);
            } else {
                jdbc.update("UPDATE app_user SET role = ?, active = ?, version = version + 1 WHERE id = ?",
                        role, request.active() == null || request.active(), userId);
            }
            jdbc.update("UPDATE refresh_token SET revoked_at = now() WHERE user_id = ? AND revoked_at IS NULL", userId);
        }
        jdbc.update("DELETE FROM user_station WHERE user_id = ?", userId);
        if (role.equals("KITCHEN") && request.stations() != null) {
            for (String station : request.stations()) jdbc.update("INSERT INTO user_station (user_id, station_id) SELECT ?, id FROM station WHERE lower(name) = lower(?) ON CONFLICT DO NOTHING", userId, station);
        }
        audit(actor, "USER", userId, existingUserId == null ? "USER_CREATED" : "USER_UPDATED", role, correlationId);
        return userId;
    }

    public Map<String, Object> user(long id) {
        return jdbc.query("SELECT id, username, role, active, version FROM app_user WHERE id = ?", (rs, row) -> {
            long userId = rs.getLong("id");
            return map("id", userId, "username", rs.getString("username"), "role", rs.getString("role"),
                    "active", rs.getBoolean("active"), "version", rs.getLong("version"),
                    "stationIds", jdbc.queryForList("SELECT station_id FROM user_station WHERE user_id = ? ORDER BY station_id", Long.class, userId));
        }, id).stream().findFirst().orElseThrow(() -> new ApiFailure(404, "NOT_FOUND", "Staff account not found."));
    }

    public List<Map<String, Object>> audit(int limit) {
        return jdbc.query("SELECT id, occurred_at, actor_id, actor_role, entity_type, entity_id, action, correlation_id, reason FROM audit_entry ORDER BY occurred_at DESC, id DESC LIMIT ?",
                (rs, row) -> map("id", rs.getLong("id"), "occurredAt", rs.getObject("occurred_at"),
                        "actorId", rs.getObject("actor_id"), "actorRole", rs.getString("actor_role"),
                        "entityType", rs.getString("entity_type"), "entityId", rs.getLong("entity_id"),
                        "action", rs.getString("action"), "correlationId", rs.getString("correlation_id"),
                        "reason", rs.getString("reason")), Math.clamp(limit, 1, 500));
    }

    public List<Map<String, Object>> itemList() {
        return jdbc.query("SELECT id, name, category_id, price, available, track_stock, stock_quantity, target_prep_seconds, version FROM menu_item WHERE archived = false ORDER BY name",
                (rs, row) -> map("id", rs.getLong("id"), "name", rs.getString("name"), "categoryId", rs.getLong("category_id"),
                        "price", rs.getBigDecimal("price").toPlainString(), "available", rs.getBoolean("available"),
                        "trackStock", rs.getBoolean("track_stock"), "stockQuantity", rs.getObject("stock_quantity"),
                        "targetPrepSeconds", rs.getInt("target_prep_seconds"), "stationIds", stationIdsForItem(rs.getLong("id")),
                        "version", rs.getLong("version"), "modifierGroups", List.of()));
    }

    public Map<String, Object> item(long id) {
        var rows = jdbc.query("SELECT i.* FROM menu_item i WHERE i.id = ? AND i.archived = false",
                (rs, row) -> menuItem(rs), id);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Menu item not found.");
        return rows.getFirst();
    }

    @Transactional
    public void archiveMenuItem(long id, ApiActor actor, String correlationId) {
        requireFound("SELECT count(*) FROM menu_item WHERE id = ? AND archived = false", id, "Menu item");
        Integer referenced = jdbc.queryForObject("SELECT count(*) FROM order_line WHERE item_id = ?", Integer.class, id);
        if (referenced != null && referenced > 0) {
            throw new ApiFailure(409, "RESOURCE_IN_USE", "This menu item is part of an order and cannot be deleted.",
                    Map.of("references", List.of(Map.of("type", "order_line", "count", referenced))));
        }
        jdbc.update("UPDATE menu_item SET archived = true, available = false, version = version + 1 WHERE id = ?", id);
        audit(actor, "MENU_ITEM", id, "MENU_ITEM_ARCHIVED", null, correlationId);
    }

    private Map<String, Object> menuItem(ResultSet rs) throws SQLException {
        long itemId = rs.getLong("id");
        List<Map<String, Object>> groups = jdbc.query("""
                SELECT g.id, g.name, g.selection_type, g.min_select, g.max_select
                FROM item_modifier_group link JOIN modifier_group g ON g.id = link.group_id
                WHERE link.item_id = ? ORDER BY g.id
                """, (groupRs, row) -> {
                    long groupId = groupRs.getLong("id");
                    List<Map<String, Object>> modifiers = jdbc.query("SELECT id, name, price_delta FROM menu_modifier WHERE group_id = ? AND active = true ORDER BY id",
                            (modifierRs, modifierRow) -> map("id", modifierRs.getLong("id"), "name", modifierRs.getString("name"), "priceDelta", modifierRs.getBigDecimal("price_delta").toPlainString()), groupId);
                    return map("id", groupId, "name", groupRs.getString("name"), "selectionType", groupRs.getString("selection_type"),
                            "minSelect", groupRs.getInt("min_select"), "maxSelect", groupRs.getInt("max_select"), "modifiers", modifiers);
                }, itemId);
        return map("id", itemId, "name", rs.getString("name"), "price", rs.getBigDecimal("price").toPlainString(),
                "available", rs.getBoolean("available"), "trackStock", rs.getBoolean("track_stock"),
                "stockQuantity", rs.getObject("stock_quantity"), "targetPrepSeconds", rs.getInt("target_prep_seconds"),
                "stationIds", stationIdsForItem(itemId), "modifierGroups", groups, "version", rs.getLong("version"));
    }

    private List<Long> stationIdsForItem(long itemId) {
        return jdbc.queryForList("SELECT station_id FROM menu_item_station WHERE item_id = ? ORDER BY station_id", Long.class, itemId);
    }

    private void requireFound(String sql, long id, String name) {
        Integer count = jdbc.queryForObject(sql, Integer.class, id);
        if (count == null || count == 0) throw new ApiFailure(404, "NOT_FOUND", name + " not found.");
    }

    private void audit(ApiActor actor, String type, long id, String action, String reason, String correlationId) {
        jdbc.update("INSERT INTO audit_entry (actor_id, actor_role, entity_type, entity_id, action, correlation_id, reason) VALUES (?, ?, ?, ?, ?, ?, ?)",
                actor.id(), actor.role(), type, id, action, correlationId, reason);
    }

    private void checkVersion(Long expected, long actual) {
        if (expected != null && expected != actual) throw new ApiFailure(409, "STALE_VERSION", "This record changed on another device. Refresh and try again.");
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) result.put((String) pairs[index], pairs[index + 1]);
        return result;
    }
    private record ItemAvailability(long version, boolean trackStock, Integer stockQuantity) {}
    private record StockState(String name, boolean trackStock, Integer quantity, long version) {}
    private record GroupState(String name, String selectionType, int minSelect, int maxSelect) {}
}
