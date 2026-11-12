-- V5: Remove all pre-seeded data so each organisation starts with a blank slate.
-- Only deletes rows that have no operational references (safe on a live instance too).

-- Remove pre-seeded stations (Fry, Grill, Assembly, Beverage) if unused.
DELETE FROM user_station   WHERE station_id IN (1, 2, 3, 4);
DELETE FROM menu_item_station WHERE station_id IN (1, 2, 3, 4);
DELETE FROM station        WHERE id IN (1, 2, 3, 4)
  AND NOT EXISTS (SELECT 1 FROM kitchen_ticket kt WHERE kt.station_id = station.id);

-- Remove pre-seeded categories (Food, Beverage) if no items reference them.
DELETE FROM menu_category  WHERE id IN (1, 2)
  AND NOT EXISTS (SELECT 1 FROM menu_item mi WHERE mi.category_id = menu_category.id);

-- Clear the placeholder outlet name set in V2 so the admin configures their own.
UPDATE outlet_config SET outlet_name = '' WHERE id = 1 AND outlet_name = 'KitchenFlow Operations';

