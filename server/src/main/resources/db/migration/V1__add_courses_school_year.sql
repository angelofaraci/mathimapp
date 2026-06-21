-- Manual/reference deployment script for environments created before the school_year column existed.
-- This file is NOT executed automatically by Gradle or server startup.
-- Runtime startup keeps the inline ALTER TABLE in DatabaseFactory.init; use this script only for manual/operator-run deployments.
-- Default 0 preserves legacy rows created before grade/year targeting was introduced.
ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS school_year INTEGER NOT NULL DEFAULT 0;
