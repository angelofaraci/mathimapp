CREATE TABLE learning_paths (
    id VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    visible_objective VARCHAR(255) NOT NULL,
    objective_type VARCHAR(50) NOT NULL,
    objective_grade_level INTEGER NOT NULL,
    CONSTRAINT pk_learning_paths PRIMARY KEY (id),
    CONSTRAINT chk_learning_paths_objective_type CHECK (objective_type = 'GRADE_LEVEL')
);

CREATE TABLE learning_path_default_grade_levels (
    grade_level INTEGER NOT NULL,
    path_id VARCHAR(50) NOT NULL,
    CONSTRAINT pk_learning_path_default_grade_levels PRIMARY KEY (grade_level),
    CONSTRAINT uq_learning_path_default_grade_levels_path UNIQUE (path_id),
    CONSTRAINT fk_learning_path_default_grade_levels_path
        FOREIGN KEY (path_id) REFERENCES learning_paths (id) ON DELETE CASCADE
);

CREATE TABLE learning_path_lessons (
    path_id VARCHAR(50) NOT NULL,
    lesson_id VARCHAR(50) NOT NULL,
    order_index INTEGER NOT NULL,
    CONSTRAINT pk_learning_path_lessons PRIMARY KEY (path_id, lesson_id),
    CONSTRAINT uq_learning_path_lessons_order UNIQUE (path_id, order_index),
    CONSTRAINT fk_learning_path_lessons_path
        FOREIGN KEY (path_id) REFERENCES learning_paths (id) ON DELETE CASCADE,
    CONSTRAINT fk_learning_path_lessons_lesson
        FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
);

CREATE TABLE user_learning_path_state (
    user_id VARCHAR(50) NOT NULL,
    selected_path_id VARCHAR(50),
    last_opened_path_id VARCHAR(50),
    CONSTRAINT pk_user_learning_path_state PRIMARY KEY (user_id),
    CONSTRAINT fk_user_learning_path_state_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_learning_path_state_selected
        FOREIGN KEY (selected_path_id) REFERENCES learning_paths (id) ON DELETE SET NULL,
    CONSTRAINT fk_user_learning_path_state_last_opened
        FOREIGN KEY (last_opened_path_id) REFERENCES learning_paths (id) ON DELETE SET NULL
);

CREATE INDEX idx_learning_path_lessons_lesson ON learning_path_lessons (lesson_id);
