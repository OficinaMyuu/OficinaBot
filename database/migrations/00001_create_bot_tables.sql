-- +goose Up
CREATE TABLE config (
    `key` VARCHAR(255) PRIMARY KEY,
    value TEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    global_name VARCHAR(255),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users_economy (
    user_id BIGINT PRIMARY KEY,
    bank INT NOT NULL DEFAULT 0,
    wallet INT NOT NULL DEFAULT 0,
    last_daily_at BIGINT,
    last_work_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CHECK (wallet >= 0 AND wallet <= 2147483647),
    CHECK (bank >= -2147483648 AND bank <= 2147483647)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users_xp (
    id INT PRIMARY KEY AUTO_INCREMENT,
    xp INT NOT NULL,
    level INT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users_preferences (
    user_id BIGINT PRIMARY KEY,
    locale VARCHAR(32),
    rankup_pings_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE accumulator_prizes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    amount INT,
    currency VARCHAR(64),
    color_role_id BIGINT,
    color_duration_seconds BIGINT,
    approved_by BIGINT,
    approved_at BIGINT,
    rejected_by BIGINT,
    rejected_at BIGINT,
    last_error TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_accumulator_prizes_status ON accumulator_prizes(status);
CREATE INDEX idx_accumulator_prizes_target ON accumulator_prizes(target_id);

CREATE TABLE application_users_ban (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    expires_at BIGINT NOT NULL,
    banned_at BIGINT NOT NULL,
    CHECK (expires_at > banned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_application_users_ban_user ON application_users_ban(user_id);

CREATE TABLE automod_actions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    threshold INT NOT NULL,
    duration INT NOT NULL,
    action VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (threshold),
    UNIQUE (duration, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bet_games (
    id BIGINT PRIMARY KEY,
    status VARCHAR(64) NOT NULL,
    board TEXT,
    bet_type VARCHAR(64) NOT NULL,
    started_at BIGINT NOT NULL,
    ended_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE games_participants (
    id INT PRIMARY KEY AUTO_INCREMENT,
    game_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    has_won BOOLEAN NOT NULL,
    created_at BIGINT NOT NULL,
    UNIQUE (game_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE birthdays (
    user_id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    birthday DATE NOT NULL,
    zone_hours INT NOT NULL DEFAULT -3,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blocked_words (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    word VARCHAR(255) NOT NULL,
    severe BOOLEAN NOT NULL,
    match_exact BOOLEAN NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_blocked_words_guild ON blocked_words(guild_id);

CREATE TABLE color_role_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    price INT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE color_roles_state (
    id INT PRIMARY KEY AUTO_INCREMENT,
    value_paid INT NOT NULL,
    currency VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    guild_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (role_id, guild_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_color_roles_state_user ON color_roles_state(user_id);
CREATE INDEX idx_color_roles_state_expires ON color_roles_state(expires_at);

CREATE TABLE commands_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    command_name VARCHAR(128) NOT NULL,
    exit_status VARCHAR(64) NOT NULL,
    ticks_cooldown BOOLEAN NOT NULL DEFAULT TRUE,
    guild_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_commands_history_user ON commands_history(user_id);
CREATE INDEX idx_commands_history_created ON commands_history(created_at);

CREATE TABLE custom_userinfo (
    user_id BIGINT PRIMARY KEY,
    color INT,
    description TEXT,
    footer TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE entities_policies (
    id INT PRIMARY KEY AUTO_INCREMENT,
    resource VARCHAR(255) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    policy_type VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    UNIQUE (resource, policy_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE former_members_roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    privileged INT NOT NULL,
    user_id BIGINT NOT NULL,
    guild_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    UNIQUE (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE game_mafia_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    match_id VARCHAR(128) NOT NULL,
    guild_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    action VARCHAR(128) NOT NULL,
    actor_user_id BIGINT,
    target_user_id BIGINT,
    channel_id BIGINT,
    phase VARCHAR(64),
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_game_mafia_logs_match ON game_mafia_logs(match_id);

CREATE TABLE giveaways (
    giveaway_id VARCHAR(128) PRIMARY KEY,
    guild_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    host_id BIGINT NOT NULL,
    status VARCHAR(64) NOT NULL,
    prize_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    winner_count INT NOT NULL,
    ends_at BIGINT NOT NULL,
    ended_at BIGINT,
    required_voice_channel_id BIGINT,
    money_amount BIGINT,
    color_role_duration_seconds BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_giveaways_status_ends ON giveaways(status, ends_at);

CREATE TABLE giveaway_entries (
    giveaway_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (giveaway_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE giveaway_winners (
    id INT PRIMARY KEY AUTO_INCREMENT,
    giveaway_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(64) NOT NULL,
    currency VARCHAR(64),
    color_role_id BIGINT,
    claimed_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (giveaway_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE group_bots (
    id INT PRIMARY KEY AUTO_INCREMENT,
    bot_id BIGINT NOT NULL,
    bot_name VARCHAR(255) NOT NULL,
    bot_category VARCHAR(128) NOT NULL,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `groups` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    guild_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    text_channel_id BIGINT,
    voice_channel_id BIGINT,
    name VARCHAR(255) NOT NULL,
    emoji VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    currency VARCHAR(64) NOT NULL,
    amount_paid INT NOT NULL,
    invoice_amount BIGINT NOT NULL,
    refund_percent DOUBLE NOT NULL,
    has_free_access BOOLEAN NOT NULL,
    has_role_emoji BOOLEAN NOT NULL DEFAULT FALSE,
    rent_status VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (emoji)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_groups_owner ON `groups`(owner_id);

CREATE TABLE groups_perks (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_id INT NOT NULL,
    user_id BIGINT NOT NULL,
    item VARCHAR(128) NOT NULL,
    value_paid INT NOT NULL,
    currency VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_groups_perks_group ON groups_perks(group_id);
CREATE INDEX idx_groups_perks_user ON groups_perks(user_id);

CREATE TABLE levels_roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    level INT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    UNIQUE (level, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE marriage_requests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_marriage_requests_target ON marriage_requests(target_id);

CREATE TABLE marriages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    married_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_marriages_requester ON marriages(requester_id);
CREATE INDEX idx_marriages_target ON marriages(target_id);

CREATE TABLE members_emojis (
    user_id BIGINT PRIMARY KEY,
    emoji VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE member_join_events (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_member_join_events_user ON member_join_events(user_id, guild_id, created_at);

CREATE TABLE members_punishments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    moderator_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    active BOOLEAN NOT NULL,
    deletion_author_id BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_members_punishments_user ON members_punishments(user_id);
CREATE INDEX idx_members_punishments_active ON members_punishments(active);

CREATE TABLE mentions_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    mentioned_user_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_mentions_log_author ON mentions_log(author_id);
CREATE INDEX idx_mentions_log_mentioned ON mentions_log(mentioned_user_id);

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

CREATE TABLE messages_versions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    message_ref_id BIGINT,
    content TEXT,
    sticker_id BIGINT,
    is_deleted BOOLEAN NOT NULL,
    is_original BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by_id BIGINT,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_messages_versions_message ON messages_versions(message_id, created_at);
CREATE INDEX idx_messages_versions_author ON messages_versions(author_id);
CREATE INDEX idx_messages_versions_channel_created ON messages_versions(channel_id, created_at);

CREATE TABLE nickname_update_requests (
    request_id VARCHAR(128) PRIMARY KEY,
    guild_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    submitted_by_id BIGINT NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    approve_button_id VARCHAR(128) NOT NULL,
    reject_button_id VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    decision_author_id BIGINT,
    decided_at BIGINT,
    emoji_approval_summary TEXT,
    unauthorized_summary TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (message_id),
    UNIQUE (approve_button_id),
    UNIQUE (reject_button_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_nickname_update_requests_target ON nickname_update_requests(target_user_id);

CREATE TABLE registers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    target_id BIGINT NOT NULL,
    moderator_id BIGINT NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(64) NOT NULL,
    device VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_registers_target ON registers(target_id);

CREATE TABLE users_reminders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    channel_type VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(64) NOT NULL,
    reminder_value BIGINT,
    schedule_expression VARCHAR(255),
    trigger_times INT NOT NULL,
    triggers_left INT NOT NULL,
    last_triggered_at BIGINT NOT NULL DEFAULT 0,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CHECK (reminder_value IS NOT NULL OR schedule_expression IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_reminders_user ON users_reminders(user_id);
CREATE INDEX idx_users_reminders_expired ON users_reminders(expired);

CREATE TABLE support_tickets (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    guild_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    initiator_id BIGINT NOT NULL,
    close_reason TEXT,
    closed_by_id BIGINT,
    merged_into INT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_support_tickets_initiator ON support_tickets(initiator_id);

CREATE TABLE temp_bans (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    guild_id BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    UNIQUE (guild_id, user_id),
    CHECK (expires_at > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usernames_updates (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    guild_id BIGINT,
    scope VARCHAR(64) NOT NULL,
    author_id BIGINT NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_usernames_updates_user ON usernames_updates(user_id);

CREATE TABLE users_emojis_permissions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    emoji VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    created_at BIGINT NOT NULL,
    UNIQUE (user_id, emoji)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE voice_channel_income_rules (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    payout_type VARCHAR(64) NOT NULL,
    multiplier DOUBLE NOT NULL,
    allow_muted BOOLEAN NOT NULL DEFAULT FALSE,
    allow_solo BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (channel_id, payout_type),
    CHECK (multiplier > 0.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE voice_heartbeats (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    is_muted BOOLEAN NOT NULL,
    is_deafened BOOLEAN NOT NULL,
    is_video BOOLEAN NOT NULL DEFAULT FALSE,
    is_stream BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_voice_heartbeats_user_created ON voice_heartbeats(user_id, created_at);

CREATE TABLE welcomed_users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    moderator_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    comment TEXT,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_welcomed_users_target ON welcomed_users(target_id);

-- +goose Down
DROP TABLE welcomed_users;
DROP TABLE voice_heartbeats;
DROP TABLE voice_channel_income_rules;
DROP TABLE users_emojis_permissions;
DROP TABLE usernames_updates;
DROP TABLE temp_bans;
DROP TABLE support_tickets;
DROP TABLE users_reminders;
DROP TABLE registers;
DROP TABLE nickname_update_requests;
DROP TABLE messages_versions;
DROP TABLE message_transcriptions;
DROP TABLE mentions_log;
DROP TABLE members_punishments;
DROP TABLE member_join_events;
DROP TABLE members_emojis;
DROP TABLE marriages;
DROP TABLE marriage_requests;
DROP TABLE levels_roles;
DROP TABLE groups_perks;
DROP TABLE `groups`;
DROP TABLE group_bots;
DROP TABLE giveaway_winners;
DROP TABLE giveaway_entries;
DROP TABLE giveaways;
DROP TABLE game_mafia_logs;
DROP TABLE former_members_roles;
DROP TABLE entities_policies;
DROP TABLE custom_userinfo;
DROP TABLE commands_history;
DROP TABLE color_roles_state;
DROP TABLE color_role_items;
DROP TABLE blocked_words;
DROP TABLE birthdays;
DROP TABLE games_participants;
DROP TABLE bet_games;
DROP TABLE automod_actions;
DROP TABLE application_users_ban;
DROP TABLE accumulator_prizes;
DROP TABLE users_preferences;
DROP TABLE users_xp;
DROP TABLE users_economy;
DROP TABLE users;
DROP TABLE config;
