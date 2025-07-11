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

CREATE TABLE permission_type (
    id BIGSERIAL PRIMARY KEY,
    permission_name TEXT NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(permission_name)
);

CREATE TABLE member_channel_permission (
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    channel_id BIGINT NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
    permission_type_id BIGINT NOT NULL REFERENCES permission_type(id) ON DELETE RESTRICT,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY(member_id, channel_id, permission_type_id)
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

    PRIMARY KEY (member_id, role_id)
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
    role_id BIGINT NOT NULL REFERENCES server_role(id),
    message_id BIGINT NOT NULL,
    emoji TEXT NOT NULL,

    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ===========================
-- Documentation Comments
-- ===========================
COMMENT ON TABLE member IS 'Stores member information and references for Discord members.';
COMMENT ON TABLE server IS 'Stores Discord server (guild) information.';
COMMENT ON TABLE channel IS 'Stores Discord channel information within servers, including category association.';
COMMENT ON TABLE permission_type IS 'Stores permission types for member-channel permissions.';
COMMENT ON TABLE member_channel_permission IS 'Stores fine-grained permissions each member has on a specific channel.';
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
CREATE INDEX idx_member_role_role_id ON member_role(role_id);

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

-- permission_type
CREATE INDEX idx_permission_type_permission_name ON permission_type(permission_name);

-- member_channel_permission
CREATE INDEX idx_member_channel_permission_member_id ON member_channel_permission(member_id);
CREATE INDEX idx_member_channel_permission_channel_id ON member_channel_permission(channel_id);
CREATE INDEX idx_member_channel_permission_permission_type_id ON member_channel_permission(permission_type_id);


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
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_permission_type'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_permission_type
        BEFORE UPDATE ON permission_type
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_set_updated_at_member_channel_permission'
    ) THEN
        CREATE TRIGGER trigger_set_updated_at_member_channel_permission
        BEFORE UPDATE ON member_channel_permission
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