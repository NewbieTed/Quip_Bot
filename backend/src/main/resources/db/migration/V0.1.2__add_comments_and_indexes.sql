-- V0.1.2__add_comments_and_indexes.sql

-- 1. Add comments for clarity on all relevant tables:
COMMENT ON TABLE member IS 'Stores member information and references for Discord users.';
COMMENT ON TABLE server IS 'Stores Discord server (guild) information.';
COMMENT ON TABLE user_server_stat IS 'Tracks member statistics per server for leveling and activity.';
COMMENT ON TABLE warning IS 'Stores warnings issued to members by moderators.';
COMMENT ON TABLE ban IS 'Stores ban records issued to members by moderators.';
COMMENT ON TABLE server_rule IS 'Stores server-specific rules.';
COMMENT ON TABLE reminder IS 'Stores member reminders.';
COMMENT ON TABLE announcement IS 'Stores server announcements.';
COMMENT ON TABLE server_role IS 'Stores server-specific roles, including auto/self-assignable roles.';
COMMENT ON TABLE user_role IS 'Stores member-role assignments.';
COMMENT ON TABLE poll IS 'Stores polls created within servers.';
COMMENT ON TABLE poll_vote IS 'Stores member votes on polls.';
COMMENT ON TABLE filtered_message IS 'Stores messages flagged by auto-moderation.';
COMMENT ON TABLE flag_reason IS 'Stores flag reasons for moderation.';
COMMENT ON TABLE problem IS 'Stores quiz problems/questions.';
COMMENT ON TABLE problem_choice IS 'Stores choices for quiz problems.';
COMMENT ON TABLE achievement IS 'Stores achievements members can earn.';
COMMENT ON TABLE user_achievement IS 'Tracks member achievements per server.';
COMMENT ON TABLE summary IS 'Stores summaries generated for channels.';
COMMENT ON TABLE poll_option IS 'Stores poll options for polls.';
COMMENT ON TABLE server_setting IS 'Stores server-level settings.';
COMMENT ON TABLE reaction_role IS 'Stores reaction-role mapping for self-assignment.';
COMMENT ON TABLE file IS 'Stores uploaded files metadata.';
COMMENT ON TABLE file_path IS 'Stores various storage paths for files.';
COMMENT ON TABLE quiz_answer_history IS 'Tracks member quiz answer submissions and correctness.';

-- 2. Create indexes for suitable tables:

-- user_server_stat
CREATE INDEX idx_user_server_stat_member_id ON user_server_stat(member_id);
CREATE INDEX idx_user_server_stat_server_id ON user_server_stat(server_id);

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

-- user_role
CREATE INDEX idx_user_role_member_id ON user_role(member_id);
CREATE INDEX idx_user_role_role_id ON user_role(role_id);

-- poll_vote
CREATE INDEX idx_poll_vote_poll_id ON poll_vote(poll_id);
CREATE INDEX idx_poll_vote_member_id ON poll_vote(member_id);

-- filtered_message
CREATE INDEX idx_filtered_message_server_id ON filtered_message(server_id);
CREATE INDEX idx_filtered_message_member_id ON filtered_message(member_id);

-- problem_choice
CREATE INDEX idx_problem_choice_problem_id ON problem_choice(problem_id);

-- user_achievement
CREATE INDEX idx_user_achievement_member_id ON user_achievement(member_id);
CREATE INDEX idx_user_achievement_server_id ON user_achievement(server_id);

-- summary
CREATE INDEX idx_summary_server_id ON summary(server_id);

-- poll_option
CREATE INDEX idx_poll_option_poll_id ON poll_option(poll_id);

-- reaction_role
CREATE INDEX idx_reaction_role_server_id ON reaction_role(server_id);

-- file_path
CREATE INDEX idx_file_path_file_id ON file_path(file_id);

-- quiz_answer_history
CREATE INDEX idx_quiz_answer_history_member_id ON quiz_answer_history(member_id);
CREATE INDEX idx_quiz_answer_history_problem_id ON quiz_answer_history(problem_id);