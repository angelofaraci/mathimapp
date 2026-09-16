ALTER TABLE user_progress
    ADD COLUMN activity_streak INTEGER NOT NULL DEFAULT 0;

ALTER TABLE user_progress
    ADD COLUMN last_activity_date VARCHAR(10);
