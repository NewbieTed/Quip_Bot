CREATE TABLE problems (
    problem_id BIGSERIAL PRIMARY KEY,
    question TEXT NOT NULL,
    choices TEXT[] NOT NULL,
    correct_answer_index INTEGER NOT NULL,
    media_url TEXT,
    contributor_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
