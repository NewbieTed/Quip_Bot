-- V0.1.1__add_file_table_and_cleanup.sql

-- 1. Create `file` table
CREATE TABLE file (
    file_id BIGSERIAL PRIMARY KEY,
    file_name TEXT NOT NULL,
    uploaded_by BIGINT REFERENCES member(member_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create `file_path` table
CREATE TABLE file_path (
    path_id BIGSERIAL PRIMARY KEY,
    file_id BIGINT REFERENCES file(file_id) ON DELETE CASCADE,
    path TEXT NOT NULL,
    storage_provider TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Rename plural tables to singular
ALTER TABLE problems RENAME TO problem;
ALTER TABLE warnings RENAME TO warning;

-- 4. Modify `problem` table
ALTER TABLE problem
    DROP COLUMN media_url,
    ADD COLUMN file_id BIGINT REFERENCES file(file_id) ON DELETE SET NULL,
    ADD COLUMN correct_choice_id BIGINT REFERENCES problem_choice(choice_id) ON DELETE SET NULL;

-- 5. Create `quiz_answer_history` table
CREATE TABLE quiz_answer_history (
    history_id BIGSERIAL PRIMARY KEY,
    member_id BIGINT REFERENCES member(member_id) ON DELETE CASCADE,
    problem_id BIGINT REFERENCES problem(problem_id) ON DELETE CASCADE,
    selected_choice_id BIGINT REFERENCES problem_choice(choice_id) ON DELETE SET NULL,
    is_correct BOOLEAN,
    answered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);