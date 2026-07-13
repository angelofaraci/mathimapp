ALTER TABLE exercises
    ADD COLUMN IF NOT EXISTS payload TEXT;

ALTER TABLE exercises
    ALTER COLUMN type TYPE VARCHAR(50);

UPDATE exercises
SET payload = CASE
    WHEN UPPER(type) = 'INPUT_VALUE' THEN
        '{"type":"inputValue","correctValue":"' || REPLACE(REPLACE(correct_answer, '\\', '\\\\'), '"', '\\"') || '"}'
    ELSE
        '{"type":"multipleChoice","options":[' ||
        REGEXP_REPLACE(
            TRIM(
                REGEXP_REPLACE(
                    REPLACE(REPLACE(options, '\\', '\\\\'), '"', '\\"'),
                    '\s*,\s*',
                    ',',
                    'g'
                )
            ),
            '([^,]+)',
            '{"id":"\1","text":"\1"}',
            'g'
        ) ||
        '],"correctOptionId":"' || REPLACE(REPLACE(TRIM(correct_answer), '\\', '\\\\'), '"', '\\"') || '"}'
END
WHERE payload IS NULL;

UPDATE exercises
SET type = 'MULTIPLE_CHOICE'
WHERE UPPER(type) = 'TRUE_FALSE';

ALTER TABLE exercises
    ALTER COLUMN payload SET NOT NULL;
