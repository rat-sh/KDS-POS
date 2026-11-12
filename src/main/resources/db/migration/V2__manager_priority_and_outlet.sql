ALTER TABLE order_header ADD COLUMN priority BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE outlet_config (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    outlet_name VARCHAR(120) NOT NULL,
    gstin VARCHAR(15) NOT NULL DEFAULT '',
    amber_percent INTEGER NOT NULL DEFAULT 80 CHECK (amber_percent > 0),
    red_percent INTEGER NOT NULL DEFAULT 100 CHECK (red_percent > amber_percent),
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO outlet_config (id, outlet_name, gstin, amber_percent, red_percent)
VALUES (1, 'KitchenFlow Operations', '', 80, 100);
