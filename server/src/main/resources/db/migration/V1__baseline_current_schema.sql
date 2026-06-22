CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users (email);

CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    -- creator_id intentionally remains a plain value to match the current Exposed schema.
    creator_id VARCHAR(50) NOT NULL,
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    school_year INTEGER NOT NULL DEFAULT 0,
    join_code VARCHAR(20),
    CONSTRAINT pk_courses PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS lessons (
    id VARCHAR(50) NOT NULL,
    course_id VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    theory_content TEXT NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_lessons PRIMARY KEY (id),
    CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exercises (
    id VARCHAR(50) NOT NULL,
    lesson_id VARCHAR(50) NOT NULL,
    question VARCHAR(500) NOT NULL,
    options VARCHAR(500) NOT NULL,
    correct_answer VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'MULTIPLE_CHOICE',
    CONSTRAINT pk_exercises PRIMARY KEY (id),
    CONSTRAINT fk_exercises_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_progress (
    user_id VARCHAR(50) NOT NULL,
    total_score INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_progress PRIMARY KEY (user_id),
    CONSTRAINT fk_user_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS completed_lessons (
    user_id VARCHAR(50) NOT NULL,
    lesson_id VARCHAR(50) NOT NULL,
    CONSTRAINT pk_completed_lessons PRIMARY KEY (user_id, lesson_id),
    CONSTRAINT fk_completed_lessons_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_completed_lessons_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS completed_exercises (
    user_id VARCHAR(50) NOT NULL,
    exercise_id VARCHAR(50) NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_completed_exercises PRIMARY KEY (user_id, exercise_id),
    CONSTRAINT fk_completed_exercises_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_completed_exercises_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS enrolled_courses (
    user_id VARCHAR(50) NOT NULL,
    course_id VARCHAR(50) NOT NULL,
    CONSTRAINT pk_enrolled_courses PRIMARY KEY (user_id, course_id),
    CONSTRAINT fk_enrolled_courses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_enrolled_courses_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);
