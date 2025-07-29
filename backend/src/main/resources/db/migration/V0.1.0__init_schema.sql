-- V0.1.0 init schema

-- ===========================
-- set_updated_at() function
-- ===========================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE member (
    id BIGINT PRIMARY KEY,
    member_name TEXT NOT NULL,

    created_by BIGINT DEFAULT 0,
    updated_by BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE server (
    id BIGINT PRIMARY KEY,
    server_name TEXT NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE server_category (
    id BIGINT PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    category_name TEXT NOT NULL,
    position INTEGER NOT NULL,
    UNIQUE (server_id, category_name)
);


CREATE TABLE channel (
    id BIGINT PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    server_category_id BIGINT REFERENCES server_category(id) on DELETE SET NULL,
    channel_name TEXT NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE authorization_type (
    id BIGSERIAL PRIMARY KEY,
    authorization_type_name TEXT NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(authorization_type_name)
);

CREATE TABLE member_channel_authorization (
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    channel_id BIGINT NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
    authorization_type_id BIGINT NOT NULL REFERENCES authorization_type(id) ON DELETE RESTRICT,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY(member_id, channel_id, authorization_type_id)
);

-- File management tables
CREATE TABLE file_path (
    id BIGSERIAL PRIMARY KEY,
    path_name TEXT NOT NULL,
    file_path TEXT NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE file_type (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,  -- server-scoped customization
    type_name TEXT NOT NULL,              -- e.g., 'quiz_image', 'project_doc'
    description TEXT,
    file_path_id BIGINT NOT NULL REFERENCES file_path(id) ON DELETE RESTRICT,
    created_by BIGINT REFERENCES member(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (server_id, type_name)         -- ensures unique type name per server
);

CREATE TABLE file (
    id BIGSERIAL PRIMARY KEY,
    file_name TEXT NOT NULL,
    file_type_id BIGINT NOT NULL REFERENCES file_type(id) ON DELETE RESTRICT,
    size BIGINT,
    mime_type TEXT,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE member_server_stat (
    member_id BIGINT NOT NULL REFERENCES member(id)ON DELETE CASCADE,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    join_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    level INTEGER DEFAULT 0 NOT NULL,
    last_active TIMESTAMP WITH TIME ZONE NOT NULL,
    total_messages BIGINT DEFAULT 0 NOT NULL,
    participation_streak INTEGER DEFAULT 0 NOT NULL,
    experience INTEGER DEFAULT 0 NOT NULL,
    num_warnings INTEGER DEFAULT 0 NOT NULL,
    num_bans INTEGER DEFAULT 0 NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id, server_id)
);

CREATE TABLE warning (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    moderator_id BIGINT REFERENCES member(id) ON DELETE SET NULL,
    reason TEXT,
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ban (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    moderator_id BIGINT REFERENCES member(id) ON DELETE SET NULL,
    reason TEXT,
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    duration INTERVAL,  -- Null for indefinitely
    unbanned BOOLEAN DEFAULT FALSE NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE server_rule (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    rule_index INTEGER NOT NULL,
    content TEXT NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (server_id, rule_index)
);

CREATE TABLE reminder (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    remind_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed BOOLEAN DEFAULT FALSE NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE announcement (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    channel_id BIGINT NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    send_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent BOOLEAN DEFAULT FALSE NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

);

CREATE TABLE server_role (
    id BIGINT PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    role_name TEXT NOT NULL,
    is_auto_assignable BOOLEAN DEFAULT FALSE NOT NULL,
    is_self_assignable BOOLEAN DEFAULT FALSE NOT NULL,
    level_required INTEGER DEFAULT 0 NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE member_role (
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    server_role_id BIGINT NOT NULL REFERENCES server_role(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (member_id, server_role_id)
);

CREATE TABLE poll (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    allow_multiple_votes BOOLEAN DEFAULT FALSE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE poll_option (
    id BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
    option_text TEXT NOT NULL,
    option_index INTEGER NOT NULL,
    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE poll_vote (
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    poll_id BIGINT NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
    poll_option_id BIGINT REFERENCES poll_option(id) ON DELETE SET NULL,
    voted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (member_id, poll_id, poll_option_id)
);

CREATE TABLE flag_reason (
    id BIGSERIAL PRIMARY KEY,
    reason TEXT NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE filtered_message (
    id BIGINT PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    channel_id BIGINT NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    flagged_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reason BIGINT NOT NULL REFERENCES flag_reason(id) ON DELETE CASCADE,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE problem_category (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    category_name TEXT NOT NULL,
    description TEXT NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (server_id, category_name),
    UNIQUE (server_id, id)
);


CREATE TABLE problem (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    problem_category_id BIGINT NOT NULL REFERENCES problem_category(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    media_file_id BIGINT REFERENCES file(id) ON DELETE SET NULL,
    contributor_id BIGINT REFERENCES member(id) ON DELETE SET NULL,
    is_valid BOOLEAN DEFAULT TRUE NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (server_id, problem_category_id) REFERENCES problem_category(server_id, id) ON DELETE CASCADE
);

CREATE TABLE problem_choice (
    id BIGSERIAL PRIMARY KEY,
    problem_id BIGINT NOT NULL REFERENCES problem(id) ON DELETE CASCADE,
    choice_text TEXT,
    media_file_id BIGINT REFERENCES file(id) ON DELETE SET NULL,
    is_correct BOOLEAN NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CHECK (
        choice_text IS NOT NULL OR media_file_id IS NOT NULL
    )
);

-- Quiz answer history
CREATE TABLE problem_answer_history (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    problem_id BIGINT NOT NULL REFERENCES problem(id) ON DELETE CASCADE,
    selected_choice_id BIGINT REFERENCES problem_choice(id) ON DELETE SET NULL,
    is_correct BOOLEAN NOT NULL,
    answered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE achievement (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    channel_id BIGINT REFERENCES channel(id) ON DELETE CASCADE, -- null means applies to all channels in server
    achievement_name TEXT NOT NULL,
    description TEXT,
    hidden BOOLEAN DEFAULT TRUE NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE member_achievement (
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    achievement_id BIGINT NOT NULL REFERENCES achievement(id) ON DELETE CASCADE,
    achieved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (member_id, server_id, achievement_id)
);

CREATE TABLE server_setting (
    server_id BIGINT PRIMARY KEY,
    auto_mod_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    level_system_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    level_multiplier DOUBLE PRECISION DEFAULT 1.0 NOT NULL,
    announcement_channel_id BIGINT REFERENCES channel(id) ON DELETE SET NULL,
    additional_settings JSONB,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reaction_role (
    id SERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    channel_id BIGINT NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
    server_role_id BIGINT NOT NULL REFERENCES server_role(id),
    message_id BIGINT NOT NULL,
    emoji TEXT NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE assistant_conversation (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT FALSE NOT NULL,
    is_interrupt BOOLEAN DEFAULT FALSE NOT NULL,
    is_processing BOOLEAN DEFAULT FALSE NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uniq_active_conversation_per_member_server
ON assistant_conversation (member_id, server_id)
WHERE is_active = true;

CREATE TABLE mcp_server (
    id BIGSERIAL PRIMARY KEY,
    discord_server_id BIGINT REFERENCES server(id) ON DELETE CASCADE,
    server_name TEXT NOT NULL,
    server_url TEXT NOT NULL,
    description TEXT,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tool (
    id BIGSERIAL PRIMARY KEY,
    mcp_server_id BIGINT NOT NULL REFERENCES mcp_server(id) ON DELETE CASCADE,
    tool_name TEXT NOT NULL,
    description TEXT,
    enabled BOOLEAN DEFAULT FALSE NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TYPE tool_whitelist_scope AS ENUM ('global', 'server', 'conversation');

CREATE TABLE tool_whitelist (
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    tool_id BIGINT NOT NULL REFERENCES tool(id) ON DELETE CASCADE,
    server_id BIGINT NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    assistant_conversation_id BIGINT DEFAULT 0 NOT NULL REFERENCES assistant_conversation(id) ON DELETE CASCADE,
    scope tool_whitelist_scope NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (member_id, tool_id, server_id, assistant_conversation_id)
);

-- ===========================
-- Documentation Comments
-- ===========================
COMMENT ON TABLE member IS 'Stores member information and references for Discord members.';
COMMENT ON TABLE server IS 'Stores Discord server (guild) information.';
COMMENT ON TABLE channel IS 'Stores Discord channel information within servers, including category association.';
COMMENT ON TABLE authorization_type IS 'Stores authorization types for member-channel authorizations.';
COMMENT ON TABLE member_channel_authorization IS 'Stores fine-grained authorizations each member has on a specific channel.';
COMMENT ON TABLE server_category IS 'Stores Discord server category/channel grouping information.';
COMMENT ON TABLE member_server_stat IS 'Tracks member statistics per server for leveling and activity.';
COMMENT ON TABLE warning IS 'Stores warnings issued to members by moderators.';
COMMENT ON TABLE ban IS 'Stores ban records issued to members by moderators.';
COMMENT ON TABLE server_rule IS 'Stores server-specific rules.';
COMMENT ON TABLE reminder IS 'Stores member reminders.';
COMMENT ON TABLE announcement IS 'Stores server announcements.';
COMMENT ON TABLE server_role IS 'Stores server-specific roles, including auto/self-assignable roles.';
COMMENT ON TABLE member_role IS 'Stores member-role assignments.';
COMMENT ON TABLE poll IS 'Stores polls created within servers.';
COMMENT ON TABLE poll_vote IS 'Stores member votes on polls.';
COMMENT ON TABLE filtered_message IS 'Stores messages flagged by auto-moderation.';
COMMENT ON TABLE flag_reason IS 'Stores flag reasons for moderation.';
COMMENT ON TABLE problem_category IS 'Stores quiz problem categories per server.';
COMMENT ON TABLE problem IS 'Stores quiz problems/questions.';
COMMENT ON TABLE problem_choice IS 'Stores choices for quiz problems.';
COMMENT ON TABLE achievement IS 'Stores achievements members can earn.';
COMMENT ON TABLE member_achievement IS 'Tracks member achievements per server.';
COMMENT ON TABLE poll_option IS 'Stores individual options associated with polls.';
COMMENT ON TABLE server_setting IS 'Stores server-level settings.';
COMMENT ON TABLE reaction_role IS 'Stores reaction-role mapping for self-assignment.';
COMMENT ON TABLE file IS 'Stores uploaded files metadata.';
COMMENT ON TABLE file_type IS 'Stores server-scoped file type definitions for uploaded files.';
COMMENT ON TABLE file_path IS 'Stores various storage paths for files.';
COMMENT ON TABLE problem_answer_history IS 'Tracks member quiz answer submissions and correctness.';
COMMENT ON TABLE assistant_conversation IS 'Stores conversation state for human-in-the-loop agent interactions.';
COMMENT ON TABLE mcp_server IS 'Stores MCP (Model Context Protocol) server definitions.';
COMMENT ON TABLE tool IS 'Stores available tools and their associated MCP servers.';
COMMENT ON TABLE tool_whitelist IS 'Stores member tool approval permissions with scope and expiration.';


-- ===========================
-- COLUMN COMMENTS FOR ALL TABLES
-- ===========================
-- This part contains comprehensive column documentation for the Discord bot database schema
-- Following PostgreSQL best practices for database documentation

-- ===========================
-- CORE TABLES
-- ===========================

-- member table
COMMENT ON COLUMN member.id IS 'Primary key - Discord member ID (snowflake)';
COMMENT ON COLUMN member.member_name IS 'Discord username or display name';
COMMENT ON COLUMN member.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN member.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN member.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN member.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- server table
COMMENT ON COLUMN server.id IS 'Primary key - Discord server/guild ID (snowflake)';
COMMENT ON COLUMN server.server_name IS 'Discord server/guild name';
COMMENT ON COLUMN server.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN server.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN server.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN server.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- server_category table
COMMENT ON COLUMN server_category.id IS 'Primary key - Discord category ID (snowflake)';
COMMENT ON COLUMN server_category.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN server_category.category_name IS 'Name of the Discord channel category';
COMMENT ON COLUMN server_category.position IS 'Display order position within the server';

-- channel table
COMMENT ON COLUMN channel.id IS 'Primary key - Discord channel ID (snowflake)';
COMMENT ON COLUMN channel.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN channel.server_category_id IS 'Foreign key to server_category table (nullable)';
COMMENT ON COLUMN channel.channel_name IS 'Discord channel name';
COMMENT ON COLUMN channel.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN channel.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN channel.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN channel.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ===========================
-- AUTHORIZATION TABLES
-- ===========================

-- authorization_type table
COMMENT ON COLUMN authorization_type.id IS 'Primary key - auto-incrementing authorization type ID';
COMMENT ON COLUMN authorization_type.authorization_type_name IS 'Name of the authorization type (e.g., "read", "write", "admin")';
COMMENT ON COLUMN authorization_type.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN authorization_type.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN authorization_type.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN authorization_type.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- member_channel_authorization table
COMMENT ON COLUMN member_channel_authorization.member_id IS 'Foreign key to member table';
COMMENT ON COLUMN member_channel_authorization.channel_id IS 'Foreign key to channel table';
COMMENT ON COLUMN member_channel_authorization.authorization_type_id IS 'Foreign key to authorization_type table';
COMMENT ON COLUMN member_channel_authorization.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN member_channel_authorization.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN member_channel_authorization.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN member_channel_authorization.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ===========================
-- FILE MANAGEMENT TABLES
-- ===========================

-- file_path table
COMMENT ON COLUMN file_path.id IS 'Primary key - auto-incrementing file path ID';
COMMENT ON COLUMN file_path.path_name IS 'Human-readable name for this file path configuration';
COMMENT ON COLUMN file_path.file_path IS 'Actual file system path or storage location';
COMMENT ON COLUMN file_path.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN file_path.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN file_path.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN file_path.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- file_type table
COMMENT ON COLUMN file_type.id IS 'Primary key - auto-incrementing file type ID';
COMMENT ON COLUMN file_type.server_id IS 'Foreign key to server table (server-scoped file types)';
COMMENT ON COLUMN file_type.type_name IS 'Name of the file type (e.g., "quiz_image", "project_doc")';
COMMENT ON COLUMN file_type.description IS 'Description of what this file type is used for';
COMMENT ON COLUMN file_type.file_path_id IS 'Foreign key to file_path table (storage location)';
COMMENT ON COLUMN file_type.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN file_type.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN file_type.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- file table
COMMENT ON COLUMN file.id IS 'Primary key - auto-incrementing file ID';
COMMENT ON COLUMN file.file_name IS 'Original filename as uploaded';
COMMENT ON COLUMN file.file_type_id IS 'Foreign key to file_type table';
COMMENT ON COLUMN file.size IS 'File size in bytes';
COMMENT ON COLUMN file.mime_type IS 'MIME type of the file (e.g., "image/png", "application/pdf")';
COMMENT ON COLUMN file.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN file.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN file.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN file.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ===========================
-- MEMBER STATISTICS & GAMIFICATION
-- ===========================

-- member_server_stat table
COMMENT ON COLUMN member_server_stat.member_id IS 'Foreign key to member table';
COMMENT ON COLUMN member_server_stat.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN member_server_stat.join_date IS 'When the member joined this server';
COMMENT ON COLUMN member_server_stat.level IS 'Current level of the member in this server';
COMMENT ON COLUMN member_server_stat.last_active IS 'Timestamp of last activity in this server';
COMMENT ON COLUMN member_server_stat.total_messages IS 'Total number of messages sent by member in this server';
COMMENT ON COLUMN member_server_stat.participation_streak IS 'Current consecutive days of participation';
COMMENT ON COLUMN member_server_stat.experience IS 'Total experience points earned in this server';
COMMENT ON COLUMN member_server_stat.num_warnings IS 'Total number of warnings received in this server';
COMMENT ON COLUMN member_server_stat.num_bans IS 'Total number of bans received in this server';
COMMENT ON COLUMN member_server_stat.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN member_server_stat.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN member_server_stat.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN member_server_stat.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ===========================
-- MODERATION TABLES
-- ===========================

-- warning table
COMMENT ON COLUMN warning.id IS 'Primary key - auto-incrementing warning ID';
COMMENT ON COLUMN warning.member_id IS 'Foreign key to member table (member being warned)';
COMMENT ON COLUMN warning.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN warning.moderator_id IS 'Foreign key to member table (moderator issuing warning)';
COMMENT ON COLUMN warning.reason IS 'Reason for the warning';
COMMENT ON COLUMN warning.issued_at IS 'Timestamp when warning was issued';
COMMENT ON COLUMN warning.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN warning.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN warning.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN warning.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ban table
COMMENT ON COLUMN ban.id IS 'Primary key - auto-incrementing ban ID';
COMMENT ON COLUMN ban.member_id IS 'Foreign key to member table (member being banned)';
COMMENT ON COLUMN ban.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN ban.moderator_id IS 'Foreign key to member table (moderator issuing ban)';
COMMENT ON COLUMN ban.reason IS 'Reason for the ban';
COMMENT ON COLUMN ban.issued_at IS 'Timestamp when ban was issued';
COMMENT ON COLUMN ban.duration IS 'Duration of the ban (NULL for permanent)';
COMMENT ON COLUMN ban.unbanned IS 'Whether the ban has been lifted';
COMMENT ON COLUMN ban.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN ban.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN ban.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN ban.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- flag_reason table
COMMENT ON COLUMN flag_reason.id IS 'Primary key - auto-incrementing flag reason ID';
COMMENT ON COLUMN flag_reason.reason IS 'Description of the flag reason (e.g., "spam", "inappropriate content")';
COMMENT ON COLUMN flag_reason.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN flag_reason.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN flag_reason.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN flag_reason.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- filtered_message table
COMMENT ON COLUMN filtered_message.id IS 'Primary key - Discord message ID (snowflake)';
COMMENT ON COLUMN filtered_message.member_id IS 'Foreign key to member table (message author)';
COMMENT ON COLUMN filtered_message.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN filtered_message.channel_id IS 'Foreign key to channel table';
COMMENT ON COLUMN filtered_message.content IS 'Content of the flagged message';
COMMENT ON COLUMN filtered_message.flagged_at IS 'Timestamp when message was flagged';
COMMENT ON COLUMN filtered_message.reason IS 'Foreign key to flag_reason table';
COMMENT ON COLUMN filtered_message.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN filtered_message.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN filtered_message.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN filtered_message.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ===========================
-- SERVER MANAGEMENT TABLES
-- ===========================

-- server_rule table
COMMENT ON COLUMN server_rule.id IS 'Primary key - auto-incrementing rule ID';
COMMENT ON COLUMN server_rule.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN server_rule.rule_index IS 'Display order of the rule (1, 2, 3, etc.)';
COMMENT ON COLUMN server_rule.content IS 'Text content of the rule';
COMMENT ON COLUMN server_rule.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN server_rule.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN server_rule.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN server_rule.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- server_setting table
COMMENT ON COLUMN server_setting.server_id IS 'Primary key - foreign key to server table';
COMMENT ON COLUMN server_setting.auto_mod_enabled IS 'Whether automatic moderation is enabled';
COMMENT ON COLUMN server_setting.level_system_enabled IS 'Whether the leveling system is enabled';
COMMENT ON COLUMN server_setting.level_multiplier IS 'Multiplier for experience point calculations';
COMMENT ON COLUMN server_setting.announcement_channel_id IS 'Foreign key to channel table (default announcement channel)';
COMMENT ON COLUMN server_setting.additional_settings IS 'JSON object for additional server-specific settings';
COMMENT ON COLUMN server_setting.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN server_setting.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN server_setting.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN server_setting.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ===========================
-- UTILITY TABLES
-- ===========================

-- reminder table
COMMENT ON COLUMN reminder.id IS 'Primary key - auto-incrementing reminder ID';
COMMENT ON COLUMN reminder.member_id IS 'Foreign key to member table (member who set the reminder)';
COMMENT ON COLUMN reminder.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN reminder.content IS 'Text content of the reminder';
COMMENT ON COLUMN reminder.remind_at IS 'Timestamp when the reminder should be triggered';
COMMENT ON COLUMN reminder.completed IS 'Whether the reminder has been sent/completed';
COMMENT ON COLUMN reminder.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN reminder.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN reminder.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN reminder.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- announcement table
COMMENT ON COLUMN announcement.id IS 'Primary key - auto-incrementing announcement ID';
COMMENT ON COLUMN announcement.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN announcement.channel_id IS 'Foreign key to channel table (where to send announcement)';
COMMENT ON COLUMN announcement.content IS 'Text content of the announcement';
COMMENT ON COLUMN announcement.send_at IS 'Timestamp when the announcement should be sent';
COMMENT ON COLUMN announcement.sent IS 'Whether the announcement has been sent';
COMMENT ON COLUMN announcement.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN announcement.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN announcement.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN announcement.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ===========================
-- ROLE MANAGEMENT TABLES
-- ===========================

-- server_role table
COMMENT ON COLUMN server_role.id IS 'Primary key - Discord role ID (snowflake)';
COMMENT ON COLUMN server_role.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN server_role.role_name IS 'Name of the Discord role';
COMMENT ON COLUMN server_role.is_auto_assignable IS 'Whether this role is automatically assigned based on criteria';
COMMENT ON COLUMN server_role.is_self_assignable IS 'Whether members can assign this role to themselves';
COMMENT ON COLUMN server_role.level_required IS 'Minimum level required to obtain this role';
COMMENT ON COLUMN server_role.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN server_role.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN server_role.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN server_role.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- member_role table
COMMENT ON COLUMN member_role.member_id IS 'Foreign key to member table';
COMMENT ON COLUMN member_role.server_role_id IS 'Foreign key to server_role table';
COMMENT ON COLUMN member_role.assigned_at IS 'Timestamp when the role was assigned to the member';
COMMENT ON COLUMN member_role.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN member_role.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN member_role.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN member_role.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- reaction_role table
COMMENT ON COLUMN reaction_role.id IS 'Primary key - auto-incrementing reaction role ID';
COMMENT ON COLUMN reaction_role.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN reaction_role.channel_id IS 'Foreign key to channel table';
COMMENT ON COLUMN reaction_role.server_role_id IS 'Foreign key to server_role table';
COMMENT ON COLUMN reaction_role.message_id IS 'Discord message ID (snowflake) that contains the reaction';
COMMENT ON COLUMN reaction_role.emoji IS 'Emoji that triggers the role assignment';
COMMENT ON COLUMN reaction_role.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN reaction_role.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN reaction_role.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN reaction_role.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ===========================
-- POLLING SYSTEM TABLES
-- ===========================

-- poll table
COMMENT ON COLUMN poll.id IS 'Primary key - auto-incrementing poll ID';
COMMENT ON COLUMN poll.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN poll.question IS 'The poll question or prompt';
COMMENT ON COLUMN poll.allow_multiple_votes IS 'Whether members can vote for multiple options';
COMMENT ON COLUMN poll.ends_at IS 'Timestamp when the poll closes';
COMMENT ON COLUMN poll.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN poll.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN poll.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN poll.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- poll_option table
COMMENT ON COLUMN poll_option.id IS 'Primary key - auto-incrementing poll option ID';
COMMENT ON COLUMN poll_option.poll_id IS 'Foreign key to poll table';
COMMENT ON COLUMN poll_option.option_text IS 'Text content of the poll option';
COMMENT ON COLUMN poll_option.option_index IS 'Display order of the option (1, 2, 3, etc.)';
COMMENT ON COLUMN poll_option.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN poll_option.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN poll_option.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN poll_option.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- poll_vote table
COMMENT ON COLUMN poll_vote.member_id IS 'Foreign key to member table (voter)';
COMMENT ON COLUMN poll_vote.poll_id IS 'Foreign key to poll table';
COMMENT ON COLUMN poll_vote.poll_option_id IS 'Foreign key to poll_option table (chosen option)';
COMMENT ON COLUMN poll_vote.voted_at IS 'Timestamp when the vote was cast';
COMMENT ON COLUMN poll_vote.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN poll_vote.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN poll_vote.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN poll_vote.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ===========================
-- QUIZ/PROBLEM SYSTEM TABLES
-- ===========================

-- problem_category table
COMMENT ON COLUMN problem_category.id IS 'Primary key - auto-incrementing category ID';
COMMENT ON COLUMN problem_category.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN problem_category.category_name IS 'Name of the problem category (e.g., "Math", "Science")';
COMMENT ON COLUMN problem_category.description IS 'Description of what problems this category contains';
COMMENT ON COLUMN problem_category.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN problem_category.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN problem_category.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN problem_category.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- problem table
COMMENT ON COLUMN problem.id IS 'Primary key - auto-incrementing problem ID';
COMMENT ON COLUMN problem.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN problem.problem_category_id IS 'Foreign key to problem_category table';
COMMENT ON COLUMN problem.question IS 'Text content of the problem/question';
COMMENT ON COLUMN problem.media_file_id IS 'Foreign key to file table (optional image/media for question)';
COMMENT ON COLUMN problem.contributor_id IS 'Foreign key to member table (member who contributed this problem)';
COMMENT ON COLUMN problem.is_valid IS 'Whether this problem is active and valid for use';
COMMENT ON COLUMN problem.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN problem.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN problem.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN problem.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- problem_choice table
COMMENT ON COLUMN problem_choice.id IS 'Primary key - auto-incrementing choice ID';
COMMENT ON COLUMN problem_choice.problem_id IS 'Foreign key to problem table';
COMMENT ON COLUMN problem_choice.choice_text IS 'Text content of the answer choice';
COMMENT ON COLUMN problem_choice.media_file_id IS 'Foreign key to file table (optional image/media for choice)';
COMMENT ON COLUMN problem_choice.is_correct IS 'Whether this choice is the correct answer';
COMMENT ON COLUMN problem_choice.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN problem_choice.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN problem_choice.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN problem_choice.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- problem_answer_history table
COMMENT ON COLUMN problem_answer_history.id IS 'Primary key - auto-incrementing answer history ID';
COMMENT ON COLUMN problem_answer_history.member_id IS 'Foreign key to member table (member who answered)';
COMMENT ON COLUMN problem_answer_history.problem_id IS 'Foreign key to problem table';
COMMENT ON COLUMN problem_answer_history.selected_choice_id IS 'Foreign key to problem_choice table (chosen answer)';
COMMENT ON COLUMN problem_answer_history.is_correct IS 'Whether the selected answer was correct';
COMMENT ON COLUMN problem_answer_history.answered_at IS 'Timestamp when the answer was submitted';

-- ===========================
-- ACHIEVEMENT SYSTEM TABLES
-- ===========================

-- achievement table
COMMENT ON COLUMN achievement.id IS 'Primary key - auto-incrementing achievement ID';
COMMENT ON COLUMN achievement.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN achievement.channel_id IS 'Foreign key to channel table (NULL means server-wide achievement)';
COMMENT ON COLUMN achievement.achievement_name IS 'Name of the achievement';
COMMENT ON COLUMN achievement.description IS 'Description of how to earn this achievement';
COMMENT ON COLUMN achievement.hidden IS 'Whether this achievement is hidden until earned';
COMMENT ON COLUMN achievement.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN achievement.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN achievement.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN achievement.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- member_achievement table
COMMENT ON COLUMN member_achievement.member_id IS 'Foreign key to member table';
COMMENT ON COLUMN member_achievement.server_id IS 'Foreign key to server table';
COMMENT ON COLUMN member_achievement.achievement_id IS 'Foreign key to achievement table';
COMMENT ON COLUMN member_achievement.achieved_at IS 'Timestamp when the achievement was earned';
COMMENT ON COLUMN member_achievement.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN member_achievement.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN member_achievement.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN member_achievement.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- ===========================
-- AGENT/AI SYSTEM TABLES
-- ===========================

-- assistant_conversation table
COMMENT ON COLUMN assistant_conversation.id IS 'Primary key - auto-incrementing conversation record ID';
COMMENT ON COLUMN assistant_conversation.server_id IS 'Foreign key to server table (Discord server where conversation occurs)';
COMMENT ON COLUMN assistant_conversation.member_id IS 'Foreign key to member table (member participating in conversation)';
COMMENT ON COLUMN assistant_conversation.is_active IS 'Whether this conversation is currently active for the member';
COMMENT ON COLUMN assistant_conversation.is_interrupt IS 'Whether the agent is paused awaiting user approval for tool usage';
COMMENT ON COLUMN assistant_conversation.is_processing IS 'Whether the agent is currently processing a request';
COMMENT ON COLUMN assistant_conversation.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN assistant_conversation.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN assistant_conversation.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN assistant_conversation.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- mcp_server table
COMMENT ON COLUMN mcp_server.id IS 'Primary key - auto-incrementing MCP server ID';
COMMENT ON COLUMN mcp_server.discord_server_id IS 'Foreign key to server table (NULL for global MCP servers)';
COMMENT ON COLUMN mcp_server.server_name IS 'Human-readable name of the MCP server';
COMMENT ON COLUMN mcp_server.server_url IS 'URL or connection string for the MCP server';
COMMENT ON COLUMN mcp_server.description IS 'Description of what tools/capabilities this MCP server provides';
COMMENT ON COLUMN mcp_server.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN mcp_server.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN mcp_server.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN mcp_server.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- tool table
COMMENT ON COLUMN tool.id IS 'Primary key - auto-incrementing tool ID';
COMMENT ON COLUMN tool.mcp_server_id IS 'Foreign key to mcp_server table (MCP server that provides this tool)';
COMMENT ON COLUMN tool.tool_name IS 'Human-readable name of the tool';
COMMENT ON COLUMN tool.description IS 'Description of what this tool does and its capabilities';
COMMENT ON COLUMN tool.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN tool.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN tool.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN tool.updated_at IS 'Timestamp when record was last modified (audit trail)';

-- tool_whitelist table
COMMENT ON COLUMN tool_whitelist.member_id IS 'Foreign key to member table (member who approved the tool)';
COMMENT ON COLUMN tool_whitelist.tool_id IS 'Foreign key to tool table (tool that was approved)';
COMMENT ON COLUMN tool_whitelist.assistant_conversation_id IS 'Foreign key to assistant_conversation table (for conversation-scoped approvals)';
COMMENT ON COLUMN tool_whitelist.scope IS 'Scope of approval: "global" (all contexts), "server" (server-wide), or "conversation" (specific conversation only)';
COMMENT ON COLUMN tool_whitelist.expires_at IS 'Optional expiration timestamp for the tool approval (NULL for permanent)';
COMMENT ON COLUMN tool_whitelist.created_by IS 'Member ID who created this record (audit trail)';
COMMENT ON COLUMN tool_whitelist.updated_by IS 'Member ID who last modified this record (audit trail)';
COMMENT ON COLUMN tool_whitelist.created_at IS 'Timestamp when record was created (audit trail)';
COMMENT ON COLUMN tool_whitelist.updated_at IS 'Timestamp when record was last modified (audit trail)';


-- ===========================
-- Indexes
-- ===========================
-- member_server_stat
CREATE INDEX idx_member_server_stat_member_id ON member_server_stat(member_id);
CREATE INDEX idx_member_server_stat_server_id ON member_server_stat(server_id);

-- warning
CREATE INDEX idx_warning_member_id ON warning(member_id);
CREATE INDEX idx_warning_server_id ON warning(server_id);

-- ban
CREATE INDEX idx_ban_member_id ON ban(member_id);
CREATE INDEX idx_ban_server_id ON ban(server_id);

-- reminder
CREATE INDEX idx_reminder_member_id ON reminder(member_id);
CREATE INDEX idx_reminder_server_id ON reminder(server_id);

-- announcement
CREATE INDEX idx_announcement_server_id ON announcement(server_id);

-- member_role
CREATE INDEX idx_member_role_member_id ON member_role(member_id);
CREATE INDEX idx_member_role_server_role_id ON member_role(server_role_id);

-- poll_vote
CREATE INDEX idx_poll_vote_poll_id ON poll_vote(poll_id);
CREATE INDEX idx_poll_vote_member_id ON poll_vote(member_id);

-- filtered_message
CREATE INDEX idx_filtered_message_server_id ON filtered_message(server_id);
CREATE INDEX idx_filtered_message_member_id ON filtered_message(member_id);

-- problem_choice
CREATE INDEX idx_problem_choice_problem_id ON problem_choice(problem_id);

-- problem_category
CREATE INDEX idx_problem_category_id ON problem_category(id);

-- member_achievement
CREATE INDEX idx_member_achievement_member_id ON member_achievement(member_id);
CREATE INDEX idx_member_achievement_server_id ON member_achievement(server_id);

-- poll_option
CREATE INDEX idx_poll_option_poll_id ON poll_option(poll_id);

-- reaction_role
CREATE INDEX idx_reaction_role_server_id ON reaction_role(server_id);

-- file_path
CREATE INDEX idx_file_path_file_id ON file_path(id);

-- problem_answer_history
CREATE INDEX idx_problem_answer_history_member_id ON problem_answer_history(member_id);
CREATE INDEX idx_problem_answer_history_problem_id ON problem_answer_history(problem_id);

-- server_category
CREATE INDEX idx_server_category_server_id ON server_category(server_id);
CREATE INDEX idx_server_category_category_name ON server_category(category_name);
CREATE INDEX idx_server_category_position ON server_category(position);

-- authorization_type
CREATE INDEX idx_authorization_type_authorization_type_name ON authorization_type(authorization_type_name);

-- member_channel_authorization
CREATE INDEX idx_member_channel_authorization_member_id ON member_channel_authorization(member_id);
CREATE INDEX idx_member_channel_authorization_channel_id ON member_channel_authorization(channel_id);
CREATE INDEX idx_member_channel_authorization_authorization_type_id ON member_channel_authorization(authorization_type_id);

-- assistant_conversation
CREATE INDEX idx_assistant_conversation_server_id ON assistant_conversation(server_id);
CREATE INDEX idx_assistant_conversation_member_id ON assistant_conversation(member_id);
CREATE INDEX idx_assistant_conversation_is_active ON assistant_conversation(is_active);
CREATE INDEX idx_assistant_conversation_is_interrupt ON assistant_conversation(is_interrupt);
CREATE INDEX idx_assistant_conversation_is_processing ON assistant_conversation(is_processing);

-- mcp_server
CREATE INDEX idx_mcp_server_server_name ON mcp_server(server_name);

-- tool
CREATE INDEX idx_tool_tool_name ON tool(tool_name);
CREATE INDEX idx_tool_mcp_server_id ON tool(mcp_server_id);

-- tool_whitelist
CREATE INDEX idx_tool_whitelist_member_id ON tool_whitelist(member_id);
CREATE INDEX idx_tool_whitelist_tool_id ON tool_whitelist(tool_id);
CREATE INDEX idx_tool_whitelist_conversation_id ON tool_whitelist(assistant_conversation_id);
CREATE INDEX idx_tool_whitelist_scope ON tool_whitelist(scope);
CREATE INDEX idx_tool_whitelist_expires_at ON tool_whitelist(expires_at);


-- ===========================
-- Triggers to auto-update updated_at columns
-- ===========================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_member'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_member
        BEFORE UPDATE ON member
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_server'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_server
        BEFORE UPDATE ON server
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_server_category'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_server_category
        BEFORE UPDATE ON server_category
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_channel'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_channel
        BEFORE UPDATE ON channel
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_authorization_type'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_authorization_type
        BEFORE UPDATE ON authorization_type
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_member_channel_authorization'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_member_channel_authorization
        BEFORE UPDATE ON member_channel_authorization
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_member_server_stat'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_member_server_stat
        BEFORE UPDATE ON member_server_stat
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_warning'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_warning
        BEFORE UPDATE ON warning
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_ban'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_ban
        BEFORE UPDATE ON ban
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_server_rule'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_server_rule
        BEFORE UPDATE ON server_rule
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_reminder'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_reminder
        BEFORE UPDATE ON reminder
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_announcement'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_announcement
        BEFORE UPDATE ON announcement
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_server_role'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_server_role
        BEFORE UPDATE ON server_role
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_member_role'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_member_role
        BEFORE UPDATE ON member_role
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_poll'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_poll
        BEFORE UPDATE ON poll
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_poll_option'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_poll_option
        BEFORE UPDATE ON poll_option
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_poll_vote'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_poll_vote
        BEFORE UPDATE ON poll_vote
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_flag_reason'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_flag_reason
        BEFORE UPDATE ON flag_reason
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_filtered_message'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_filtered_message
        BEFORE UPDATE ON filtered_message
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_problem_category'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_problem_category
        BEFORE UPDATE ON problem_category
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_problem'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_problem
        BEFORE UPDATE ON problem
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_problem_choice'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_problem_choice
        BEFORE UPDATE ON problem_choice
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_file_path'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_file_path
        BEFORE UPDATE ON file_path
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_file_type'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_file_type
        BEFORE UPDATE ON file_type
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_file'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_file
        BEFORE UPDATE ON file
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_achievement'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_achievement
        BEFORE UPDATE ON achievement
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_member_achievement'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_member_achievement
        BEFORE UPDATE ON member_achievement
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_server_setting'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_server_setting
        BEFORE UPDATE ON server_setting
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_reaction_role'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_reaction_role
        BEFORE UPDATE ON reaction_role
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_assistant_conversation'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_assistant_conversation
        BEFORE UPDATE ON assistant_conversation
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_mcp_server'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_mcp_server
        BEFORE UPDATE ON mcp_server
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_tool'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_tool
        BEFORE UPDATE ON tool
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_tool_whitelist'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_tool_whitelist
        BEFORE UPDATE ON tool_whitelist
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;