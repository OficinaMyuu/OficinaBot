-- +goose Up
CREATE TABLE store_item_settings (
    item_type VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    price INT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (item_type),
    CONSTRAINT chk_store_item_settings_type CHECK (
        item_type IN (
            'GROUP',
            'GROUP_TEXT_CHANNEL',
            'GROUP_VOICE_CHANNEL',
            'UPDATE_GROUP',
            'ADDITIONAL_BOT',
            'GROUP_SLOT',
            'GROUP_PERMISSION',
            'PIN_MESSAGE'
        )
    ),
    CONSTRAINT chk_store_item_settings_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO store_item_settings (item_type, price, created_at, updated_at) VALUES
    ('GROUP', 600000, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('GROUP_TEXT_CHANNEL', 412500, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('GROUP_VOICE_CHANNEL', 300000, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('UPDATE_GROUP', 187500, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('ADDITIONAL_BOT', 80000, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('GROUP_SLOT', 75000, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('GROUP_PERMISSION', 15000, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('PIN_MESSAGE', 8000, UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

-- +goose Down
DROP TABLE store_item_settings;
