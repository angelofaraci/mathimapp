ALTER TABLE lessons
    ADD COLUMN IF NOT EXISTS creator_id VARCHAR(50);

UPDATE lessons
SET creator_id = (
    SELECT courses.creator_id
    FROM courses
    WHERE courses.id = lessons.course_id
)
WHERE course_id IS NOT NULL
  AND creator_id IS NULL;

ALTER TABLE lessons
    DROP CONSTRAINT IF EXISTS fk_lessons_course;

ALTER TABLE lessons
    ALTER COLUMN course_id DROP NOT NULL;

ALTER TABLE lessons
    ADD CONSTRAINT fk_lessons_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE;

ALTER TABLE lessons
    DROP CONSTRAINT IF EXISTS chk_lessons_course_or_creator;

ALTER TABLE lessons
    ADD CONSTRAINT chk_lessons_course_or_creator
        CHECK (course_id IS NOT NULL OR creator_id IS NOT NULL);
