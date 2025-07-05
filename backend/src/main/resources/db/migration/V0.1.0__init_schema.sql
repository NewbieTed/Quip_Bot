CREATE TABLE member (
    member_id BIGINT PRIMARY KEY,
    username TEXT,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE server (
    server_id BIGINT PRIMARY KEY,
    server_name TEXT,
    join_date TIMESTAMP,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_server_stat (
    member_id BIGINT REFERENCES member(member_id) ON DELETE CASCADE,
    server_id BIGINT REFERENCES server(server_id) ON DELETE CASCADE,
    join_date TIMESTAMP,
    level INTEGER DEFAULT 0,
    last_active TIMESTAMP,
    total_messages BIGINT DEFAULT 0,
    participation_streak INTEGER DEFAULT 0,
    experience INTEGER DEFAULT 0,
    warnings INTEGER DEFAULT 0,
    bans INTEGER DEFAULT 0,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id, server_id)
);

CREATE TABLE warnings (
    id SERIAL PRIMARY KEY,
    member_id BIGINT REFERENCES member(member_id) ON DELETE CASCADE,
    server_id BIGINT REFERENCES server(server_id) ON DELETE CASCADE,
    moderator_id BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    reason TEXT,
    issued_at TIMESTAMP,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ban (
    id SERIAL PRIMARY KEY,
    member_id BIGINT REFERENCES member(member_id) ON DELETE CASCADE,
    server_id BIGINT REFERENCES server(server_id) ON DELETE CASCADE,
    moderator_id BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    reason TEXT,
    issued_at TIMESTAMP,
    duration INTERVAL,
    unbanned BOOLEAN DEFAULT FALSE,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE server_rule (
    id SERIAL PRIMARY KEY,
    server_id BIGINT REFERENCES server(server_id) ON DELETE CASCADE,
    "order" INTEGER,
    content TEXT,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reminder (
    id SERIAL PRIMARY KEY,
    member_id BIGINT REFERENCES member(member_id) ON DELETE CASCADE,
    server_id BIGINT REFERENCES server(server_id) ON DELETE CASCADE,
    content TEXT,
    remind_at TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed BOOLEAN DEFAULT FALSE,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE announcement (
    id SERIAL PRIMARY KEY,
    server_id BIGINT REFERENCES server(server_id) ON DELETE CASCADE,
    content TEXT,
    scheduled_at TIMESTAMP,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sent BOOLEAN DEFAULT FALSE
);

CREATE TABLE server_role (
    id BIGINT PRIMARY KEY,
    server_id BIGINT REFERENCES server(server_id) ON DELETE CASCADE,
    name TEXT,
    is_auto_assignable BOOLEAN DEFAULT FALSE,
    is_self_assignable BOOLEAN DEFAULT FALSE,
    level_required INTEGER DEFAULT 0,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_role (
    member_id BIGINT REFERENCES member(member_id) ON DELETE CASCADE,
    role_id BIGINT REFERENCES server_role(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id, role_id)
);

CREATE TABLE poll (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT REFERENCES server(server_id) ON DELETE CASCADE,
    question TEXT,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMP
);

CREATE TABLE poll_vote (
    poll_id BIGINT REFERENCES poll(id) ON DELETE CASCADE,
    member_id BIGINT REFERENCES member(member_id) ON DELETE CASCADE,
    option_index INTEGER,
    voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (poll_id, member_id)
);

CREATE TABLE filtered_message (
    id BIGINT PRIMARY KEY,
    member_id BIGINT REFERENCES member(member_id) ON DELETE CASCADE,
    server_id BIGINT REFERENCES server(server_id) ON DELETE CASCADE,
    channel_id BIGINT,
    content TEXT,
    flagged_at TIMESTAMP,
    reason BIGINT,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE flag_reason (
    id BIGSERIAL PRIMARY KEY,
    reason TEXT,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE problems (
    problem_id BIGSERIAL PRIMARY KEY,
    question TEXT NOT NULL,
    correct_answer_index INTEGER NOT NULL,
    media_url TEXT,
    contributor_id BIGINT,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE problem_choice (
    choice_id BIGSERIAL PRIMARY KEY,
    problem_id BIGINT REFERENCES problems(problem_id) ON DELETE CASCADE,
    choice_text TEXT NOT NULL,
    choice_index INTEGER NOT NULL,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE achievement (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    hidden BOOLEAN DEFAULT TRUE,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_achievement (
    member_id BIGINT NOT NULL,
    server_id BIGINT NOT NULL,
    achievement_id INTEGER NOT NULL REFERENCES achievement(id) ON DELETE CASCADE,
    achieved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id, server_id, achievement_id)
);

CREATE TABLE summary (
    id SERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    server_id BIGINT NOT NULL,
    channel_id BIGINT,
    summary_text TEXT NOT NULL,
    source_message_ids BIGINT[],
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE poll_option (
    id SERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
    option_text TEXT NOT NULL,
    option_index INTEGER NOT NULL,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE server_setting (
    server_id BIGINT PRIMARY KEY,
    chatgpt_memory_enabled BOOLEAN DEFAULT FALSE,
    auto_mod_enabled BOOLEAN DEFAULT TRUE,
    level_system_enabled BOOLEAN DEFAULT TRUE,
    level_multiplier FLOAT DEFAULT 1.0,
    announcement_channel BIGINT,
    additional_settings JSONB,
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reaction_role (
    id SERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    emoji TEXT NOT NULL,
    role_id BIGINT NOT NULL REFERENCES server_role(id),
    created_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
