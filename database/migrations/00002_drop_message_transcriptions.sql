-- +goose Up
DROP TABLE IF EXISTS message_transcriptions;

-- +goose Down
CREATE TABLE message_transcriptions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    audio_length DOUBLE NOT NULL,
    transcription TEXT NOT NULL,
    file_extension VARCHAR(32) NOT NULL DEFAULT 'wav',
    is_harmful BOOLEAN,
    sexual_score DOUBLE,
    hate_score DOUBLE,
    illicit_score DOUBLE,
    self_harm_score DOUBLE,
    violence_score DOUBLE,
    created_at BIGINT NOT NULL,
    UNIQUE (requester_id, channel_id, message_id),
    UNIQUE (channel_id, message_id),
    CHECK (sexual_score >= 0 OR sexual_score IS NULL),
    CHECK (hate_score >= 0 OR hate_score IS NULL),
    CHECK (illicit_score >= 0 OR illicit_score IS NULL),
    CHECK (self_harm_score >= 0 OR self_harm_score IS NULL),
    CHECK (violence_score >= 0 OR violence_score IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
