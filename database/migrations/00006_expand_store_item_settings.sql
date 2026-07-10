-- +goose Up
ALTER TABLE store_item_settings
    DROP CHECK chk_store_item_settings_type,
    ADD CONSTRAINT chk_store_item_settings_type CHECK (
        item_type IN (
            'GROUP',
            'GROUP_TEXT_CHANNEL',
            'GROUP_VOICE_CHANNEL',
            'UPDATE_GROUP',
            'ADDITIONAL_BOT',
            'GROUP_SLOT',
            'GROUP_PERMISSION',
            'PIN_MESSAGE',
            'COLOR_ROLE',
            'COUNTING_RELEASE',
            'MARRIAGE'
        )
    );

INSERT INTO store_item_settings (item_type, price, created_at, updated_at) VALUES
    ('COLOR_ROLE', 75000, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('COUNTING_RELEASE', 2000, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('MARRIAGE', 25000, UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

ALTER TABLE color_role_items
    DROP COLUMN price;

-- +goose Down
ALTER TABLE color_role_items
    ADD COLUMN price INT NOT NULL DEFAULT 75000 AFTER id;

DELETE FROM store_item_settings
WHERE item_type IN ('COLOR_ROLE', 'COUNTING_RELEASE', 'MARRIAGE');

ALTER TABLE store_item_settings
    DROP CHECK chk_store_item_settings_type,
    ADD CONSTRAINT chk_store_item_settings_type CHECK (
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
    );
