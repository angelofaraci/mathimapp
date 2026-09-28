CREATE TABLE lesson_theory_sections (
    id VARCHAR(50) NOT NULL,
    lesson_id VARCHAR(50) NOT NULL,
    position INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(160),
    content TEXT NOT NULL,
    CONSTRAINT pk_lesson_theory_sections PRIMARY KEY (id),
    CONSTRAINT uq_lesson_theory_sections_position UNIQUE (lesson_id, position),
    CONSTRAINT fk_lesson_theory_sections_lesson
        FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
);

CREATE TABLE exercise_hints (
    id VARCHAR(50) NOT NULL,
    exercise_id VARCHAR(50) NOT NULL,
    position INTEGER NOT NULL,
    unlock_after_attempts INTEGER NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    CONSTRAINT pk_exercise_hints PRIMARY KEY (id),
    CONSTRAINT uq_exercise_hints_position UNIQUE (exercise_id, position),
    CONSTRAINT chk_exercise_hints_unlock_after_attempts CHECK (unlock_after_attempts >= 0),
    CONSTRAINT fk_exercise_hints_exercise
        FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE
);

CREATE TABLE user_exercise_attempts (
    id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    exercise_id VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_exercise_attempts PRIMARY KEY (id),
    CONSTRAINT fk_user_exercise_attempts_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_exercise_attempts_exercise
        FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_exercise_attempts_user_exercise
    ON user_exercise_attempts (user_id, exercise_id);

CREATE TABLE user_exercise_hint_reveals (
    user_id VARCHAR(50) NOT NULL,
    exercise_id VARCHAR(50) NOT NULL,
    hint_id VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_exercise_hint_reveals PRIMARY KEY (user_id, exercise_id, hint_id),
    CONSTRAINT fk_user_exercise_hint_reveals_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_exercise_hint_reveals_exercise
        FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_exercise_hint_reveals_hint
        FOREIGN KEY (hint_id) REFERENCES exercise_hints (id) ON DELETE CASCADE
);
